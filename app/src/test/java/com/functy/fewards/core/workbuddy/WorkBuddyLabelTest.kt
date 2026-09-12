package com.functy.fewards.core.workbuddy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class WorkBuddyLabelTest {

    private fun jwt(payload: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"alg":"none"}""".toByteArray())
        val body = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "$header.$body.sig"
    }

    @Test
    fun prefersNicknameOverUsername() {
        val token = jwt("""{"nickname":"  Alice  ","preferred_username":"13800000000"}""")
        assertEquals("Alice", WorkBuddyLabel.decode(token))
    }

    @Test
    fun fallsBackToPreferredUsername() {
        val token = jwt("""{"preferred_username":"13800000000"}""")
        assertEquals("13800000000", WorkBuddyLabel.decode(token))
    }

    @Test
    fun garbageReturnsNull() {
        assertNull(WorkBuddyLabel.decode("not-a-jwt"))
    }

    @Test
    fun decodeProfileReadsHttpPicture() {
        val token = jwt("""{"nickname":"Bob","picture":"https://cdn.example/a.png"}""")
        val profile = WorkBuddyLabel.decodeProfile(token)
        assertEquals("Bob", profile.label)
        assertEquals("https://cdn.example/a.png", profile.avatarUrl)
    }

    @Test
    fun decodeProfileIgnoresNonHttpPicture() {
        val token = jwt("""{"nickname":"Bob","picture":"not-a-url"}""")
        assertNull(WorkBuddyLabel.decodeProfile(token).avatarUrl)
    }
}
