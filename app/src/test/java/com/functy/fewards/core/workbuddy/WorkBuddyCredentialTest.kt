package com.functy.fewards.core.workbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 钉「复制 / 粘贴」凭据格式：
 * 复制固定输出 AccessToken / RefreshToken 两行；粘贴侧认单行折叠版、拼写容错、JSON 与裸 JWT。
 */
class WorkBuddyCredentialTest {

    private val at = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1LTEifQ.sig-at"
    private val rt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1LTEifQ.sig-rt"

    @Test
    fun formatsBothTokensOnTwoLines() {
        assertEquals(
            "AccessToken: " + at + "\n" + "RefreshToken: " + rt,
            WorkBuddyCredential.format(at, rt),
        )
    }

    @Test
    fun formatsAccessTokenOnlyWhenRefreshTokenMissing() {
        assertEquals("AccessToken: " + at, WorkBuddyCredential.format(at, "  "))
        assertEquals("", WorkBuddyCredential.format("", ""))
    }

    @Test
    fun parsesItsOwnOutput() {
        val parsed = WorkBuddyCredential.parse(WorkBuddyCredential.format(at, rt))
        assertEquals(at, parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
        assertTrue(parsed!!.hasRefreshToken)
    }

    @Test
    fun parsesSingleLineFoldedByTextField() {
        // miuix 输入框是 singleLine：粘贴进来的换行会被折叠成空格
        val parsed = WorkBuddyCredential.parse("AccessToken: " + at + " RefreshToken: " + rt)
        assertEquals(at, parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
    }

    @Test
    fun toleratesMisspelledLabelAndEqualsSign() {
        // 用户手写时常见的 AcessToken 拼写 + "=" 分隔 + 分号分隔
        val parsed = WorkBuddyCredential.parse("AcessToken=" + at + ";RefreshToken=" + rt)
        assertEquals(at, parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
    }

    @Test
    fun parsesSnakeCaseJsonExport() {
        val parsed = WorkBuddyCredential.parse("""{"access_token":"$at","refresh_token":"$rt"}""")
        assertEquals(at, parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
    }

    @Test
    fun parsesRawJwtAsAccessTokenOnly() {
        val parsed = WorkBuddyCredential.parse("  " + at + "  ")
        assertEquals(at, parsed?.accessToken)
        assertEquals("", parsed?.refreshToken)
        assertFalse(parsed!!.hasRefreshToken)
    }

    @Test
    fun parsesTwoBareTokensInOrder() {
        val parsed = WorkBuddyCredential.parse(at + " " + rt)
        assertEquals(at, parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
    }

    @Test
    fun stripsBearerPrefix() {
        val parsed = WorkBuddyCredential.parse("Bearer " + at)
        assertEquals(at, parsed?.accessToken)
    }

    @Test
    fun refreshTokenOnlyPasteIsRecognized() {
        val parsed = WorkBuddyCredential.parse("RefreshToken: " + rt)
        assertEquals("", parsed?.accessToken)
        assertEquals(rt, parsed?.refreshToken)
        assertTrue(parsed!!.hasRefreshToken)
    }

    @Test
    fun emptyOrPunctuationOnlyTextIsRejected() {
        assertNull(WorkBuddyCredential.parse("   "))
        assertNull(WorkBuddyCredential.parse("{}"))
        assertNull(WorkBuddyCredential.parse("Bearer"))
    }
}
