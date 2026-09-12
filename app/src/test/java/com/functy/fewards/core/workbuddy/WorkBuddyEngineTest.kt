package com.functy.fewards.core.workbuddy

import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkBuddyEngineTest {

    @Test
    fun dailyCheckinCodeZeroIsSuccess() {
        val json = JSONObject("""{"code":0,"data":{"credit":10}}""")
        val out = WorkBuddyEngine.interpretDailyCheckin(200, json)
        assertTrue(out.success)
        assertFalse(out.already)
        assertFalse(out.expired)
    }

    @Test
    fun dailyCheckinCode10001IsAlreadySignedSuccess() {
        val json = JSONObject("""{"code":10001,"msg":"already"}""")
        val out = WorkBuddyEngine.interpretDailyCheckin(400, json)
        assertTrue(out.success)
        assertTrue(out.already)
    }

    @Test
    fun http401IsExpired() {
        val out = WorkBuddyEngine.interpretDailyCheckin(401, JSONObject())
        assertTrue(out.expired)
        assertFalse(out.success)
        val status = WorkBuddyEngine.interpretStatus(403, JSONObject())
        assertTrue(status.expired)
    }

    @Test
    fun statusTodayCheckedInIsAlready() {
        val json = JSONObject("""{"data":{"today_checked_in":true}}""")
        val out = WorkBuddyEngine.interpretStatus(200, json)
        assertTrue(out.success)
        assertTrue(out.already)
    }

    @Test
    fun unknownCodeIsFailure() {
        val json = JSONObject("""{"code":999,"msg":"no"}""")
        val out = WorkBuddyEngine.interpretDailyCheckin(200, json)
        assertFalse(out.success)
        assertFalse(out.already)
    }
}
