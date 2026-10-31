package com.functy.fewards.core.workbuddy

import com.functy.fewards.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * WorkBuddy accessToken 自动续期（refreshToken -> 新 accessToken）。
 *
 * 协议与 dsh-workbuddy-panel 的内核（workbuddy2api）RefreshToken 同源：
 *
 *   POST {base}/v2/plugin/auth/token/refresh
 *   Header: X-Refresh-Token: <refreshToken>
 *           X-Auth-Refresh-Source: plugin
 *           Authorization: Bearer <accessToken>
 *   Body:   {}                      （官方 axios.post(url, {})，不是空 body）
 *   Resp:   {code:0, data:{accessToken, refreshToken, expiresIn, domain}}
 *
 * 三条硬约束（照抄内核，缺一条就会把号刷死）：
 *  1. **单飞**：上游对 refreshToken 反重放，同一个 RT 被消费后再用会撞 12153
 *     （refresh token is invalid）并把整条 RT 链作废。同账号同一时刻只允许一个刷新在途。
 *  2. **写回**：刷新响应的 accessToken / refreshToken 总是同时轮换，新 RT 必须原子写回
 *     账号仓储；只写内存等于把旧 RT 留在磁盘上，下次刷新必然 12153。
 *  3. **12153 是终态**：会话已被上游判死，只能重新扫码授权，不要再重试。
 *
 * 日志严禁打印 accessToken / refreshToken。
 */
