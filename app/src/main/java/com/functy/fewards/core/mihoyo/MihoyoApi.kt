package com.functy.fewards.core.mihoyo

import com.functy.fewards.core.mihoyo.MihoyoConstants.STOKEN_COOKIE_URL
import com.functy.fewards.data.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 米游社 HTTP 层：请求 / JSON / 登录态工具。逻辑移植自 MiyoQian（core/http.py、cookies.py、auth/login.py）。
 * 接口细节可根据实际抓包微调。
 */
class MihoyoApi(private val client: OkHttpClient) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    // ==================== 基础请求 ====================

    suspend fun getJson(url: String, headers: Map<String, String>, params: Map<String, String> = emptyMap()): JSONObject =
        requestJson("GET", url, null, headers, params)

    suspend fun postJson(url: String, body: String, headers: Map<String, String>, params: Map<String, String> = emptyMap()): JSONObject =
        requestJson("POST", url, body, headers, params)

    private suspend fun requestJson(
        method: String,
        url: String,
        body: String?,
        headers: Map<String, String>,
        params: Map<String, String>,
    ): JSONObject = withContext(Dispatchers.IO) {
        val fullUrl = if (params.isEmpty()) url else buildString {
            append(url)
            append(if (url.contains('?')) '&' else '?')
            append(params.entries.joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            })
        }
        val builder = Request.Builder().url(fullUrl)
        headers.forEach { (k, v) -> builder.header(k, v) }
        when (method) {
            "POST" -> builder.post((body ?: "{}").toRequestBody(jsonMedia))
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string() ?: "{}"
            runCatching { JSONObject(text) }.getOrElse { JSONObject() }
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        fun cookieValue(cookie: String, name: String): String {
            val re = Regex("""(?:^|;\s*)${Regex.escape(name)}=([^;]+)""")
            val m = re.find(cookie)
            return m?.groupValues?.get(1) ?: ""
        }

        fun guessUid(cookie: String): String = cookieValue(cookie, "account_id")
            .ifEmpty { cookieValue(cookie, "account_id_v2") }
            .ifEmpty { cookieValue(cookie, "ltuid") }
            .ifEmpty { cookieValue(cookie, "login_uid") }
            .ifEmpty { cookieValue(cookie, "stuid") }

        fun guessMid(cookie: String): String = cookieValue(cookie, "mid")
            .ifEmpty { cookieValue(cookie, "account_mid_v2") }
            .ifEmpty { cookieValue(cookie, "ltmid_v2") }

        /** stoken cookie：v2 必须带 mid（MiyoQian cookies.stoken_cookie）。 */
        fun stokenCookie(account: AccountRepository.MihoyoAccount): String {
            val uid = account.stuid.ifEmpty { guessUid(account.cookie) }
            val mid = account.mid.ifEmpty { guessMid(account.cookie) }
            require(uid.isNotEmpty()) { "缺少 stuid，无法执行米游币社区任务" }
            require(!(account.stoken.startsWith("v2_") && mid.isEmpty())) {
                "v2 stoken 需要 mid，请重新扫码登录"
            }
            return buildString {
                append("stuid=$uid;stoken=${account.stoken}")
                if (mid.isNotEmpty()) append(";mid=$mid")
            }
        }

        fun webCookie(account: AccountRepository.MihoyoAccount): String = account.cookie

        /** 判断粘贴的 cookie 是否可用（含 stoken + mid 或 stoken v0）。 */
        fun parseCookie(cookie: String): AccountRepository.MihoyoAccount? {
            val stoken = cookieValue(cookie, "stoken")
            val uid = guessUid(cookie)
            val mid = guessMid(cookie)
            if (stoken.isEmpty() || uid.isEmpty()) return null
            return AccountRepository.MihoyoAccount(
                id = "mhy_${System.currentTimeMillis()}",
                nickname = "账号$uid",
                stoken = stoken,
                stuid = uid,
                mid = mid,
                cookie = cookie,
            )
        }
    }

    // ==================== 凭证刷新 ====================

    /**
     * 用 stoken 刷新 cookie_token（老接口）。
     * ⚠️ 验证 stoken / 刷 cookie_token 必须用 getCookieAccountInfoBySToken 老接口，
     * 不要用 ma-cn-session getTokenBySToken（非官方设备环境 -5300 风控）。
     */
    suspend fun refreshCookieToken(account: AccountRepository.MihoyoAccount): String? {
        val cookie = runCatching { stokenCookie(account) }.getOrNull() ?: return null
        val data = getJson(
            STOKEN_COOKIE_URL,
            mapOf(
                "cookie" to cookie,
                "user-agent" to DsSign.DEFAULT_MOBILE_UA,
            )
        )
        if (data.optInt("retcode", -1) != 0) return null
        val token = data.optJSONObject("data")?.optString("cookie_token") ?: return null
        if (token.isEmpty()) return null
        return replaceOrAppendCookieValue(account.cookie, "cookie_token", token)
    }

    fun replaceOrAppendCookieValue(cookie: String, key: String, value: String): String {
        if (cookie.isEmpty()) return "$key=$value"
        val pattern = Regex("""((?:^|;\s*)${Regex.escape(key)}=)([^;]*)""")
        val match = pattern.find(cookie) ?: return cookie.trimEnd(';', ' ') + "; $key=$value"
        return cookie.replaceRange(match.range, match.groupValues[1] + value)
    }

    // ==================== 扫码登录 ====================

    data class QrSession(val url: String, val ticket: String, val deviceId: String, val deviceFp: String)

    data class QrResult(val stoken: String, val stuid: String, val mid: String, val nickname: String)

    private fun qrHeaders(deviceId: String, deviceFp: String, body: String): Map<String, String> = mapOf(
        "User-Agent" to DsSign.PASSPORT_APP_UA,
        "Accept" to "*/*",
        "Accept-Language" to "zh-cn",
        "x-rpc-client_type" to "3",
        "x-rpc-app_version" to DsSign.PASSPORT_APP_VERSION,
        "x-rpc-device_id" to deviceId,
        "x-rpc-device_fp" to deviceFp,
        "x-rpc-game_biz" to "bbs_cn",
        "x-rpc-app_id" to DsSign.PASSPORT_APP_ID,
        "x-rpc-sdk_version" to DsSign.PASSPORT_APP_VERSION,
        "x-rpc-device_model" to "Mi 14",
        "x-rpc-device_name" to "Mihoyo Capture",
        "x-rpc-account_version" to DsSign.PASSPORT_APP_VERSION,
        "DS" to DsSign.dsX4(body = body),
        "Content-Type" to "application/json; charset=UTF-8",
    )

    /** 生成二维码登录会话。返回二维码内容 URL（供用户用米游社 App 扫）。 */
    suspend fun createQrLogin(): QrSession {
        val deviceId = DsSign.randomDeviceId()
        val deviceFp = DsSign.deviceFp(deviceId)
        val body = "{}"
        val data = postJson(MihoyoConstants.QRCODE_FETCH_URL, body, qrHeaders(deviceId, deviceFp, body))
        if (data.optInt("retcode", -1) != 0) {
            throw RuntimeException("生成二维码失败: retcode=${data.optInt("retcode")} ${data.optString("message")}")
        }
        val d = data.optJSONObject("data") ?: JSONObject()
        val url = d.optString("url")
        val ticket = d.optString("ticket")
        if (url.isEmpty() || ticket.isEmpty()) throw RuntimeException("二维码接口未返回 url/ticket")
        return QrSession(url, ticket, deviceId, deviceFp)
    }

    sealed class QrStatus {
        data object Waiting : QrStatus()
        data object Scanned : QrStatus()
        data class Confirmed(val result: QrResult) : QrStatus()
        data object Expired : QrStatus()
    }

    /** 查询一次二维码状态。 */
    suspend fun queryQrLogin(session: QrSession): QrStatus {
        val body = JSONObject().put("ticket", session.ticket).toString()
        val data = postJson(MihoyoConstants.QRCODE_QUERY_URL, body, qrHeaders(session.deviceId, session.deviceFp, body))
        if (data.optInt("retcode", -1) != 0) {
            throw RuntimeException("查询二维码状态失败: retcode=${data.optInt("retcode")} ${data.optString("message")}")
        }
        val d = data.optJSONObject("data") ?: JSONObject()
        return when (d.optString("status")) {
            "Scanned" -> QrStatus.Scanned
            "Confirmed" -> {
                val userInfo = d.optJSONObject("user_info") ?: JSONObject()
                val tokens = d.optJSONArray("tokens")
                var stoken = ""
                if (tokens != null) {
                    for (i in 0 until tokens.length()) {
                        val t = tokens.getJSONObject(i)
                        // token_type==1 → stoken v2
                        if (t.optInt("token_type") == 1) {
                            stoken = t.optString("token")
                            break
                        }
                    }
                }
                val mid = userInfo.optString("mid")
                val aid = userInfo.optString("aid")
                if (stoken.isEmpty() || mid.isEmpty() || aid.isEmpty()) {
                    throw RuntimeException("扫码结果缺少 stoken/mid/aid")
                }
                QrStatus.Confirmed(
                    QrResult(
                        stoken = stoken,
                        stuid = aid,
                        mid = mid,
                        nickname = userInfo.optString("nickname").ifEmpty { "账号$aid" },
                    )
                )
            }
            "Expired" -> QrStatus.Expired
            else -> QrStatus.Waiting
        }
    }

    /** 轮询二维码直到确认/过期/超时。 */
    suspend fun awaitQrLogin(
        session: QrSession,
        timeoutMs: Long = 120_000,
        onStatus: (QrStatus) -> Unit = {},
    ): QrResult {
        val start = System.currentTimeMillis()
        var last: String = ""
        while (System.currentTimeMillis() - start < timeoutMs) {
            when (val st = queryQrLogin(session)) {
                is QrStatus.Waiting -> if (last != "Init") { last = "Init"; onStatus(st) }
                is QrStatus.Scanned -> if (last != "Scanned") { last = "Scanned"; onStatus(st) }
                is QrStatus.Expired -> { onStatus(st); throw RuntimeException("二维码已过期") }
                is QrStatus.Confirmed -> { onStatus(st); return st.result }
            }
            delay(2000)
        }
        throw RuntimeException("扫码登录超时")
    }

    /** stoken 验证 + 换 web cookie（老接口）。retcode==0 即有效。 */
    suspend fun validateStoken(account: AccountRepository.MihoyoAccount): Boolean {
        val cookie = runCatching { stokenCookie(account) }.getOrNull() ?: return false
        val data = getJson(
            STOKEN_COOKIE_URL,
            mapOf(
                "cookie" to cookie,
                "user-agent" to DsSign.DEFAULT_MOBILE_UA,
            )
        )
        return data.optInt("retcode", -1) == 0
    }
}
