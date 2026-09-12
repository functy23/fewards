package com.functy.fewards.data.repository

import com.functy.fewards.core.AppLog
import com.functy.fewards.core.mihoyo.MihoyoApi
import com.functy.fewards.core.mihoyo.MihoyoProfileHydrator
import com.functy.fewards.core.workbuddy.WorkBuddyLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 配置导入/导出：
 * 导出 JSON：{"_format":"fewards-config","version":1,"mihoyos":[{...}],"workbuddies":[{...}]}
 * 导入：解析上述格式（兼容裸 cookie / 裸 JWT），逐条合并入账号仓储。
 */
class ConfigTransferRepository(
    private val accounts: AccountRepository = AccountRepository(),
) {

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
            .put("_format", ConfigTransferParser.FORMAT)
            .put("version", ConfigTransferParser.VERSION)
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
                    .put("uid", a.uid)
                    .put("enterpriseId", a.enterpriseId)
                    .put("refreshToken", a.refreshToken)
                    .put("expiresAt", a.expiresAt)
            )
        }
        root.put("workbuddies", wbArr)
        root.toString(2)
    }

    data class ImportResult(val mihoyo: Int, val workbuddy: Int, val errors: List<String>)

    suspend fun import(json: String): ImportResult = withContext(Dispatchers.IO) {
        val parsed = ConfigTransferParser.parse(json)
        parsed.mihoyos.forEach { d ->
            accounts.addMihoyoAccount(
                AccountRepository.MihoyoAccount(
                    id = d.id,
                    nickname = d.nickname,
                    stoken = d.stoken,
                    stuid = d.stuid,
                    mid = d.mid,
                    cookie = d.cookie,
                )
            )
        }
        parsed.workbuddies.forEach { d ->
            val profile = WorkBuddyLabel.decodeProfile(d.token)
            accounts.addWorkBuddyAccount(
                AccountRepository.WorkBuddyAccount(
                    // 裸 JWT / 旧配置没有独立 id：用 JWT sub 兜底，避免同一账号重复导入成多条
                    id = d.id.ifEmpty { profile.uid.ifEmpty { "wb_" + System.currentTimeMillis() } },
                    label = profile.label ?: d.label,
                    token = d.token,
                    avatarUrl = profile.avatarUrl.orEmpty(),
                    uid = profile.uid.ifEmpty { d.uid },
                    enterpriseId = d.enterpriseId,
                    refreshToken = d.refreshToken,
                    expiresAt = d.expiresAt,
                )
            )
        }
        val api = MihoyoApi(MihoyoApi.defaultClient())
        accounts.mihoyoAccounts()
            .filter { MihoyoProfileHydrator.needsHydration(it) }
            .forEach { acc ->
                if (!MihoyoProfileHydrator.markAttempted(acc.id)) return@forEach
                val next = MihoyoProfileHydrator.hydrate(api, acc)
                if (next != acc) accounts.addMihoyoAccount(next)
            }
        AppLog.i("SYS", "导入完成：米游社 " + parsed.mihoyos.size + " 个，WorkBuddy " + parsed.workbuddies.size + " 个")
        ImportResult(parsed.mihoyos.size, parsed.workbuddies.size, parsed.errors)
    }
}
