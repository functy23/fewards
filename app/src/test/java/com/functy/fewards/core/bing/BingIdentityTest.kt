package com.functy.fewards.core.bing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 账号指纹。
 *
 * 指纹用于生成稳定的本地 id 与兜底显示名，必须**不可逆**且不含凭据本身。
 */
class BingIdentityTest {

    @Test
    fun fingerprintIsStableAndTwelveHexChars() {
        val a = BingIdentity.fingerprint("refresh-token-abc")
        val b = BingIdentity.fingerprint("refresh-token-abc")
        assertEquals(a, b)
        assertEquals(12, a.length)
        assertTrue(a.all { it in "0123456789abcdef" })
    }

    @Test
    fun differentTokensGiveDifferentFingerprints() {
        assertNotEquals(
            BingIdentity.fingerprint("token-one"),
            BingIdentity.fingerprint("token-two"),
        )
    }

    @Test
    fun fingerprintDoesNotLeakTheToken() {
        val token = "M.C123_BAY.this-is-the-secret"
        val fp = BingIdentity.fingerprint(token)
        assertTrue(!fp.contains("secret"))
        assertTrue(!token.contains(fp))
    }

    @Test
    fun emptyTokenHasNeutralFingerprint() {
        assertEquals("000000000000", BingIdentity.fingerprint(""))
    }

    @Test
    fun accountIdIsPrefixedAndStable() {
        val id = BingIdentity.accountId("token-x")
        assertTrue(id.startsWith("bing_"))
        assertEquals(id, BingIdentity.accountId("token-x"))
    }

    @Test
    fun fallbackLabelUsesShortFingerprint() {
        val label = BingIdentity.fallbackLabel("token-x")
        assertTrue(label.startsWith("Bing · "))
        assertTrue(label.contains(BingIdentity.fingerprint("token-x").take(6)))
    }

    @Test
    fun legacyFallbackLabelIsRewritten() {
        // v3.0.0 之前这个任务的显示名叫 Bing Rewards，老账号的标签是那样写死的
        assertEquals("Bing · 9faf71", BingIdentity.normalizeLabel("Bing Rewards · 9faf71"))
    }

    @Test
    fun normalizeKeepsCurrentAndUserLabels() {
        assertEquals("Bing · 9faf71", BingIdentity.normalizeLabel("Bing · 9faf71"))
        // 服务端给的真昵称、以及用户自己没改过的其它写法，一律不动
        assertEquals("张三", BingIdentity.normalizeLabel("张三"))
        assertEquals("", BingIdentity.normalizeLabel(""))
        // 只是碰巧带 Bing 字样，不是旧兜底格式
        assertEquals("Bing Rewards 账号", BingIdentity.normalizeLabel("Bing Rewards 账号"))
    }
}
