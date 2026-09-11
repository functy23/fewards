package com.functy.fewards.core.mihoyo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS 黄金对照：期望值由 Python hashlib 按 MiyoQian crypto.py 公式离线计算。 */
class DsSignTest {

    private val t = "1725000000"

    @Test
    fun dsAppSaltMatchesPythonGolden() {
        assertEquals("1725000000,abc123,fd87cc7d1f01c0e3759002af5b4097cd", DsSign.ds(web = false, t = t, r = "abc123"))
    }

    @Test
    fun dsWebSaltMatchesPythonGolden() {
        assertEquals("1725000000,def456,8d7ea5947ca8bd83cbb49ebec4df28b3", DsSign.ds(web = true, t = t, r = "def456"))
    }

    @Test
    fun dsX6IncludesBodyBeforeQuery() {
        val body = """{"gids":"5"}"""
        assertEquals("1725000000,123456,693a792157e4c050c39ca3789f5ee8fc", DsSign.dsX6(t = t, r = "123456", body = body))
    }

    @Test
    fun dsX4MatchesPythonGolden() {
        assertEquals("1725000000,654321,a1a5f393aa6eaf0cebb6518570c78c5b", DsSign.dsX4(query = "foo=1", t = t, r = "654321"))
    }

    @Test
    fun dsAppMatchesPythonGolden() {
        assertEquals("1725000000,150000,d72d2fffac27f590ccecc70505b71423", DsSign.dsApp(body = "{}", t = t, r = "150000"))
    }

    @Test
    fun liveDsHasThreeCommaSeparatedParts() {
        val value = DsSign.ds()
        assertEquals(3, value.split(",").size)
        assertTrue(value.split(",")[2].matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun deviceIdIsStableForSameSeed() {
        assertEquals(DsSign.deviceId("fewards"), DsSign.deviceId("fewards"))
    }

    @Test
    fun uaContainsBbsVersion() {
        assertTrue(DsSign.DEFAULT_MOBILE_UA.contains(DsSign.BBS_VERSION))
        assertTrue(DsSign.QR_MOBILE_UA.contains(DsSign.QR_LOGIN_VERSION))
    }
}
