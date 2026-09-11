package com.functy.fewards.core.workbuddy

import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * WorkBuddy 每日积分领取引擎。
 * 参考 workbuddy-checkin 公开实现：POST copilot.tencent.com/billing/meter/checkin-status 与 /daily-checkin，
 * Bearer token 鉴权，body 为空 JSON `{}`。
 * 幂等：HTTP 400 + body code==10001 = 今日已签（视为成功）；401/403 = token 过期。
 * ⚠️ `today_checked_in` 字段不可靠，仅作快速短路；最终以 daily-checkin 的 code==0/10001 为准。
 * （接口细节可根据实际抓包微调）
 */
class WorkBuddyEngine(
    private val client: OkHttpClient,
    private val accounts: List<AccountRepository.WorkBuddyAccount>,
) {

    companion object {
        const val API_BASE = "https://copilot.tencent.com"
        const val PATH_STATUS = "/billing/meter/checkin-status"
        const val PATH_DAILY = "/billing/meter/daily-checkin"
        private val jsonMedia = "application/json; charset=utf-8".toMediaType()

        data class Outcome(val success: Boolean, val expired: Boolean, val already: Boolean)

        fun interpretStatus(httpStatus: Int, json: JSONObject): Outcome {
            if (httpStatus == 401 || httpStatus == 403) {
                return Outcome(success = false, expired = true, already = false)
            }
            if (httpStatus in 200..299 && json.optJSONObject("data")?.optBoolean("today_checked_in") == true) {
                return Outcome(success = true, expired = false, already = true)
            }
            return Outcome(success = false, expired = false, already = false)
        }

        fun interpretDailyCheckin(httpStatus: Int, json: JSONObject): Outcome {
            if (httpStatus == 401 || httpStatus == 403) {
                return Outcome(success = false, expired = true, already = false)
            }
            val code = json.optInt("code", if (httpStatus in 200..299) 0 else -1)
            return when (code) {
                0 -> Outcome(success = true, expired = false, already = false)
                10001 -> Outcome(success = true, expired = false, already = true)
                else -> Outcome(success = false, expired = false, already = false)
            }
        }
    }

    private fun emit(message: String) = AppLog.i("WB", message)

    private suspend fun post(path: String, token: String): Triple<Int, String, JSONObject> =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url("$API_BASE$path")
                .post("{}".toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $token")
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: "{}"
                val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                Triple(resp.code, body, json)
            }
        }

    /** 执行全部账号签到。返回是否整体成功（幂等已签也算成功）。 */
    suspend fun runAll(onAccountDone: (Int, Int) -> Unit = { _, _ -> }): Boolean =
        withContext(Dispatchers.IO) {
            var allOk = true
            for ((index, account) in accounts.withIndex()) {
                emit("── 账号 ${account.label.ifEmpty { account.id }} ──")
                val ok = checkin(account)
                if (!ok) allOk = false
                onAccountDone(index + 1, accounts.size)
            }
            allOk
        }

    suspend fun checkin(account: AccountRepository.WorkBuddyAccount): Boolean {
        val token = account.token.trim()
        if (token.isEmpty()) {
            emit("WorkBuddy 未配置 token")
            return false
        }

        // 1. 状态查询（只用于省一次请求 + 401 探测）
        val (stStatus, _, stJson) = post(PATH_STATUS, token)
        val statusOutcome = interpretStatus(stStatus, stJson)
        if (statusOutcome.expired) {
            emit("token 已过期（HTTP $stStatus），请打开 WorkBuddy 桌面端刷新登录态")
            return false
        }
        if (statusOutcome.already) {
            emit("今日已签到（状态接口返回），无需重复领取")
            return true
        }
        if (stStatus !in 200..299) {
            emit("状态查询 HTTP $stStatus（继续尝试签到）")
        }

        // 2. 签到（小随机延迟，避免整点风控）
        delay(800 + (System.currentTimeMillis() % 900))

        val (status, body, json) = post(PATH_DAILY, token)
        val outcome = interpretDailyCheckin(status, json)
        return when {
            outcome.expired -> {
                emit("token 已过期（HTTP $status）")
                false
            }
            outcome.already -> {
                emit("今日已签到（code=10001），无需重复领取")
                true
            }
            outcome.success -> {
                val data = json.optJSONObject("data") ?: JSONObject()
                emit("领取成功 credit=${data.opt("credit")}, streak_days=${data.opt("streak_days")}")
                true
            }
            else -> {
                val code = json.optInt("code", -1)
                emit("签到失败 code=$code msg=${json.optString("msg", json.optString("message"))} body=${body.take(200)}")
                false
            }
        }
    }
}
