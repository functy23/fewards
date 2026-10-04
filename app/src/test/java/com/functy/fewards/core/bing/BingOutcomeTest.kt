package com.functy.fewards.core.bing

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 领奖结果判定。
 *
 * 判定口径直接决定「今天到底领到了没有」：把失败当成功会让首页显示一片绿，
 * 把成功当失败会诱导用户反复重试。
 */
class BingOutcomeTest {

    private fun json(text: String): JSONObject = JSONObject(text)

    @Test
    fun unauthorizedMeansTokenExpired() {
        assertEquals(BingClaim.TokenExpired, BingOutcome.interpretClaim(401, JSONObject()))
        assertEquals(BingClaim.TokenExpired, BingOutcome.interpretClaim(403, JSONObject()))
    }

    @Test
    fun creditedReadsPointsFromActivity() {
        val result = BingOutcome.interpretClaim(
            200,
            json("""{"response":{"activity":{"p":15},"balance":1234}}"""),
        )
        assertTrue(result is BingClaim.Credited)
        assertEquals(15, (result as BingClaim.Credited).points)
    }

    @Test
    fun activityWithoutPointsStillCountsAsCredited() {
        val result = BingOutcome.interpretClaim(200, json("""{"response":{"activity":{}}}"""))
        assertTrue(result is BingClaim.Credited)
        assertEquals(0, (result as BingClaim.Credited).points)
    }

    @Test
    fun duplicateIsIdempotentSuccess() {
        assertEquals(
            BingClaim.Duplicate,
            BingOutcome.interpretClaim(200, json("""{"response":{"isDuplicate":true}}""")),
        )
    }

    @Test
    fun duplicateWinsOverMissingActivity() {
        // 重复领取时没有 activity，不能因此判成失败
        assertEquals(
            BingClaim.Duplicate,
            BingOutcome.interpretClaim(200, json("""{"response":{"isDuplicate":true,"balance":100}}""")),
        )
    }

    @Test
    fun topLevelErrorIsRejected() {
        val result = BingOutcome.interpretClaim(200, json("""{"error":"Offer not available"}"""))
        assertTrue(result is BingClaim.Rejected)
        assertEquals("Offer not available", (result as BingClaim.Rejected).reason)
    }

    @Test
    fun topLevelSuccessFalseIsRejected() {
        assertTrue(BingOutcome.interpretClaim(200, json("""{"success":false}""")) is BingClaim.Rejected)
    }

    @Test
    fun responseSuccessFalseIsRejected() {
        val result = BingOutcome.interpretClaim(
            200,
            json("""{"response":{"success":false,"message":"already claimed"}}"""),
        )
        assertTrue(result is BingClaim.Rejected)
        assertEquals("already claimed", (result as BingClaim.Rejected).reason)
    }

    @Test
    fun emptyOrUnconfirmedResponseIsNotTreatedAsSuccess() {
        // 「有 response」不等于「到账」——宁可按失败处理，让日志说清楚
        assertTrue(BingOutcome.interpretClaim(200, JSONObject()) is BingClaim.Rejected)
        assertTrue(BingOutcome.interpretClaim(200, json("""{"response":{}}""")) is BingClaim.Rejected)
    }

    @Test
    fun parsesMeWithBalanceCountryAndPromotions() {
        val me = BingOutcome.parseMe(
            json(
                """
                {"response":{
                  "balance":8888,
                  "profile":{"attributes":{"country":"CN","displayName":"张三"}},
                  "promotions":[
                    {"attributes":{"offerid":"A","type":"urlreward","complete":"false"}},
                    {"attributes":{"offerid":"B","type":"checkin","complete":"true","hidden":"true"}},
                    {"attributes":{"type":"urlreward"}}
                  ]
                }}
                """.trimIndent()
            )
        )
        assertEquals(8888, me!!.balance)
        assertEquals("CN", me.country)
        assertEquals("张三", me.displayName)
        // 第三条没有 offerid，应被丢弃而不是变成一条空任务
        assertEquals(2, me.promotions.size)
    }

    @Test
    fun meWithoutResponseIsNull() {
        assertNull(BingOutcome.parseMe(json("""{"error":"unauthorized"}""")))
    }

    @Test
    fun meWithoutProfileStillParses() {
        val me = BingOutcome.parseMe(json("""{"response":{"balance":10}}"""))
        assertEquals(10, me!!.balance)
        assertEquals("", me.country)
        assertEquals("", me.displayName)
        assertTrue(me.promotions.isEmpty())
    }

    @Test
    fun missingBalanceIsMinusOne() {
        // -1 表示「还不知道」，账号页据此显示「尚未同步」而不是「积分 0」
        assertEquals(-1, BingOutcome.parseMe(json("""{"response":{}}"""))!!.balance)
    }
}
