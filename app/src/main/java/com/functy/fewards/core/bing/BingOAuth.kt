package com.functy.fewards.core.bing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder

/**
 * Microsoft 账号 OAuth 客户端（授权码 + refresh_token，公开客户端无 client_secret）。
 *
 * 流程（App 内在 WebView 里完成，不跳出应用）：
 *  1. WebView 打开 [authorizeUrl]；用户在 login.live.com 正常登录（支持 2FA / passkey）；
 *  2. 登录成功后浏览器跳向 [BingConstants.REDIRECT_URI]?code=…，
 *     WebViewClient 在加载**之前**拦截并调用 [extractCode]；
 *  3. [exchangeCode] 用授权码换 access_token + refresh_token；
 *  4. 之后 access_token 过期就用 refresh_token 换新的，**用户无需再登录**。
 *
 * 换 token 的响应体在实测中可能是 JSON，也可能是 form-urlencoded，
 * 因此 [parseTokenResponse] 两种都认——这条分支有单测钉住。
 */
class BingOAuth(private val client: OkHttpClient) {

    companion object {

        private val formMedia = "application/x-www-form-urlencoded".toMediaType()

        data class Token(
            val accessToken: String,
            /** 为空表示本次响应没换发新 refresh_token，调用方应沿用旧的。 */
            val refreshToken: String,
            val expiresInSeconds: Long,
        )

        /** 生成授权页 URL。[state] 用于回跳时校验，防串号。 */
        fun authorizeUrl(state: String): String {
            val builder = BingConstants.AUTHORIZE_URL.toHttpUrlOrNull()?.newBuilder()
                ?: return BingConstants.AUTHORIZE_URL
            return builder
                .addQueryParameter("client_id", BingConstants.CLIENT_ID)
                .addQueryParameter("response_type", "code")
                .addQueryParameter("scope", BingConstants.SCOPE)
                .addQueryParameter("redirect_uri", BingConstants.REDIRECT_URI)
                .addQueryParameter("state", state)
                .build()
                .toString()
        }

        /** 是否是授权回跳地址（命中即可在 WebView 里拦下，不必真的加载那个页面）。 */
        fun isRedirect(url: String?): Boolean {
            val parsed = url?.toHttpUrlOrNull() ?: return false
            return parsed.host.equals("login.live.com", ignoreCase = true) &&
                parsed.encodedPath == "/oauth20_desktop.srf"
        }

        /** 从回跳地址取授权码；没有 code 返回 null。 */
        fun extractCode(url: String?): String? =
            url?.toHttpUrlOrNull()?.queryParameter("code")?.takeIf { it.isNotEmpty() }

        /** 从回跳地址取 state（用于防串号校验）。 */
        fun extractState(url: String?): String? =
            url?.toHttpUrlOrNull()?.queryParameter("state")

        /** 用户点了「拒绝」，或微软直接回错。 */
        fun extractError(url: String?): String? {
            val parsed = url?.toHttpUrlOrNull() ?: return null
            val error = parsed.queryParameter("error") ?: return null
            val desc = parsed.queryParameter("error_description").orEmpty()
            return if (desc.isEmpty()) error else "$error: $desc"
        }

        /**
         * 解析换 token 的响应体。JSON 优先，回落到 form-urlencoded。
         * 没有 access_token 一律返回 null（不能因为「有响应」就当成功）。
         */
        fun parseTokenResponse(body: String): Token? {
            val text = body.trim()
            if (text.isEmpty()) return null

            val json = runCatching { JSONObject(text) }.getOrNull()
            if (json != null) {
                val access = json.optString("access_token")
                if (access.isNotEmpty()) {
                    return Token(
                        accessToken = access,
                        refreshToken = json.optString("refresh_token"),
                        expiresInSeconds = json.optLong("expires_in", 3600L),
                    )
                }
            }

            val form = parseForm(text)
            val access = form["access_token"]?.takeIf { it.isNotEmpty() } ?: return null
            return Token(
                accessToken = access,
                refreshToken = form["refresh_token"].orEmpty(),
                expiresInSeconds = form["expires_in"]?.toLongOrNull() ?: 3600L,
            )
        }

        private fun parseForm(body: String): Map<String, String> =
            body.split('&').mapNotNull { pair ->
                val i = pair.indexOf('=')
                if (i <= 0) return@mapNotNull null
                runCatching {
                    URLDecoder.decode(pair.substring(0, i), "UTF-8") to
                        URLDecoder.decode(pair.substring(i + 1), "UTF-8")
                }.getOrNull()
            }.toMap()
    }

    // ==================== HTTP ====================

    private suspend fun postToken(form: Map<String, String>): Token? =
        withContext(Dispatchers.IO) {
            val body = form.entries.joinToString("&") { (k, v) ->
                java.net.URLEncoder.encode(k, "UTF-8") + "=" + java.net.URLEncoder.encode(v, "UTF-8")
            }.toRequestBody(formMedia)
            val req = Request.Builder()
                .url(BingConstants.TOKEN_URL)
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body.string()
                parseTokenResponse(text)
            }
        }

    /** 授权码 -> token。 */
    suspend fun exchangeCode(code: String): Token? = postToken(
        mapOf(
            "client_id" to BingConstants.CLIENT_ID,
            "code" to code,
            "redirect_uri" to BingConstants.REDIRECT_URI,
            "grant_type" to "authorization_code",
        )
    )

    /** refresh_token -> token。 */
    suspend fun refresh(refreshToken: String): Token? = postToken(
        mapOf(
            "client_id" to BingConstants.CLIENT_ID,
            "refresh_token" to refreshToken,
            "scope" to BingConstants.SCOPE,
            "grant_type" to "REFRESH_TOKEN",
        )
    )
}
