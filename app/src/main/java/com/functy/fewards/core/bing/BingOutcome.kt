package com.functy.fewards.core.bing

import org.json.JSONObject

/**
 * 领奖结果判定（纯函数，JVM 单测钉这里）。
 *
 * 判定依据来自两处独立实现的交叉核对：
 *  - `response.activity.p` = 本次到账分数；`response.isDuplicate` = 今天已经领过；
 *  - 响应体里出现 `error` / `success == false` / `response.success == false` 都表示被拒绝，
 *    此时**不能**因为「有 response」就当成功。
 */
sealed interface BingClaim {
    /** 本次真的到账。 */
    data class Credited(val points: Int) : BingClaim

    /** 今天已领过（幂等，算成功，不计分）。 */
    data object Duplicate : BingClaim

    /** token 失效，需要刷新后重试。 */
    data object TokenExpired : BingClaim

    /** 被拒绝 / 响应无法确认成功。 */
    data class Rejected(val reason: String) : BingClaim
}

object BingOutcome {

    fun interpretClaim(httpStatus: Int, json: JSONObject): BingClaim {
        if (httpStatus == 401 || httpStatus == 403) return BingClaim.TokenExpired

        val response = json.optJSONObject("response")

        if (json.optBoolean("success", true).not()) {
            return BingClaim.Rejected(json.optString("error").ifEmpty { "success=false" })
        }
        if (json.has("error") && json.optString("error").isNotEmpty()) {
            return BingClaim.Rejected(json.optString("error"))
        }
        if (response != null) {
            if (!response.optBoolean("success", true)) {
                return BingClaim.Rejected(
                    response.optString("message").ifEmpty { "success=false" }
                )
            }
            if (response.optBoolean("isDuplicate", false)) return BingClaim.Duplicate
            val activity = response.optJSONObject("activity")
            if (activity != null) {
                return BingClaim.Credited(activity.optInt("p", 0))
            }
        }
        // HTTP 2xx 但没有 activity / isDuplicate：无法确认到账，按失败处理
        return BingClaim.Rejected("响应未确认到账")
    }

    /** `/dapi/me` 的响应体：`{ "response": { balance, profile, promotions } }`。 */
    data class Me(
        val balance: Int,
        val country: String,
        /** 服务端给出的账号显示名；取不到时为空串，调用方回落到指纹标签。 */
        val displayName: String,
        val promotions: List<BingPromotion>,
    )

    /**
     * 显示名候选字段。
     *
     * ⚠️ 只有 `profile.attributes.country` 是社区实现里被明确引用过的字段，
     * 其余是**防御性候选**：不同地区 / 账号类型下 dapi 的 profile 结构并不一致，
     * 这里按顺序取第一个非空值，全都取不到就返回空串、由调用方用不可逆指纹兜底。
     * 不要在别处依赖这些字段一定存在。
     */
    private val DISPLAY_NAME_KEYS = listOf(
        "displayName", "nickname", "name", "userName", "email", "anonymizedId",
    )

    private fun displayNameOf(attributes: JSONObject?): String {
        if (attributes == null) return ""
        for (key in DISPLAY_NAME_KEYS) {
            val value = attributes.optString(key)
            if (value.isNotEmpty()) return value
        }
        return ""
    }

    fun parseMe(json: JSONObject): Me? {
        val response = json.optJSONObject("response") ?: return null
        val attributes = response.optJSONObject("profile")?.optJSONObject("attributes")
        return Me(
            balance = response.optInt("balance", -1),
            country = attributes?.optString("country").orEmpty(),
            displayName = displayNameOf(attributes),
            promotions = response.optJSONArray("promotions")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let { BingPromotions.parse(it) }
                }
            }.orEmpty(),
        )
    }
}
