package com.functy.fewards.core.mihoyo

import com.functy.fewards.data.repository.AccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MihoyoCookieTest {

    private val v2 = "stoken=v2_abc; mid=xyz123; account_id=10001; cookie_token=tok"

    @Test
    fun parseCookieRequiresStokenAndUid() {
        val parsed = MihoyoApi.parseCookie(v2)!!
        assertEquals("v2_abc", parsed.stoken)
        assertEquals("10001", parsed.stuid)
        assertEquals("xyz123", parsed.mid)
    }

    @Test
    fun parseCookieRejectsMissingStoken() {
        assertNull(MihoyoApi.parseCookie("account_id=1; mid=x"))
    }

    @Test
    fun parseCookieRejectsMissingUid() {
        assertNull(MihoyoApi.parseCookie("stoken=v2_abc; mid=x"))
    }

    @Test
    fun guessUidFallsBackThroughKnownKeys() {
        assertEquals("9", MihoyoApi.guessUid("ltuid=9"))
        assertEquals("8", MihoyoApi.guessUid("stuid=8"))
        assertEquals("7", MihoyoApi.guessUid("login_uid=7"))
    }

    @Test
    fun stokenCookieRequiresMidForV2() {
        val account = AccountRepository.MihoyoAccount(
            id = "1", nickname = "n", stoken = "v2_abc", stuid = "10001", mid = "", cookie = ""
        )
        val err = runCatching { MihoyoApi.stokenCookie(account) }.exceptionOrNull()
        assertTrue(err is IllegalArgumentException)
        assertTrue(err!!.message!!.contains("mid"))
    }

    @Test
    fun stokenCookieEmitsStuidStokenMid() {
        val account = AccountRepository.MihoyoAccount(
            id = "1", nickname = "n", stoken = "v2_abc", stuid = "10001", mid = "xyz123", cookie = v2
        )
        assertEquals("stuid=10001;stoken=v2_abc;mid=xyz123", MihoyoApi.stokenCookie(account))
    }

    @Test
    fun replaceCookieTokenKeepsOtherFields() {
        val api = MihoyoApi(okhttp3.OkHttpClient())
        val next = api.replaceOrAppendCookieValue(v2, "cookie_token", "NEW")
        assertTrue(next.contains("cookie_token=NEW"))
        assertTrue(next.contains("stoken=v2_abc"))
    }
}
