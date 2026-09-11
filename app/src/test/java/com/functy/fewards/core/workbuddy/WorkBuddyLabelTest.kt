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
}
