package com.functy.fewards.core.mihoyo

import com.functy.fewards.data.repository.AccountRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MihoyoProfileHydratorTest {

    private fun account(
        nickname: String = "账号10001",
        avatarUrl: String = "",
    ) = AccountRepository.MihoyoAccount(
        id = "mhy_1",
        nickname = nickname,
        stoken = "v2_abc",
        stuid = "10001",
        mid = "mid",
        cookie = "stoken=v2_abc",
        avatarUrl = avatarUrl,
    )

    @Test
    fun needsHydrationWhenAvatarMissing() {
        assertTrue(MihoyoProfileHydrator.needsHydration(account()))
    }

    @Test
    fun needsHydrationWhenPlaceholderNickname() {
        assertTrue(MihoyoProfileHydrator.needsHydration(account(avatarUrl = "https://x/a.png")))
    }

    @Test
    fun skipHydrationWhenComplete() {
        assertFalse(
            MihoyoProfileHydrator.needsHydration(
                account(nickname = "旅行者", avatarUrl = "https://x/a.png"),
            )
        )
    }
}