class WorkBuddyAuth(
    private val client: OkHttpClient,
    /** 刷新端点 base；默认与扫码授权同一个 plugin 通道（单测注入本地服务器）。 */
    private val baseUrl: String = WorkBuddyLogin.API_BASE,
) {

    companion object {
        /** 刷新端点：与扫码授权同一个 plugin 通道（copilot.tencent.com）。 */
        const val PATH_REFRESH = "/v2/plugin/auth/token/refresh"

        /** 上游明确的「会话不存在 / RT 失效」业务码。 */
        const val CODE_SESSION_DEAD = 12153

        /** 提前量：距过期不足 10 分钟就先换，避免签到请求正好撞在过期瞬间。 */
        const val REFRESH_SKEW_SECONDS = 600L

        /** 防御值：expiresIn 超过 10 年按脏数据丢弃，保留旧过期时间（同内核）。 */
        const val MAX_EXPIRES_IN = 10L * 365 * 24 * 3600

        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        /** 单飞锁：key = uid（没有 uid 时退回本地 id）。 */
        private val locks = ConcurrentHashMap<String, Mutex>()

        /**
         * 该不该在业务请求之前先刷新。
         *
         * - 没有 refreshToken：永远不刷新（手填 accessToken 的账号只能用到过期）；
         * - 没有 accessToken 但有 refreshToken：先换一个；
         * - 过期时间未知（0，老数据 / 导入时没解析出 exp）：有 RT 就先换一次，换来确定的过期时间；
         * - 距过期不足 [REFRESH_SKEW_SECONDS]：提前换。
         */
        fun needsRefresh(
            accessToken: String,
            refreshToken: String,
            expiresAt: Long,
            nowSeconds: Long = System.currentTimeMillis() / 1000,
        ): Boolean {
            if (refreshToken.isEmpty()) return false
            if (accessToken.isEmpty()) return true
            if (expiresAt <= 0L) return true
            return expiresAt - nowSeconds <= REFRESH_SKEW_SECONDS
        }

        /** 纯解析：HTTP 状态 + 腾讯信封 -> 刷新结果（JVM 单测钉这里）。 */
        fun parseRefresh(
            httpStatus: Int,
            json: JSONObject,
            nowSeconds: Long = System.currentTimeMillis() / 1000,
        ): Result {
            val code = WorkBuddyLogin.envelopeCode(httpStatus, json)
            if (code == CODE_SESSION_DEAD) return Result.SessionDead
            if (httpStatus == 401 || httpStatus == 403) return Result.SessionDead
            if (!WorkBuddyLogin.hasOkCode(json)) {
                val msg = json.optString("msg", json.optString("message")).take(120)
                return Result.Failed("code=$code msg=$msg")
            }
            val data = json.optJSONObject("data")
                ?: return Result.Failed("响应缺少 data")
            val accessToken = data.optString("accessToken").trim()
            if (accessToken.isEmpty()) return Result.Failed("响应缺少 accessToken")
            val expiresIn = data.optLong("expiresIn", 0L)
            val expiresAt = if (expiresIn in 1..MAX_EXPIRES_IN) nowSeconds + expiresIn else 0L
            return Result.Success(
                Tokens(
                    accessToken = accessToken,
                    refreshToken = data.optString("refreshToken").trim(),
                    expiresAt = expiresAt,
                    domain = data.optString("domain").trim(),
                )
            )
        }

        /**
         * 刷新请求头（纯函数，单测钉住「X-Refresh-Token 只出现在刷新端点」这条红线）。
         * [accessToken] 为空时不带 Authorization —— 只拿 RT 换号的场景上游也不认，
         * 但仍然把请求发出去，让上游用 12153 给出明确答复。
         */
        fun refreshHeaders(
            accessToken: String,
            refreshToken: String,
            uid: String,
        ): Map<String, String> = buildMap {
            putAll(WorkBuddyLogin.HEADERS)
            put("X-Refresh-Token", refreshToken)
            put("X-Auth-Refresh-Source", "plugin")
            put("X-Auth-Method", "plugin")
            if (accessToken.isNotEmpty()) put("Authorization", "Bearer $accessToken")
            if (uid.isNotEmpty()) put("X-User-Id", uid)
        }
    }

    /** 刷新结果。12153 / HTTP 401·403 = 会话已死，只能重登。 */
    sealed class Result {
        data class Success(val tokens: Tokens) : Result()
        data object SessionDead : Result()
        data class Failed(val reason: String) : Result()
    }

    /** 刷新后的新凭据。[expiresAt] 为 0 表示响应没给 expiresIn，调用方沿用旧值。 */
    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val domain: String,
    )

    /**
     * 用 refreshToken 换一对新 token。同账号串行（单飞），并发调用排队复用同一个结果。
     * 失败不会抛异常：调用方按 [Result] 分支处理（SessionDead 要提示重新扫码）。
     */
    suspend fun refresh(
        accessToken: String,
        refreshToken: String,
        uid: String,
        key: String = uid.ifEmpty { "anonymous" },
    ): Result {
        val lock = locks.computeIfAbsent(key) { Mutex() }
        return lock.withLock { refreshOnce(accessToken, refreshToken, uid) }
    }

    private suspend fun refreshOnce(
        accessToken: String,
        refreshToken: String,
        uid: String,
    ): Result = withContext(Dispatchers.IO) {
        val rt = refreshToken.trim()
        if (rt.isEmpty()) return@withContext Result.Failed("没有 refreshToken")

        val builder = Request.Builder()
            .url(baseUrl + PATH_REFRESH)
            .post("{}".toRequestBody(jsonMedia))
        refreshHeaders(accessToken.trim(), rt, uid.trim()).forEach { (k, v) -> builder.header(k, v) }

        try {
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body.string()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                val result = parseRefresh(resp.code, json)
                when (result) {
                    is Result.Success -> AppLog.i("WB", "accessToken 自动刷新成功")
                    Result.SessionDead -> AppLog.w("WB", "refreshToken 已失效（12153），需要重新扫码授权")
                    is Result.Failed -> AppLog.w("WB", "自动刷新失败：${result.reason}")
                }
                result
            }
        } catch (t: Throwable) {
            AppLog.w("WB", "自动刷新请求异常：${t.message}")
            Result.Failed(t.message ?: "网络异常")
        }
    }
}
