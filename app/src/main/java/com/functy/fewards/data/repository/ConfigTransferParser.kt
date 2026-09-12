package com.functy.fewards.data.repository

import com.functy.fewards.core.mihoyo.MihoyoApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * 纯解析：不碰 SharedPreferences。裸 cookie 不再要求先过 JSONObject。
 */
object ConfigTransferParser {
    const val FORMAT = "fewards-config"
    const val VERSION = 1

    data class MihoyoDraft(
        val id: String,
        val nickname: String,
        val stoken: String,
        val stuid: String,
        val mid: String,
        val cookie: String,
    )

    data class WorkBuddyDraft(
        val id: String,
        val label: String,
        val token: String,
        val uid: String = "",
        val enterpriseId: String = "",
        val refreshToken: String = "",
        val expiresAt: Long = 0L,
    )

    data class Parsed(
        val mihoyos: List<MihoyoDraft> = emptyList(),
        val workbuddies: List<WorkBuddyDraft> = emptyList(),
        val errors: List<String> = emptyList(),
        val recognized: Boolean = true,
    )

    fun parse(raw: String): Parsed {
        val text = raw.trim()
        if (text.isEmpty()) return Parsed(errors = listOf("内容为空"), recognized = false)
        if (text.startsWith("{")) {
            return runCatching { parseJsonObject(JSONObject(text), text) }
                .getOrElse { fallbackPlain(text, it.message) }
        }
        return fallbackPlain(text, null)
    }

    private fun parseJsonObject(root: JSONObject, original: String): Parsed {
        if (root.optString("_format") == FORMAT) return parseBundle(root)
        if (original.contains("stoken") && original.contains("=")) return parseBareCookie(original)
        if (original.trim().startsWith("eyJ")) return parseBareJwt(original)
        return Parsed(errors = listOf("无法识别的配置格式"), recognized = false)
    }

    private fun fallbackPlain(text: String, jsonError: String?): Parsed {
        if (text.contains("stoken") && text.contains("=")) return parseBareCookie(text)
        if (text.startsWith("eyJ")) return parseBareJwt(text)
        val reason = jsonError?.let { "解析失败: $it" } ?: "无法识别的配置格式"
        return Parsed(errors = listOf(reason), recognized = false)
    }

    private fun parseBundle(root: JSONObject): Parsed {
        val errors = mutableListOf<String>()
        val mihoyos = mutableListOf<MihoyoDraft>()
        val workbuddies = mutableListOf<WorkBuddyDraft>()
        val mhyArr = root.optJSONArray("mihoyos") ?: JSONArray()
        for (i in 0 until mhyArr.length()) {
            val o = mhyArr.getJSONObject(i)
            val stoken = o.optString("stoken")
            val stuid = o.optString("stuid")
            if (stoken.isEmpty() || stuid.isEmpty()) {
                errors.add("米游社账号 #$i 缺少 stoken/stuid，跳过")
                continue
            }
            val mid = o.optString("mid")
            mihoyos.add(
                MihoyoDraft(
                    id = o.optString("id").ifEmpty { "mhy_" + mid.ifEmpty { i.toString() } },
                    nickname = o.optString("nickname").ifEmpty { "账号$stuid" },
                    stoken = stoken,
                    stuid = stuid,
                    mid = mid,
                    cookie = o.optString("cookie"),
                )
            )
        }
        val wbArr = root.optJSONArray("workbuddies") ?: JSONArray()
        for (i in 0 until wbArr.length()) {
            val o = wbArr.getJSONObject(i)
            val token = o.optString("token")
            if (token.isEmpty()) {
                errors.add("WorkBuddy 账号 #$i token 为空，跳过")
                continue
            }
            workbuddies.add(
                WorkBuddyDraft(
                    id = o.optString("id").ifEmpty { "wb_$i" },
                    label = o.optString("label").ifEmpty { "Work Buddy 账号" },
                    token = token,
                    uid = o.optString("uid"),
                    enterpriseId = o.optString("enterpriseId"),
                    refreshToken = o.optString("refreshToken"),
                    expiresAt = o.optLong("expiresAt"),
                )
            )
        }
        return Parsed(mihoyos = mihoyos, workbuddies = workbuddies, errors = errors)
    }

    private fun parseBareCookie(text: String): Parsed {
        val account = MihoyoApi.parseCookie(text)
        if (account == null) {
            return Parsed(errors = listOf("无法识别的 Cookie 内容"), recognized = false)
        }
        return Parsed(
            mihoyos = listOf(
                MihoyoDraft(
                    id = account.id,
                    nickname = account.nickname,
                    stoken = account.stoken,
                    stuid = account.stuid,
                    mid = account.mid,
                    cookie = account.cookie,
                )
            )
        )
    }

    private fun parseBareJwt(text: String): Parsed {
        val token = text.trim()
        return Parsed(
            workbuddies = listOf(
                WorkBuddyDraft(
                    id = "wb_import",
                    label = WorkBuddyLabelOrDefault(token),
                    token = token,
                )
            )
        )
    }

    private fun WorkBuddyLabelOrDefault(token: String): String =
        com.functy.fewards.core.workbuddy.WorkBuddyLabel.decode(token) ?: "Work Buddy 账号"
}
