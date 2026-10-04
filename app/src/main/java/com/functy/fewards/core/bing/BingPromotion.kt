package com.functy.fewards.core.bing

import org.json.JSONObject

/**
 * 一条 Rewards 活动 / 卡片。
 *
 * dapi 把真实字段都塞在 `attributes` 里；不同卡片大小写并不统一
 * （`offerid` / `offerId`，`progress` / `activityprogress`），解析时两种都认。
 */
data class BingPromotion(
    val offerId: String,
    val name: String,
    val type: String,
    val title: String,
    val complete: Boolean,
    val progress: Int,
    val max: Int,
    val hidden: Boolean,
    val classificationTag: String,
) {
    val displayName: String get() = title.ifEmpty { name.ifEmpty { offerId } }
}

object BingPromotions {

    private fun attrsOf(o: JSONObject): JSONObject =
        o.optJSONObject("attributes") ?: o

    private fun str(a: JSONObject, vararg keys: String): String {
        for (k in keys) {
            val v = a.optString(k)
            if (v.isNotEmpty()) return v
        }
        return ""
    }

    private fun bool(a: JSONObject, vararg keys: String): Boolean {
        for (k in keys) {
            if (!a.has(k)) continue
            val raw = a.opt(k)
            when {
                raw is Boolean -> return raw
                raw is String -> return raw.equals("true", ignoreCase = true)
                raw is Number -> return raw.toInt() != 0
            }
        }
        return false
    }

    private fun num(a: JSONObject, vararg keys: String): Int {
        for (k in keys) {
            if (!a.has(k)) continue
            val raw = a.opt(k)
            val n = when {
                raw is Number -> raw.toInt()
                raw is String -> raw.toIntOrNull()
                else -> null
            }
            if (n != null) return n
        }
        return 0
    }

    /** 解析单条 promotion。无 offerid 视为无效，返回 null。 */
    fun parse(o: JSONObject): BingPromotion? {
        val a = attrsOf(o)
        val offerId = str(a, "offerid", "offerId", "offer_id")
        if (offerId.isEmpty()) return null
        return BingPromotion(
            offerId = offerId,
            name = str(a, "name"),
            type = str(a, "type").lowercase(),
            title = str(a, "title"),
            complete = bool(a, "complete"),
            progress = num(a, "progress", "activityprogress"),
            max = num(a, "max", "activitymax"),
            hidden = bool(a, "hidden"),
            classificationTag = str(a, "Classification.Tag", "AnswerScenario.Tag"),
        )
    }

    /**
     * 能否用一次 claim 领掉。
     *
     * 规则来自对社区实现（SkyBlue997 的 RewardsApi.isClaimable）的核对：
     *  - 已完成 → 不用领；
     *  - `checkin`（每日签到）在 dapi 里被标成 hidden（它渲染在独立挂件里），但它确实可领，放行；
     *  - 其余 hidden → 不可领（信息类 / 抽奖类占位）；
     *  - 只有 `urlreward`（每日活动 / 打卡）与 `msnreadearn`（阅读赚分）能一次领掉。
     *
     * 搜索分（PCSearch / MobileSearch）**不能**这样领，本 App 也不做搜索。
     */
    fun isClaimable(p: BingPromotion): Boolean {
        if (p.complete) return false
        if (p.type == TYPE_CHECKIN) return true
        if (p.hidden) return false
        return p.type == TYPE_URL_REWARD || p.type == TYPE_MSN_READ_EARN
    }

    const val TYPE_CHECKIN = "checkin"
    const val TYPE_URL_REWARD = "urlreward"
    const val TYPE_MSN_READ_EARN = "msnreadearn"
}
