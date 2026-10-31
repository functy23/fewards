package com.functy.fewards.core.workbuddy

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 钉 WorkBuddy 自动续期协议（与 dsh-workbuddy-panel 内核 workbuddy2api 的 RefreshToken 同源）：
 * 提前量判定、信封解析、12153 终态、X-Refresh-Token 只出现在刷新端点。
 */
class WorkBuddyAuthTest {

    private val at = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1LTEifQ.sig-at"
    private val rt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1LTEifQ.sig-rt"

    @Test
    fun refreshesOnlyWhenRefreshTokenExists() {
        val now = 1_700_000_000L
        // 没有 refreshToken：永远不刷新（手填 accessToken 的账号只能用到过期）
        assertFalse(WorkBuddyAuth.needsRefresh(at, "", 0L, now))
        // 有 RT 但没 AT / 过期时间未知：先换一次
        assertTrue(WorkBuddyAuth.needsRefresh("", rt, 0L, now))
        assertTrue(WorkBuddyAuth.needsRefresh(at, rt, 0L, now))
        // 还早
        assertFalse(WorkBuddyAuth.needsRefresh(at, rt, now + 7200, now))
        // 提前 10 分钟内就要换
        assertTrue(WorkBuddyAuth.needsRefresh(at, rt, now + 600, now))
        assertTrue(WorkBuddyAuth.needsRefresh(at, rt, now - 1, now))
    }

    @Test
    fun parsesCamelCaseRefreshResponse() {
        val json = JSONObject(
            """{"code":0,"msg":"ok","data":{"accessToken":"at-new","refreshToken":"rt-new","expiresIn":5184000,"domain":"copilot.tencent.com"}}"""
        )
        val result = WorkBuddyAuth.parseRefresh(200, json, nowSeconds = 1_000L)
        assertTrue(result is WorkBuddyAuth.Result.Success)
        val tokens = (result as WorkBuddyAuth.Result.Success).tokens
        assertEquals("at-new", tokens.accessToken)
        assertEquals("rt-new", tokens.refreshToken)
        assertEquals(1_000L + 5_184_000L, tokens.expiresAt)
        assertEquals("copilot.tencent.com", tokens.domain)
    }

    @Test
    fun missingExpiresInKeepsExpiryUnknown() {
        val json = JSONObject("""{"code":0,"data":{"accessToken":"at-new"}}""")
        val result = WorkBuddyAuth.parseRefresh(200, json, 1_000L)
        val tokens = (result as WorkBuddyAuth.Result.Success).tokens
        assertEquals(0L, tokens.expiresAt)
        assertEquals("", tokens.refreshToken)
    }

    @Test
    fun sessionDeadCodeIsTerminal() {
        val json = JSONObject("""{"code":12153,"msg":"refresh token is invalid"}""")
        assertEquals(WorkBuddyAuth.Result.SessionDead, WorkBuddyAuth.parseRefresh(200, json))
        // HTTP 401 / 403 且没有业务信封同样按会话失效处理
        assertEquals(WorkBuddyAuth.Result.SessionDead, WorkBuddyAuth.parseRefresh(401, JSONObject()))
        assertEquals(WorkBuddyAuth.Result.SessionDead, WorkBuddyAuth.parseRefresh(403, JSONObject()))
    }

    @Test
    fun otherFailuresAreNotTerminal() {
        assertTrue(WorkBuddyAuth.parseRefresh(200, JSONObject("""{"code":500,"msg":"boom"}""")) is WorkBuddyAuth.Result.Failed)
        // 缺 code 字段不能当成功
        assertTrue(WorkBuddyAuth.parseRefresh(200, JSONObject()) is WorkBuddyAuth.Result.Failed)
        // 缺 accessToken 也算失败（刷新必须换出可用的 AT）
        assertTrue(
            WorkBuddyAuth.parseRefresh(200, JSONObject("""{"code":0,"data":{"refreshToken":"r"}}"""))
                is WorkBuddyAuth.Result.Failed
        )
    }

    @Test
    fun refreshHeadersCarryTheRotatingToken() {
        val headers = WorkBuddyAuth.refreshHeaders(at, rt, "u-1")
        assertEquals(rt, headers["X-Refresh-Token"])
        assertEquals("plugin", headers["X-Auth-Refresh-Source"])
        assertEquals("Bearer " + at, headers["Authorization"])
        assertEquals("u-1", headers["X-User-Id"])
        // 安全红线：X-Refresh-Token 只允许出现在刷新端点，扫码 / 签到用的公共头里不能有
        assertFalse(WorkBuddyLogin.HEADERS.containsKey("X-Refresh-Token"))
        // 没有 accessToken 时不带 Authorization，让上游用 12153 明确拒绝
        assertFalse(WorkBuddyAuth.refreshHeaders("", rt, "").containsKey("Authorization"))
        assertFalse(WorkBuddyAuth.refreshHeaders("", rt, "").containsKey("X-User-Id"))
    }

    @Test
    fun protocolPathIsFrozen() {
        assertEquals("/v2/plugin/auth/token/refresh", WorkBuddyAuth.PATH_REFRESH)
        assertEquals(12153, WorkBuddyAuth.CODE_SESSION_DEAD)
        assertEquals(600L, WorkBuddyAuth.REFRESH_SKEW_SECONDS)
    }
}
