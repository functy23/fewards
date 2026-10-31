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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkBuddy 每日积分领取引擎。
 * 参考 workbuddy-checkin 公开实现：POST copilot.tencent.com/billing/meter/checkin-status 与 /daily-checkin，
 * Bearer token 鉴权，body 为空 JSON {}。
 * 幂等：HTTP 400 + body code==10001 = 今日已签（视为成功）；401/403 = token 过期。
 * 注意：today_checked_in 字段不可靠，仅作快速短路；最终以 daily-checkin 的 code==0/10001 为准。
 *
 * 自动续期（refreshToken -> 新 accessToken，见 [WorkBuddyAuth]）：
 *  1. 签到前先看 accessToken 是否临近过期，临近就先换（不把过期瞬间压在签到请求上）；
 *  2. 签到途中撞 401/403，用 refreshToken 再换一次并重试一轮；
 *  3. 换到的新 token 一律经 [onTokensRefreshed] 写回账号仓储——refreshToken 会轮换，
 *     只留在内存里等于把旧 RT 留在磁盘上，下次刷新必然 12153。
 */
class WorkBuddyEngine(
    private val client: OkHttpClient,
    private val accounts: List<AccountRepository.WorkBuddyAccount>,
    private val auth: WorkBuddyAuth = WorkBuddyAuth(client),
    /** 续期成功后把新凭据写回仓储；默认空实现（单测 / 预览用）。 */
    private val onTokensRefreshed: suspend (AccountRepository.WorkBuddyAccount) -> Unit = {},
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
                .url(API_BASE + path)
                .post("{}".toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body.string()
                val json = runCatching { JSONObject(body) }.getOrElse { JSONObject() }
                Triple(resp.code, body, json)
            }
        }

    /** 执行全部账号签到。返回是否整体成功（幂等已签也算成功）。 */
    suspend fun runAll(onAccountDone: (Int, Int) -> Unit = { _, _ -> }): Boolean =
        withContext(Dispatchers.IO) {
            var allOk = true
            for ((index, account) in accounts.withIndex()) {
                emit("── 账号 " + account.label.ifEmpty { account.id } + " ──")
                val ok = checkin(account)
                if (!ok) allOk = false
                onAccountDone(index + 1, accounts.size)
            }
            allOk
        }

    /** 一次续期的结果。 */
    private sealed class Renewal {
        data class Renewed(val account: AccountRepository.WorkBuddyAccount) : Renewal()
        /** RT 被判死（12153）：再打业务接口没有意义。 */
        data object Dead : Renewal()
        /** 网络 / 上游临时错误：手上的 accessToken 可能还能用，继续往下走。 */
        data object Failed : Renewal()
    }

    /**
     * 单账号签到。需要时先自动续期；401/403 再续一次并重试一轮。
     * 返回 false 的三种情况：没有可用凭据、续期被判死、签到被拒。
     */
    suspend fun checkin(account: AccountRepository.WorkBuddyAccount): Boolean {
        var current = account
        if (current.token.trim().isEmpty() && current.refreshToken.isEmpty()) {
            emit("WorkBuddy 未配置 token")
            return false
        }
        if (WorkBuddyAuth.needsRefresh(current.token, current.refreshToken, current.expiresAt)) {
            when (val renewal = renew(current)) {
                is Renewal.Renewed -> current = renewal.account
                Renewal.Dead -> return false
                // 续期失败但手上还有 AT：先按原 token 试一次，真过期了下面的 401 分支会再兜一次
                Renewal.Failed -> if (current.token.trim().isEmpty()) return false
            }
        }

        val first = checkinOnce(current)
        if (!first.expired) return first.success

        emit("token 已过期（HTTP 401/403），尝试用 refreshToken 自动续期")
        val second = when (val renewal = renew(current)) {
            is Renewal.Renewed -> checkinOnce(renewal.account)
            Renewal.Dead, Renewal.Failed -> {
                emit("自动续期未能恢复会话，请重新扫码授权")
                return false
            }
        }
        if (second.expired) {
            emit("续期后仍被拒绝（401/403），请重新扫码授权")
        }
        return second.success
    }

    /** 一次完整签到尝试：状态查询（省一次请求 + 401 探测）+ 签到。 */
    private suspend fun checkinOnce(account: AccountRepository.WorkBuddyAccount): Outcome {
        val token = account.token.trim()

        val (stStatus, _, stJson) = post(PATH_STATUS, token)
        val statusOutcome = interpretStatus(stStatus, stJson)
        if (statusOutcome.expired) {
            emit("状态查询 HTTP " + stStatus + "：token 已过期")
            return statusOutcome
        }
        if (statusOutcome.already) {
            emit("今日已签到（状态接口返回），无需重复领取")
            return statusOutcome
        }
        if (stStatus !in 200..299) {
            emit("状态查询 HTTP " + stStatus + "（继续尝试签到）")
        }

        // 小随机延迟，避免整点风控
        delay(800 + (System.currentTimeMillis() % 900))

        val (status, body, json) = post(PATH_DAILY, token)
        val outcome = interpretDailyCheckin(status, json)
        when {
            outcome.expired -> emit("token 已过期（HTTP " + status + "）")
            outcome.already -> emit("今日已签到（code=10001），无需重复领取")
            outcome.success -> {
                val data = json.optJSONObject("data") ?: JSONObject()
                emit("领取成功 credit=" + data.opt("credit") + ", streak_days=" + data.opt("streak_days"))
            }
            else -> {
                val code = json.optInt("code", -1)
                val msg = json.optString("msg", json.optString("message"))
                emit("签到失败 code=" + code + " msg=" + msg + " body=" + body.take(200))
            }
        }
        return outcome
    }

    /**
     * 用 refreshToken 换一对新 token 并写回仓储。
     * 没有 refreshToken 的账号（手填 accessToken 导入）不参与自动续期，按 [Renewal.Failed] 处理。
     */
    private suspend fun renew(account: AccountRepository.WorkBuddyAccount): Renewal {
        if (account.refreshToken.isEmpty()) {
            emit("没有 refreshToken，无法自动续期；重新扫码授权可补上")
            return Renewal.Failed
        }
        return when (val result = auth.refresh(account.token, account.refreshToken, account.uid, account.id)) {
            is WorkBuddyAuth.Result.Success -> {
                val tokens = result.tokens
                val next = account.copy(
                    token = tokens.accessToken,
                    // 上游刷新时会一起轮换 RT；响应没给就沿用旧的
                    refreshToken = tokens.refreshToken.ifEmpty { account.refreshToken },
                    expiresAt = if (tokens.expiresAt > 0) tokens.expiresAt else account.expiresAt,
                    uid = account.uid.ifEmpty { WorkBuddyLabel.decodeProfile(tokens.accessToken).uid },
                )
                runCatching { onTokensRefreshed(next) }
                    .onFailure { AppLog.w("WB", "续期后的 token 写回失败：" + it.message) }
                emit("accessToken 已自动续期" + expiryText(next.expiresAt))
                Renewal.Renewed(next)
            }
            WorkBuddyAuth.Result.SessionDead -> {
                emit("refreshToken 已失效（12153），需要重新扫码授权")
                Renewal.Dead
            }
            is WorkBuddyAuth.Result.Failed -> {
                emit("自动续期失败：" + result.reason)
                Renewal.Failed
            }
        }
    }

    private fun expiryText(expiresAt: Long): String =
        if (expiresAt <= 0) {
            ""
        } else {
            "（有效期至 " + SimpleDateFormat("MM-dd HH:mm", Locale.US).format(Date(expiresAt * 1000)) + "）"
        }
}
