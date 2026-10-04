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

    /** 现行兜底标签前缀。 */
    const val LABEL_PREFIX = "Bing · "

    /** 历史上用过的兜底标签前缀（那时这个任务的显示名还叫 Bing Rewards）。 */
    private const val LEGACY_LABEL_PREFIX = "Bing Rewards · "

    /** 没有可用昵称时的兜底标签。 */
    fun fallbackLabel(refreshToken: String): String = LABEL_PREFIX + fingerprint(refreshToken).take(6)

    /**
     * 把历史遗留的兜底标签改写成现行写法。
     *
     * 账号标签是**纳管那一刻**写死的，之后改显示名不会自动回溯到已有账号，
     * 所以读取侧要过一道；不匹配旧前缀的原样返回。
     */
    fun normalizeLabel(label: String): String =
        if (label.startsWith(LEGACY_LABEL_PREFIX)) {
            LABEL_PREFIX + label.removePrefix(LEGACY_LABEL_PREFIX)
        } else {
            label
        }
}
