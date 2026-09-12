package com.functy.fewards.core.mihoyo

import com.functy.fewards.data.repository.AccountRepository

/**
 * 凭据可用后立刻补全米游社昵称 / 头像（不再等到点「开始执行」）。
 */
object MihoyoProfileHydrator {

    private val attempted = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun needsHydration(account: AccountRepository.MihoyoAccount): Boolean =
        account.avatarUrl.isEmpty() || account.nickname.startsWith("账号")

    fun markAttempted(id: String): Boolean = attempted.add(id)

    suspend fun hydrate(
        api: MihoyoApi,
        account: AccountRepository.MihoyoAccount,
    ): AccountRepository.MihoyoAccount {
        var fixed = account
        if (!account.cookie.contains("cookie_token")) {
            val deviceId = DsSign.deviceId(account.stoken + account.stuid)
            val deviceFp = DsSign.deviceFp(deviceId)
            val full = api.fetchWebCookie(account.stoken, account.stuid, account.mid, deviceId, deviceFp)
            if (full != null) fixed = fixed.copy(cookie = full)
        }
        val deviceId = DsSign.deviceId(fixed.stoken + fixed.stuid)
        val deviceFp = DsSign.deviceFp(deviceId)
        val info = api.fetchUserInfo(fixed.stoken, fixed.stuid, fixed.mid, deviceId, deviceFp) ?: return fixed
        if (info.nickname.isNotEmpty() && (fixed.nickname.startsWith("账号") || fixed.nickname.isEmpty())) {
            fixed = fixed.copy(nickname = info.nickname)
        }
        if (info.avatarUrl.isNotEmpty() && fixed.avatarUrl.isEmpty()) {
            fixed = fixed.copy(avatarUrl = info.avatarUrl)
        }
        return fixed
    }
}
