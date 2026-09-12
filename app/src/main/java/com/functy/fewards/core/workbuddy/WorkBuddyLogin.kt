package com.functy.fewards.core.workbuddy

import com.functy.fewards.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * WorkBuddy（腾讯 CodeBuddy）扫码授权协议客户端。
 * 移植自 workbuddy-manager 的 server/services/tencent.py（其自述对齐 workbuddy2api cmd/login）。
 *
 * 三步：
 *  1. POST /v2/plugin/auth/state?platform=CLI  -> data{state, authUrl}
 *  2. GET  /v2/plugin/auth/token?state=…       -> code!=0 表示仍在等待；code==0 时 data 含驼峰 token
 *  3. GET  /v2/plugin/login/account?state=…    -> 需 Bearer accessToken，data{uid, enterpriseId, nickname}
 *
 * 所有腾讯接口统一信封 {code, msg, data}，HTTP 4xx 也可能是业务信封，因此判定以 code 为准。
 * 轮询期间的「未扫码」不是错误：实测未扫码时返回 code=11217（msg "login ing..."）。
 * 日志严禁打印 accessToken / refreshToken。
 */
class WorkBuddyLogin(private val client: OkHttpClient) {

    companion object {
        const val API_BASE = "https://copilot.tencent.com"
        const val PATH_AUTH_STATE = "/v2/plugin/auth/state"
        const val PATH_AUTH_TOKEN = "/v2/plugin/auth/token"
        const val PATH_LOGIN_ACCOUNT = "/v2/plugin/login/account"

        /** 实测：未扫码时 /auth/token 返回的业务码。 */
        const val CODE_WAITING = 11217

        /** 本地会话有效期，与 workbuddy-manager 的 STATE_TTL 一致。 */
        const val STATE_TTL_MS = 300_000L

        val HEADERS: Map<String, String> = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json, text/plain, */*",
            "X-Requested-With" to "XMLHttpRequest",
            "User-Agent" to "CLI/2.63.2 CodeBuddy/2.63.2",
            "Origin" to "https://www.codebuddy.cn",
            "Referer" to "https://www.codebuddy.cn/",
        )

        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        // ==================== 纯解析（JVM 单测钉这里） ====================

        /** 腾讯信封：优先取 body 里的 code，取不到时退回 HTTP 状态码。 */
        fun envelopeCode(httpStatus: Int, json: JSONObject): Int =
            if (json.has("code")) json.optInt("code", -1) else httpStatus

        /**
         * 解析用的成功判定：必须**显式**带 code==0。
         * 空对象 / 非 JSON / 缺 code 一律算失败——这类响应只会出现在网络或协议异常时，
         * 不能因为「没有 code 字段」就当成成功。
         */
        fun hasOkCode(json: JSONObject): Boolean = json.has("code") && json.optInt("code", -1) == 0

        fun envelopeData(json: JSONObject): JSONObject? = json.optJSONObject("data")

        /** 解析 /auth/state 的 data。缺 state 或 authUrl 视为失败。 */
        fun parseAuthState(json: JSONObject): Session? {
            if (!hasOkCode(json)) return null
            val data = envelopeData(json) ?: return null
            val state = data.optString("state").trim()
            val authUrl = data.optString("authUrl").trim()
            if (state.isEmpty() || authUrl.isEmpty()) return null
            return Session(state = state, authUrl = authUrl)
        }

        /** 解析 /auth/token 的 data。未扫码时 data 为空 -> null（调用方按等待处理）。 */
        fun parseAuthToken(json: JSONObject): TokenPayload? {
            if (!hasOkCode(json)) return null
            val data = envelopeData(json) ?: return null
            val accessToken = data.optString("accessToken").trim()
            if (accessToken.isEmpty()) return null
            return TokenPayload(
                accessToken = accessToken,
                refreshToken = data.optString("refreshToken").trim(),
                expiresIn = data.optInt("expiresIn", 3600).let { if (it > 0) it else 3600 },
                domain = data.optString("domain").trim(),
            )
        }

        /** 解析 /login/account 的 data。缺 uid 视为未就绪。 */
        fun parseLoginAccount(json: JSONObject): AccountInfo? {
            val data = envelopeData(json) ?: return null
            val uid = data.optString("uid").trim()
            if (uid.isEmpty()) return null
            return AccountInfo(
                uid = uid,
                nickname = data.optString("nickname").trim(),
                enterpriseId = data.optString("enterpriseId").trim(),
            )
        }

        /** 未扫码（含任何非 0 业务码）都按「继续等待」处理，与 workbuddy-manager 一致。 */
        fun isWaiting(code: Int): Boolean = code != 0
    }

