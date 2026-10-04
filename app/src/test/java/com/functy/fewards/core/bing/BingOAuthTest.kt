package com.functy.fewards.core.bing

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OAuth URL 生成 / 回跳解析 / 换 token 响应解析。
 *
 * 这几条是「点一次登录就能长期免登录」的地基：回跳解析错一步，
 * 用户就会卡在授权页；换 token 解析错一步，会静默存下一个空凭据。
 */
class BingOAuthTest {

    private val state = "0123456789abcdef0123456789abcdef"

    @Test
    fun authorizeUrlCarriesClientScopeAndState() {
        val url = BingOAuth.authorizeUrl(state).toHttpUrlOrNull()
        assertNotNull(url)
        assertEquals(BingConstants.CLIENT_ID, url!!.queryParameter("client_id"))
        assertEquals("code", url.queryParameter("response_type"))
        assertEquals(BingConstants.SCOPE, url.queryParameter("scope"))
        assertEquals(BingConstants.REDIRECT_URI, url.queryParameter("redirect_uri"))
        assertEquals(state, url.queryParameter("state"))
        assertEquals("login.live.com", url.host)
        assertEquals("/oauth20_authorize.srf", url.encodedPath)
    }

    @Test
    fun differentStateProducesDifferentUrl() {
        assertTrue(BingOAuth.authorizeUrl("a") != BingOAuth.authorizeUrl("b"))
    }

    @Test
    fun onlyTheDesktopRedirectCountsAsRedirect() {
        assertTrue(BingOAuth.isRedirect("https://login.live.com/oauth20_desktop.srf?code=abc"))
        assertFalse(BingOAuth.isRedirect("https://login.live.com/oauth20_authorize.srf?client_id=x"))
        // 别的域名带着同样的路径不算回跳
        assertFalse(BingOAuth.isRedirect("https://evil.example.com/oauth20_desktop.srf?code=abc"))
        assertFalse(BingOAuth.isRedirect(null))
        assertFalse(BingOAuth.isRedirect("not a url"))
    }

    @Test
    fun extractsCodeAndStateFromRedirect() {
        val url = "https://login.live.com/oauth20_desktop.srf?code=M.R3_BAY.abc-123&state=$state"
        assertEquals("M.R3_BAY.abc-123", BingOAuth.extractCode(url))
        assertEquals(state, BingOAuth.extractState(url))
    }

    @Test
    fun noCodeMeansNull() {
        assertNull(BingOAuth.extractCode("https://login.live.com/oauth20_desktop.srf?state=$state"))
        assertNull(BingOAuth.extractCode(null))
    }

    @Test
    fun extractsUserDenial() {
        val url = "https://login.live.com/oauth20_desktop.srf?error=access_denied&error_description=User%20denied"
        assertEquals("access_denied: User denied", BingOAuth.extractError(url))
        assertNull(BingOAuth.extractError("https://login.live.com/oauth20_desktop.srf?code=abc"))
    }

    @Test
    fun parsesJsonTokenResponse() {
        val token = BingOAuth.parseTokenResponse(
            """{"token_type":"bearer","access_token":"AT","refresh_token":"RT","expires_in":3600}"""
        )
        assertNotNull(token)
        assertEquals("AT", token!!.accessToken)
        assertEquals("RT", token.refreshToken)
        assertEquals(3600L, token.expiresInSeconds)
    }

    @Test
    fun parsesFormEncodedTokenResponse() {
        // login.live.com 在部分情况下返回 urlencoded 而不是 JSON——这条分支必须也能用
        val token = BingOAuth.parseTokenResponse("access_token=AT&refresh_token=RT&expires_in=1800")
        assertNotNull(token)
        assertEquals("AT", token!!.accessToken)
        assertEquals("RT", token.refreshToken)
        assertEquals(1800L, token.expiresInSeconds)
    }

    @Test
    fun formTokenResponseDecodesPercentEncoding() {
        val token = BingOAuth.parseTokenResponse("access_token=a%2Bb%3Dc&refresh_token=r%2Fx")
        assertEquals("a+b=c", token!!.accessToken)
        assertEquals("r/x", token.refreshToken)
    }

    @Test
    fun refreshResponseWithoutNewRefreshTokenKeepsItEmpty() {
        // 微软刷新时可能不换发新的 refresh_token：这里返回空串，调用方负责沿用旧的
        val token = BingOAuth.parseTokenResponse("""{"access_token":"AT2","expires_in":3600}""")
        assertEquals("AT2", token!!.accessToken)
        assertEquals("", token.refreshToken)
    }

    @Test
    fun accessTokenIsMandatory() {
        assertNull(BingOAuth.parseTokenResponse(""))
        assertNull(BingOAuth.parseTokenResponse("   "))
        assertNull(BingOAuth.parseTokenResponse("""{"error":"invalid_grant","error_description":"bad code"}"""))
        assertNull(BingOAuth.parseTokenResponse("refresh_token=RT&expires_in=3600"))
        assertNull(BingOAuth.parseTokenResponse("not json at all"))
    }

    @Test
    fun expiresInFallsBackToOneHour() {
        val token = BingOAuth.parseTokenResponse("""{"access_token":"AT"}""")
        assertEquals(3600L, token!!.expiresInSeconds)
    }
}
