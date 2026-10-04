package com.functy.fewards.core.bing

import com.functy.fewards.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlin.random.Random

/**
 * Microsoft Rewards dapi 客户端。
 *
 *  - GET  /dapi/me?channel=SAAndroid&options=511  -> 余额 + 全部 promotion（任务清单）
 *  - POST /dapi/me/activities  {type:101, attributes:{offerid}} -> 领取活动/打卡卡
 *  - POST /dapi/me/activities  {type:103, channel:SAAndroid}    -> Bing App 每日连签
 *
 * 只做「领取」类操作；搜索分不在本客户端能力范围内（也不在本 App 范围内）。
 * 日志绝不打印 token。
 */
class BingRewardsApi(
    private val client: OkHttpClient,
    private val accessToken: String,
    private val country: String,
) {

    companion object {
        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        /** 32 字节随机十六进制活动 id（64 字符），与社区实现一致。 */
        fun randomActivityId(random: Random = Random.Default): String {
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** 领奖 payload。 */
        fun claimPayload(offerId: String, country: String, activityId: String): String = JSONObject()
            .put("amount", 1)
            .put("country", country)
            .put("id", activityId)
            .put("type", 101)
            .put("attributes", JSONObject().put("offerid", offerId))
            .toString()

        /** App 每日连签 payload（type 103，无 offerid）。 */
        fun checkInPayload(country: String, activityId: String): String = JSONObject()
            .put("amount", 1)
            .put("country", country)
            .put("id", activityId)
            .put("type", 103)
            .put("channel", BingConstants.CHANNEL)
            .put("attributes", JSONObject())
            .put("risk_context", JSONObject())
            .toString()
    }

    private fun baseHeaders(): Map<String, String> = mapOf(
        "Authorization" to "Bearer " + accessToken,
        "X-Rewards-Country" to country,
        "X-Rewards-Language" to "zh",
        "X-Rewards-AppId" to BingConstants.APP_ID,
        "X-Rewards-IsMobile" to "true",
        "Accept" to "application/json",
        "User-Agent" to "Bing/31.4.2110003555 (Android)",
    )

    private fun get(url: String): Triple<Int, String, JSONObject> {
        val req = Request.Builder().url(url)
            .apply { baseHeaders().forEach { (k, v) -> header(k, v) } }
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body.string()
            return Triple(resp.code, text, runCatching { JSONObject(text) }.getOrElse { JSONObject() })
        }
    }

    private fun post(
        url: String,
        payload: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): Triple<Int, String, JSONObject> {
        val req = Request.Builder().url(url)
            .apply {
                baseHeaders().forEach { (k, v) -> header(k, v) }
                extraHeaders.forEach { (k, v) -> header(k, v) }
            }
            .post(payload.toRequestBody(jsonMedia))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body.string()
            return Triple(resp.code, text, runCatching { JSONObject(text) }.getOrElse { JSONObject() })
        }
    }

    private fun meUrl(): String =
        BingConstants.DAPI_BASE + "/me?channel=" + BingConstants.CHANNEL + "&options=" + BingConstants.OPTIONS

    private fun activitiesUrl(): String = BingConstants.DAPI_BASE + "/me/activities"

    /** 拉取余额 + 任务清单。失败返回 null。 */
    suspend fun fetchMe(): BingOutcome.Me? = withContext(Dispatchers.IO) {
        val (status, body, json) = get(meUrl())
        if (status !in 200..299) {
            AppLog.w(
                "BING",
                "拉取 Rewards 数据失败 HTTP " + status + if (body.isNotEmpty()) " body=" + body.take(160) else ""
            )
            return@withContext null
        }
        BingOutcome.parseMe(json).also {
            if (it == null) AppLog.w("BING", "Rewards 响应无法解析：" + body.take(160))
        }
    }

    /** 领取一个活动。 */
    suspend fun claim(offerId: String): BingClaim = withContext(Dispatchers.IO) {
        val (status, body, json) = post(activitiesUrl(), claimPayload(offerId, country, randomActivityId()))
        val outcome = BingOutcome.interpretClaim(status, json)
        if (outcome is BingClaim.Rejected) {
            AppLog.w("BING", "领取失败 " + offerId + " HTTP " + status + " " + body.take(160))
        }
        outcome
    }

    /** Bing App 每日连签（type 103）。 */
    suspend fun appCheckIn(): BingClaim = withContext(Dispatchers.IO) {
        val (status, body, json) = post(
            activitiesUrl(),
            checkInPayload(country, randomActivityId()),
            extraHeaders = mapOf(
                "X-Rewards-AppId" to BingConstants.CHANNEL,
                "X-Rewards-PartnerId" to "startapp",
            ),
        )
        val outcome = BingOutcome.interpretClaim(status, json)
        if (outcome is BingClaim.Rejected) {
            AppLog.w("BING", "App 连签失败 HTTP " + status + " " + body.take(160))
        }
        outcome
    }

    /** 领奖之间的小间隔，避免连点被风控。 */
    suspend fun settleDelay() {
        delay(Random.nextLong(BingConstants.CLAIM_DELAY_MIN_MS, BingConstants.CLAIM_DELAY_MAX_MS))
    }
}
