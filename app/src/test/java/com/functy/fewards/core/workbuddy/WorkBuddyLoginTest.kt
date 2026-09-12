package com.functy.fewards.core.workbuddy

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 钉 WorkBuddy 扫码授权协议的解析与判定：
 * 腾讯信封 code 优先、未扫码码 11217、驼峰 token 字段、缺 uid 视为未就绪。
 */
class WorkBuddyLoginTest {

    @Test
    fun parsesAuthState() {
        val json = JSONObject(
            """{"code":0,"msg":"OK","data":{"state":"abc","authUrl":"https://copilot.tencent.com/login?platform=CLI&state=abc"}}"""
        )
        val session = WorkBuddyLogin.parseAuthState(json)
        assertEquals("abc", session?.state)
        assertEquals("https://copilot.tencent.com/login?platform=CLI&state=abc", session?.authUrl)
    }

    @Test
    fun rejectsAuthStateWhenCodeNonZero() {
        val json = JSONObject("""{"code":500,"msg":"boom","data":{"state":"abc","authUrl":"https://x"}}""")
        assertNull(WorkBuddyLogin.parseAuthState(json))
    }

    @Test
    fun rejectsAuthStateWhenStateMissing() {
        val json = JSONObject("""{"code":0,"data":{"authUrl":"https://x"}}""")
        assertNull(WorkBuddyLogin.parseAuthState(json))
    }

    @Test
    fun unscannedStateIsWaiting() {
        // 实测未扫码返回：{"code":11217,"msg":"11217:login ing..."}
        val json = JSONObject("""{"code":11217,"msg":"11217:login ing..."}""")
        val code = WorkBuddyLogin.envelopeCode(200, json)
        assertEquals(11217, code)
        assertTrue(WorkBuddyLogin.isWaiting(code))
        assertNull(WorkBuddyLogin.parseAuthToken(json))
    }

    @Test
    fun parsesCamelCaseTokenFields() {
        val json = JSONObject(
            """{"code":0,"data":{"accessToken":"eyJ.a.b","refreshToken":"eyJ.c.d","expiresIn":3600,"domain":"https://x"}}"""
        )
        val payload = WorkBuddyLogin.parseAuthToken(json)
        assertEquals("eyJ.a.b", payload?.accessToken)
        assertEquals("eyJ.c.d", payload?.refreshToken)
        assertEquals(3600, payload?.expiresIn)
        assertEquals("https://x", payload?.domain)
    }

    @Test
    fun defaultsExpiresInWhenMissing() {
        val json = JSONObject("""{"code":0,"data":{"accessToken":"t"}}""")
        assertEquals(3600, WorkBuddyLogin.parseAuthToken(json)?.expiresIn)
    }

    @Test
    fun rejectsTokenWithoutAccessToken() {
        assertNull(WorkBuddyLogin.parseAuthToken(JSONObject("""{"code":0,"data":{"refreshToken":"r"}}""")))
    }

    @Test
    fun parsesLoginAccount() {
        val json = JSONObject(
            """{"code":0,"data":{"uid":"u-1","nickname":"小明","enterpriseId":"ent-9"}}"""
        )
        val info = WorkBuddyLogin.parseLoginAccount(json)
        assertEquals("u-1", info?.uid)
        assertEquals("小明", info?.nickname)
        assertEquals("ent-9", info?.enterpriseId)
    }

    @Test
    fun accountWithoutUidIsNotReady() {
        assertNull(WorkBuddyLogin.parseLoginAccount(JSONObject("""{"code":0,"data":{"nickname":"小明"}}""")))
    }

    @Test
    fun envelopeFallsBackToHttpStatus() {
        assertEquals(502, WorkBuddyLogin.envelopeCode(502, JSONObject("""{"msg":"bad gateway"}""")))
    }

    @Test
    fun missingCodeFieldIsNeverTreatedAsSuccess() {
        // 空对象 / 非 JSON 响应（网络异常、网关 HTML）不能因为「没有 code」被当成授权成功
        assertFalse(WorkBuddyLogin.hasOkCode(JSONObject()))
        assertFalse(WorkBuddyLogin.hasOkCode(JSONObject("""{"msg":"ok"}""")))
        assertNull(WorkBuddyLogin.parseAuthState(JSONObject()))
        assertNull(WorkBuddyLogin.parseAuthToken(JSONObject()))
        assertNull(WorkBuddyLogin.parseLoginAccount(JSONObject()))
        assertTrue(WorkBuddyLogin.hasOkCode(JSONObject("""{"code":0}""")))
    }

    @Test
    fun protocolPathsAndBaseAreFrozen() {
        // 契约：移植自 workbuddy-manager server/services/tencent.py
        assertEquals("https://copilot.tencent.com", WorkBuddyLogin.API_BASE)
        assertEquals("/v2/plugin/auth/state", WorkBuddyLogin.PATH_AUTH_STATE)
        assertEquals("/v2/plugin/auth/token", WorkBuddyLogin.PATH_AUTH_TOKEN)
        assertEquals("/v2/plugin/login/account", WorkBuddyLogin.PATH_LOGIN_ACCOUNT)
        assertEquals(11217, WorkBuddyLogin.CODE_WAITING)
        assertEquals(300_000L, WorkBuddyLogin.STATE_TTL_MS)
        // 扫码平台参数固定 CLI（服务端只认这个）
        assertTrue(WorkBuddyLogin.HEADERS["User-Agent"]!!.startsWith("CLI/"))
    }

    @Test
    fun isWaitingOnlyForNonZeroCode() {
        assertTrue(WorkBuddyLogin.isWaiting(11217))
        assertTrue(WorkBuddyLogin.isWaiting(-1))
        assertFalse(WorkBuddyLogin.isWaiting(0))
    }
}
