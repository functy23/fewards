package com.functy.fewards.core.bing

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * promotion 解析与「能否一键领取」的判定。
 *
 * 误判成可领 → 发出一堆注定失败的请求；误判成不可领 → 用户当天少拿分。
 */
class BingPromotionTest {

    private fun promo(
        type: String = "urlreward",
        complete: Boolean = false,
        hidden: Boolean = false,
        offerIdKey: String = "offerid",
        progress: Any? = 0,
        max: Any? = 10,
    ): BingPromotion {
        val attrs = JSONObject()
            .put(offerIdKey, "OFFER_1")
            .put("type", type)
            .put("title", "每日活动")
            .put("complete", complete.toString())
            .put("hidden", hidden.toString())
        if (progress != null) attrs.put("progress", progress)
        if (max != null) attrs.put("max", max)
        return BingPromotions.parse(JSONObject().put("attributes", attrs))!!
    }

    @Test
    fun parsesAttributesBlock() {
        val p = promo(type = "urlreward", progress = "3", max = "10")
        assertEquals("OFFER_1", p.offerId)
        assertEquals("urlreward", p.type)
        assertEquals("每日活动", p.title)
        assertEquals(3, p.progress)
        assertEquals(10, p.max)
        assertFalse(p.complete)
    }

    @Test
    fun acceptsCamelCaseOfferId() {
        assertEquals("OFFER_1", promo(offerIdKey = "offerId").offerId)
    }

    @Test
    fun acceptsTopLevelFieldsWithoutAttributes() {
        val p = BingPromotions.parse(JSONObject().put("offerid", "TOP").put("type", "checkin"))
        assertEquals("TOP", p!!.offerId)
        assertEquals("checkin", p.type)
    }

    @Test
    fun typeIsLowercased() {
        assertEquals("urlreward", promo(type = "UrlReward").type)
    }

    @Test
    fun missingOfferIdIsRejected() {
        assertNull(BingPromotions.parse(JSONObject().put("attributes", JSONObject().put("type", "checkin"))))
    }

    @Test
    fun displayNameFallsBackToTitleThenNameThenOfferId() {
        assertEquals("每日活动", promo().displayName)
        assertEquals(
            "OFFER_1",
            BingPromotions.parse(JSONObject().put("offerid", "OFFER_1"))!!.displayName
        )
    }

    @Test
    fun completedCardIsNeverClaimable() {
        assertFalse(BingPromotions.isClaimable(promo(complete = true)))
        assertFalse(BingPromotions.isClaimable(promo(type = "checkin", complete = true, hidden = true)))
    }

    @Test
    fun dailyCheckInIsClaimableEvenThoughHidden() {
        // 每日签到在 dapi 里被标成 hidden（渲染在独立挂件里），但它确实可领
        assertTrue(BingPromotions.isClaimable(promo(type = "checkin", hidden = true)))
    }

    @Test
    fun hiddenNonCheckInIsNotClaimable() {
        assertFalse(BingPromotions.isClaimable(promo(type = "urlreward", hidden = true)))
        assertFalse(BingPromotions.isClaimable(promo(type = "msnreadearn", hidden = true)))
    }

    @Test
    fun urlrewardAndReadEarnAreClaimable() {
        assertTrue(BingPromotions.isClaimable(promo(type = "urlreward")))
        assertTrue(BingPromotions.isClaimable(promo(type = "msnreadearn")))
    }

    @Test
    fun searchAndUnknownTypesAreNotClaimable() {
        // 搜索分不能靠 claim 领；本 App 也明确不做搜索
        assertFalse(BingPromotions.isClaimable(promo(type = "search")))
        assertFalse(BingPromotions.isClaimable(promo(type = "streak")))
        assertFalse(BingPromotions.isClaimable(promo(type = "")))
    }
}
