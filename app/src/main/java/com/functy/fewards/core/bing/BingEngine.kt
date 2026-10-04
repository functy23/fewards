package com.functy.fewards.core.bing

import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Bing / Microsoft Rewards 领取引擎（方案 B：只领，不搜索）。
 *
 * 每个账号做三件事：
 *  1. 保证 access_token 可用（没有 / 快过期就用 refresh_token 换一个）；
 *  2. 拉一次 `/dapi/me`，把可领的活动（每日卡片、打卡、阅读赚分）逐条领掉；
 *  3. 可选地打一次 Bing App 每日连签（type 103）。
 *
 * **不做自动搜索**。搜索分只能靠真实搜索拿，且微软官方明文禁止用程序/脚本搜索，
 * 违规可能导致积分作废甚至封号，因此本引擎不提供该能力。
 *
 * 幂等：重复领取返回 isDuplicate，视为成功。
 */
class BingEngine(
    private val client: OkHttpClient,
    private val accounts: List<AccountRepository.BingAccount>,
    private val appCheckIn: Boolean = true,
    private val persist: (AccountRepository.BingAccount) -> Unit = {},
) {

    /** 单账号执行结果。 */
    data class AccountResult(
        val label: String,
        val claimed: Int,
        val alreadyDone: Int,
        val failed: Int,
        val balance: Int,
        val ok: Boolean,
        val note: String = "",
    )

    /** 用 refresh_token 换 token 并顺便探一次余额／地区。登录与执行共用。 */
    data class Probe(
        val accessToken: String,
        val refreshToken: String,
        val expiresAt: Long,
        val country: String,
        val balance: Int,
        val displayName: String,
    )

    companion object {

        /** 单次执行内最多领多少张卡，防止异常数据把一次运行拖长。 */
        const val MAX_CLAIMS = 40

        /**
         * 用 refresh_token 换一个可用的 access_token，并拉一次 /dapi/me 验证。
         * 失败（token 失效 / 网络 / 协议变更）返回 null。
         */
        suspend fun probe(
            client: OkHttpClient,
            oauth: BingOAuth,
            refreshToken: String,
            country: String,
        ): Probe? {
            val token = oauth.refresh(refreshToken) ?: return null
            val access = token.accessToken
            val effectiveCountry = country.ifEmpty { BingConstants.DEFAULT_COUNTRY }
            val api = BingRewardsApi(client, access, effectiveCountry)
            val me = api.fetchMe() ?: return null
            return Probe(
                accessToken = access,
                // 微软可能不换发新的 refresh_token，那就沿用旧的
                refreshToken = token.refreshToken.ifEmpty { refreshToken },
                expiresAt = System.currentTimeMillis() / 1000 + token.expiresInSeconds,
                country = me.country.ifEmpty { effectiveCountry },
                balance = me.balance,
                displayName = me.displayName,
            )
        }

        /** access_token 是否还需要刷新。 */
        fun needsRefresh(account: AccountRepository.BingAccount): Boolean {
            if (account.accessToken.isEmpty()) return true
            val now = System.currentTimeMillis() / 1000
            return account.expiresAt - now <= BingConstants.TOKEN_REFRESH_SKEW_SECONDS
        }
    }

    private fun emit(message: String) = AppLog.i("BING", message)

    /** 执行全部账号。返回是否整体成功。 */
    suspend fun runAll(onAccountDone: (Int, Int) -> Unit = { _, _ -> }): Boolean =
        withContext(Dispatchers.IO) {
            var allOk = true
            for ((index, account) in accounts.withIndex()) {
                val result = runAccount(account)
                if (!result.ok) allOk = false
                emit(
                    "── " + result.label + "：领到 " + result.claimed + " 张" +
                        "，已领过 " + result.alreadyDone + " 张" +
                        (if (result.failed > 0) "，失败 " + result.failed + " 张" else "") +
                        (if (result.balance >= 0) "，余额 " + result.balance else "") +
                        (if (result.note.isNotEmpty()) "（" + result.note + "）" else "")
                )
                onAccountDone(index + 1, accounts.size)
            }
            allOk
        }

    /** 执行单个账号。 */
    suspend fun runAccount(account: AccountRepository.BingAccount): AccountResult {
        val oauth = BingOAuth(client)
        var current = account

        // 1. 保证 access_token 可用
        if (needsRefresh(current)) {
            val refreshed = refreshAccount(oauth, current)
            if (refreshed == null) {
                return AccountResult(
                    label = current.label, claimed = 0, alreadyDone = 0, failed = 0,
                    balance = current.lastBalance, ok = false, note = "授权失效，请重新登录",
                )
            }
            current = refreshed
            persist(current)
        }

        val country = current.country.ifEmpty { BingConstants.DEFAULT_COUNTRY }
        var api = BingRewardsApi(client, current.accessToken, country)

        // 2. 拉任务清单
        var me = api.fetchMe()
        if (me == null) {
            // 可能是 access_token 被提前吊销：再刷新一次试试
            val retried = refreshAccount(oauth, current)
            if (retried != null) {
                current = retried
                persist(current)
                api = BingRewardsApi(client, current.accessToken, current.country.ifEmpty { BingConstants.DEFAULT_COUNTRY })
                me = api.fetchMe()
            }
        }
        if (me == null) {
            return AccountResult(
                label = current.label, claimed = 0, alreadyDone = 0, failed = 0,
                balance = current.lastBalance, ok = false, note = "拉取任务清单失败",
            )
        }

        var claimed = 0
        var already = 0
        var failed = 0

        // 3. 逐条领奖
        val pending = me.promotions.filter { BingPromotions.isClaimable(it) }.take(MAX_CLAIMS)
        if (pending.isEmpty()) {
            emit("没有待领取的活动卡片")
        }
        for (promotion in pending) {
            when (val outcome = claimWithRetry(api, oauth, promotion.offerId, current) { next ->
                current = next
                persist(next)
                api = BingRewardsApi(client, next.accessToken, next.country.ifEmpty { BingConstants.DEFAULT_COUNTRY })
            }) {
                is BingClaim.Credited -> {
                    claimed++
                    emit("✔ " + promotion.displayName + "（+" + outcome.points + "）")
                }
                BingClaim.Duplicate -> already++
                is BingClaim.Rejected -> {
                    failed++
                    emit("✘ " + promotion.displayName + "：" + outcome.reason)
                }
                BingClaim.TokenExpired -> {
                    failed++
                    emit("✘ " + promotion.displayName + "：授权失效")
                }
            }
            api.settleDelay()
        }

        // 4. App 每日连签（可选）
        var note = ""
        if (appCheckIn) {
            when (val checkIn = api.appCheckIn()) {
                is BingClaim.Credited -> {
                    claimed++
                    emit("✔ Bing App 连签（+" + checkIn.points + "）")
                }
                BingClaim.Duplicate -> {
                    already++
                    note = "App 连签今日已完成"
                }
                is BingClaim.Rejected -> note = "App 连签：" + checkIn.reason
                BingClaim.TokenExpired -> note = "App 连签：授权失效"
            }
        }

        // 5. 落库（余额 / 地区 / 新 token）
        val updated = current.copy(
            country = me.country.ifEmpty { current.country },
            lastBalance = me.balance,
        )
        persist(updated)

        return AccountResult(
            label = updated.label,
            claimed = claimed,
            alreadyDone = already,
            failed = failed,
            balance = me.balance,
            ok = failed == 0,
            note = note,
        )
    }

    /** 用 refresh_token 换新 token；成功返回更新后的账号。 */
    private suspend fun refreshAccount(
        oauth: BingOAuth,
        account: AccountRepository.BingAccount,
    ): AccountRepository.BingAccount? {
        if (account.refreshToken.isEmpty()) return null
        val token = oauth.refresh(account.refreshToken) ?: return null
        return account.copy(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken.ifEmpty { account.refreshToken },
            expiresAt = System.currentTimeMillis() / 1000 + token.expiresInSeconds,
        )
    }

    /**
     * 领一次；遇到 token 失效就刷新一次再重试。
     * [onTokenRefreshed] 用于把新 token 立刻落库并换掉 api 实例。
     */
    private suspend fun claimWithRetry(
        api: BingRewardsApi,
        oauth: BingOAuth,
        offerId: String,
        account: AccountRepository.BingAccount,
        onTokenRefreshed: (AccountRepository.BingAccount) -> Unit,
    ): BingClaim {
        val first = api.claim(offerId)
        if (first !is BingClaim.TokenExpired) return first
        val refreshed = refreshAccount(oauth, account) ?: return first
        onTokenRefreshed(refreshed)
        val retryApi = BingRewardsApi(
            client, refreshed.accessToken,
            refreshed.country.ifEmpty { BingConstants.DEFAULT_COUNTRY },
        )
        return retryApi.claim(offerId)
    }
}
