package com.functy.fewards.data.repository

import com.functy.fewards.core.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置导入/导出：
 * 导出 JSON：{"_format":"fewards-config","version":1,"mihoyos":[{...}],"workbuddies":[{...}]}
 * 导入：解析上述格式（兼容缺失字段），逐条合并入账号仓储。
 */
class ConfigTransferRepository(
    private val accounts: AccountRepository = AccountRepository(),
) {

    companion object {
        private const val FORMAT = "fewards-config"
        private const val VERSION = 1
    }

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("_format", FORMAT)
            .put("version", VERSION)
        val mhyArr = JSONArray()
        accounts.mihoyoAccounts().forEach { a ->
            mhyArr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("nickname", a.nickname)
                    .put("stoken", a.stoken)
                    .put("stuid", a.stuid)
                    .put("mid", a.mid)
                    .put("cookie", a.cookie)
            )
        }
        root.put("mihoyos", mhyArr)
        val wbArr = JSONArray()
        accounts.workBuddyAccounts().forEach { a ->
            wbArr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("label", a.label)
                    .put("token", a.token)
            )
        }
        root.put("workbuddies", wbArr)
        root.toString(2)
    }

    data class ImportResult(val mihoyo: Int, val workbuddy: Int, val errors: List<String>)

    suspend fun import(json: String): ImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var mhyCount = 0
        var wbCount = 0
        runCatching {
            val root = JSONObject(json.trim())
            when {
                root.optString("_format") == FORMAT -> {
                    val mhyArr = root.optJSONArray("mihoyos") ?: JSONArray()
                    for (i in 0 until mhyArr.length()) {
                        val o = mhyArr.getJSONObject(i)
                        val stoken = o.optString("stoken")
                        val stuid = o.optString("stuid")
                        if (stoken.isEmpty() || stuid.isEmpty()) {
                            errors.add("米游社账号 #$i 缺少 stoken/stuid，跳过")
                            continue
                        }
                        accounts.addMihoyoAccount(
                            AccountRepository.MihoyoAccount(
                                id = o.optString("id").ifEmpty { "mhy_${o.optString("mid").ifEmpty { System.currentTimeMillis().toString() }}" },
                                nickname = o.optString("nickname").ifEmpty { "账号$stuid" },
                                stoken = stoken,
                                stuid = stuid,
                                mid = o.optString("mid"),
                                cookie = o.optString("cookie"),
                            )
                        )
                        mhyCount++
                    }
                    val wbArr = root.optJSONArray("workbuddies") ?: JSONArray()
                    for (i in 0 until wbArr.length()) {
                        val o = wbArr.getJSONObject(i)
                        val token = o.optString("token")
                        if (token.isEmpty()) {
                            errors.add("WorkBuddy 账号 #$i token 为空，跳过")
                            continue
                        }
                        accounts.addWorkBuddyAccount(
                            AccountRepository.WorkBuddyAccount(
                                id = o.optString("id").ifEmpty { "wb_${System.currentTimeMillis()}_$i" },
                                label = o.optString("label").ifEmpty { "WorkBuddy 账号" },
                                token = token,
                            )
                        )
                        wbCount++
                    }
                }
                // 兼容裸 cookie 文本（含 stoken）
                json.contains("stoken") && json.contains("=") -> {
                    val account = com.functy.fewards.core.mihoyo.MihoyoApi.parseCookie(json.trim())
                    if (account != null) {
                        accounts.addMihoyoAccount(account)
                        mhyCount++
                    } else {
                        errors.add("无法识别的 Cookie 内容")
                    }
                }
                // 兼容裸 WorkBuddy token（JWT）
                json.trim().startsWith("eyJ") -> {
                    accounts.addWorkBuddyAccount(
                        AccountRepository.WorkBuddyAccount(
                            id = "wb_${System.currentTimeMillis()}",
                            label = "WorkBuddy 账号",
                            token = json.trim(),
                        )
                    )
                    wbCount++
                }
                else -> errors.add("无法识别的配置格式")
            }
        }.onFailure { errors.add("解析失败: ${it.message}") }
        AppLog.i("SYS", "导入完成：米游社 $mhyCount 个，WorkBuddy $wbCount 个")
        ImportResult(mhyCount, wbCount, errors)
    }
}
