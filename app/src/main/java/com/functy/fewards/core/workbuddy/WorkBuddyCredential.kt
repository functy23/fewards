package com.functy.fewards.core.workbuddy

import org.json.JSONObject

/**
 * WorkBuddy 凭据文本的「复制 / 粘贴」编解码。
 *
 * 复制出来的形状（账号页点复制按钮拿到的就是这段）：
 *
 *     AccessToken: eyJhbGciOi...
 *     RefreshToken: eyJhbGciOi...
 *
 * 粘贴侧比复制侧宽容，以下都能认：
 *  - 上面两行的格式（含单行版，空格分隔——miuix 输入框是 singleLine，
 *    换行会被折叠成空格，所以解析必须按「空白分隔」处理）；
 *  - 标签拼写容错：AccessToken / AcessToken / access_token / access token，
 *    分隔符 ":" 或 "="，值带不带引号都行；
 *  - 配置导出里的 JSON：{"accessToken":...,"refreshToken":...}（也认 snake_case）；
 *  - 裸 JWT；两个裸 token 用空白隔开时按「先 access 后 refresh」解释。
 *
 * 日志严禁打印 token 本身。
 */
object WorkBuddyCredential {

    /** 复制时的标签，保持这两个拼写稳定（外部工具按行首标签取值）。 */
    const val LABEL_ACCESS = "AccessToken"
    const val LABEL_REFRESH = "RefreshToken"

    /** 一对凭据。[refreshToken] 为空表示这个账号没有续期能力（只能用到 accessToken 过期）。 */
    data class Credential(val accessToken: String, val refreshToken: String) {
        val hasRefreshToken: Boolean get() = refreshToken.isNotEmpty()
    }

    /**
     * AccessToken: xxx / AcessToken: xxx（拼写容错）/ access_token=xxx。
     * 值停在空白、引号、逗号、分号处——粘贴时这些字符只会来自格式本身。
     */
    private val ACCESS_LABEL =
        Regex("""(?i)\b(?:access|acess)[\s_-]?token\s*[:=]\s*["']?([^\s"',;]+)""")

    private val REFRESH_LABEL =
        Regex("""(?i)\brefresh[\s_-]?token\s*[:=]\s*["']?([^\s"',;]+)""")

    /** 裸 token 的分隔符。 */
    private val SEPARATORS = Regex("""[\s,;]+""")

    /** 只有括号 / 引号 / null 的片段不是 token（粘贴了一个空 JSON 对象之类）。 */
    private val PUNCTUATION = Regex("""^[\s{}\[\]"',;:]+$|^null$""")

    /** 复制用文本：有哪个写哪个，两行之间用换行。 */
    fun format(accessToken: String, refreshToken: String): String {
        val at = accessToken.trim()
        val rt = refreshToken.trim()
        val lines = ArrayList<String>(2)
        if (at.isNotEmpty()) lines.add(LABEL_ACCESS + ": " + at)
        if (rt.isNotEmpty()) lines.add(LABEL_REFRESH + ": " + rt)
        return lines.joinToString("\n")
    }

    /** 解析粘贴文本。完全解析不出东西时返回 null（调用方保留输入让用户改）。 */
    fun parse(text: String): Credential? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        // 1. 配置导出 / 任何 JSON 形态
        if (trimmed.startsWith("{")) {
            val json = runCatching { JSONObject(trimmed) }.getOrNull()
            if (json != null) {
                val at = firstNonEmpty(
                    json.optString("accessToken"),
                    json.optString("access_token"),
                    json.optString("token"),
                )
                val rt = firstNonEmpty(
                    json.optString("refreshToken"),
                    json.optString("refresh_token"),
                )
                if (at.isNotEmpty() || rt.isNotEmpty()) return Credential(at, rt)
            }
        }

        // 2. 带标签的文本（复制格式 / 单行折叠版 / snake_case / 拼写容错）
        val labeledAccess = ACCESS_LABEL.find(trimmed)?.groupValues?.get(1)?.trim().orEmpty()
        val labeledRefresh = REFRESH_LABEL.find(trimmed)?.groupValues?.get(1)?.trim().orEmpty()
        if (labeledAccess.isNotEmpty() || labeledRefresh.isNotEmpty()) {
            return Credential(labeledAccess, labeledRefresh)
        }

        // 3. 裸 token：一个 = accessToken；两个 = accessToken + refreshToken
        val chunks = trimmed.split(SEPARATORS)
            .map { it.trim('"', '\'') }
            .filter { it.isNotEmpty() && !it.equals("bearer", ignoreCase = true) }
            .filterNot { PUNCTUATION.matches(it) }
        return when {
            chunks.isEmpty() -> null
            chunks.size == 1 -> Credential(chunks[0], "")
            else -> Credential(chunks[0], chunks[1])
        }
    }

    private fun firstNonEmpty(vararg values: String): String =
        values.firstOrNull { it.trim().isNotEmpty() }?.trim().orEmpty()
}