    data class Session(val state: String, val authUrl: String, val startedAt: Long = System.currentTimeMillis())

    data class TokenPayload(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int,
        val domain: String,
    )

    data class AccountInfo(val uid: String, val nickname: String, val enterpriseId: String)

    data class LoginResult(
        val uid: String,
        val nickname: String,
        val enterpriseId: String,
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val domain: String,
    )

    sealed class Status {
        /** 仍在等待扫码 / 已扫未确认。 */
        data object Pending : Status()

        /** 本地会话超时（腾讯侧 state 过期）。 */
        data object Expired : Status()

        data class Ready(val result: LoginResult) : Status()
    }

    // ==================== HTTP ====================

    private suspend fun post(path: String, query: Map<String, String> = emptyMap()): JSONObject =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(API_BASE + path + buildQuery(query))
                .post("{}".toRequestBody(jsonMedia))
                .apply { HEADERS.forEach { (k, v) -> header(k, v) } }
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body.string()
                runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            }
        }

    private suspend fun get(path: String, query: Map<String, String>, bearer: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(API_BASE + path + buildQuery(query))
            HEADERS.forEach { (k, v) -> builder.header(k, v) }
            if (!bearer.isNullOrEmpty()) builder.header("Authorization", "Bearer $bearer")
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body.string()
                runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            }
        }

    private fun buildQuery(query: Map<String, String>): String {
        if (query.isEmpty()) return ""
        return query.entries.joinToString("&", prefix = "?") { (k, v) ->
            "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
        }
    }

    // ==================== 流程 ====================

    /** 第一步：申请授权链接。返回的 authUrl 即二维码内容。 */
    suspend fun startLogin(): Session {
        val json = post(PATH_AUTH_STATE, mapOf("platform" to "CLI"))
        val session = parseAuthState(json)
        if (session == null) {
            val code = if (json.has("code")) json.optInt("code", -1) else -1
            AppLog.w("WB", "获取授权链接失败 code=$code")
            throw RuntimeException("获取授权链接失败 code=$code")
        }
        AppLog.i("WB", "已生成 WorkBuddy 授权二维码，等待扫码")
        return session
    }

    /** 第二步 + 第三步：查询一次扫码结果，成功时顺带取回账号信息。 */
    suspend fun poll(session: Session): Status {
        if (System.currentTimeMillis() - session.startedAt > STATE_TTL_MS) {
            AppLog.w("WB", "二维码已过期（本地会话超时）")
            return Status.Expired
        }
        val tokenJson = get(PATH_AUTH_TOKEN, mapOf("state" to session.state))
        val code = envelopeCode(0, tokenJson)
        if (isWaiting(code)) return Status.Pending
        val payload = parseAuthToken(tokenJson) ?: return Status.Pending

        val accountJson = get(
            PATH_LOGIN_ACCOUNT,
            mapOf("state" to session.state),
            bearer = payload.accessToken,
        )
        val info = parseLoginAccount(accountJson) ?: return Status.Pending

        AppLog.i("WB", "扫码授权成功，账号 ${info.nickname.ifEmpty { info.uid.take(8) }}")
        return Status.Ready(
            LoginResult(
                uid = info.uid,
                nickname = info.nickname.ifEmpty { "Work Buddy 账号" },
                enterpriseId = info.enterpriseId,
                accessToken = payload.accessToken,
                refreshToken = payload.refreshToken,
                expiresAt = System.currentTimeMillis() / 1000 + payload.expiresIn,
                domain = payload.domain,
            )
        )
    }

    /**
     * 轮询直到确认 / 过期 / 超时。默认 2 秒一次，与 workbuddy-manager 前端一致。
     * 腾讯协议在「已扫未确认」和「未扫码」都返回非 0 码，因此没有单独的「已扫码」回调，
     * UI 只需区分「等待中 / 成功 / 过期」。
     */
    suspend fun awaitLogin(
        session: Session,
        timeoutMs: Long = STATE_TTL_MS,
        pollIntervalMs: Long = 2_000,
    ): LoginResult {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            when (val st = poll(session)) {
                is Status.Ready -> return st.result
                is Status.Expired -> throw RuntimeException("二维码已过期")
                is Status.Pending -> delay(pollIntervalMs)
            }
        }
        throw RuntimeException("二维码已过期")
    }
}
