package com.functy.fewards.core.mihoyo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** API 契约钉：改 act_id / 域名 / 路径必须同步改测试，避免静默过期。 */
class MihoyoConstantsContractTest {

    @Test
    fun sixGamesPresentWithActIds() {
        assertEquals(6, MihoyoConstants.GAMES.size)
        assertEquals("e202311201442471", MihoyoConstants.GAMES.getValue("genshin").actId)
        assertEquals("e202304121516551", MihoyoConstants.GAMES.getValue("starrail").actId)
        assertEquals("e202406242138391", MihoyoConstants.GAMES.getValue("zzz").actId)
        assertEquals("e202306201626331", MihoyoConstants.GAMES.getValue("honkai3rd").actId)
        assertEquals("e202202251749321", MihoyoConstants.GAMES.getValue("tears").actId)
        assertEquals("e202203291431091", MihoyoConstants.GAMES.getValue("honkai2").actId)
    }

    @Test
    fun zzzUsesNapHost() {
        val zzz = MihoyoConstants.GAMES.getValue("zzz")
        assertTrue(zzz.signUrl.startsWith(MihoyoConstants.ZZZ_ACT_API))
        assertEquals("zzz", zzz.extraHeaders["x-rpc-signgame"])
    }

    @Test
    fun cookieRefreshUsesLegacySTokenPath() {
        assertTrue(MihoyoConstants.STOKEN_COOKIE_URL.contains("getCookieAccountInfoBySToken"))
        assertTrue(MihoyoConstants.COOKIE_TOKEN_BY_STOKEN_URL.contains("getCookieAccountInfoBySToken"))
    }

    @Test
    fun bbsMissionIdsMatchMiyoQian() {
        assertEquals(58, MihoyoConstants.MISSION_SIGN)
        assertEquals(59, MihoyoConstants.MISSION_READ)
        assertEquals(60, MihoyoConstants.MISSION_LIKE)
        assertEquals(61, MihoyoConstants.MISSION_SHARE)
        assertEquals(3, MihoyoConstants.READ_COUNT)
        assertEquals(5, MihoyoConstants.LIKE_COUNT)
        assertEquals(1, MihoyoConstants.SHARE_COUNT)
    }

    @Test
    fun workBuddyPathsStayOnCopilotBillingMeter() {
        assertEquals("https://copilot.tencent.com", com.functy.fewards.core.workbuddy.WorkBuddyEngine.API_BASE)
        assertEquals("/billing/meter/checkin-status", com.functy.fewards.core.workbuddy.WorkBuddyEngine.PATH_STATUS)
        assertEquals("/billing/meter/daily-checkin", com.functy.fewards.core.workbuddy.WorkBuddyEngine.PATH_DAILY)
    }
}
