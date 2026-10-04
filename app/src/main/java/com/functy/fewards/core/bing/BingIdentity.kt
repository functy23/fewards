package com.functy.fewards.core.bing

import java.security.MessageDigest

/**
 * 账号标识工具。
 *
 * refresh_token 会随每次刷新轮换，**不能**拿它本身当账号 id（会泄漏凭据，也不稳定）。
 * 这里只取它的不可逆短指纹用于：
 *  - 生成稳定的本地 id（首次纳管时算一次，之后刷新 token 不改 id）；
 *  - 多账号时的兜底显示名（用户能区分是哪一条）。
 */
object BingIdentity {

    /** refresh_token 的 12 位十六进制指纹（sha-256 前 6 字节）。 */
    fun fingerprint(refreshToken: String): String {
        if (refreshToken.isEmpty()) return "000000000000"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(refreshToken.toByteArray(Charsets.UTF_8))
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    fun accountId(refreshToken: String): String = "bing_" + fingerprint(refreshToken)

    /** 没有可用昵称时的兜底标签。 */
    fun fallbackLabel(refreshToken: String): String = "Bing · " + fingerprint(refreshToken).take(6)
}
