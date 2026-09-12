package com.functy.fewards.core.workbuddy

import org.json.JSONObject
import java.util.Base64

/**
 * 从 WorkBuddy accessToken（JWT）解出账号实际名字。
 * payload 字段：nickname（带前导控制字符，需 trim）、preferred_username（手机号）。
 * 两者都取不到时返回 null（调用方沿用占位名）。
 */
object WorkBuddyLabel {

    data class Profile(val label: String?, val avatarUrl: String?)

    fun decode(token: String): String? = decodeProfile(token).label

    fun decodeProfile(token: String): Profile = runCatching {
        val payload = token.trim().split(".")[1]
        val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
        val json = JSONObject(String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8))
        val nickname = json.optString("nickname", "").trim()
        val username = json.optString("preferred_username", "").trim()
        val label = nickname.ifEmpty { username }.ifEmpty { null }
        val avatar = listOf("picture", "avatar_url", "avatar", "headimgurl")
            .map { json.optString(it, "").trim() }
            .firstOrNull { it.startsWith("http") }
        Profile(label, avatar)
    }.getOrElse { Profile(null, null) }
}
