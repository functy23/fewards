package com.functy.fewards.data.repository

import android.content.Context
import androidx.core.content.edit
import com.functy.fewards.fewardsApp
import org.json.JSONArray
import org.json.JSONObject

/**
 * 账号与登录态仓储。所有 token / cookie 仅存本机 SharedPreferences（sec 前缀），绝不外传。
 * 键：sec.mhy.accounts（JSON 数组）、sec.wb.accounts（JSON 数组）。
 */
class AccountRepository {

    private val prefs by lazy {
        fewardsApp.getSharedPreferences("secure_store", Context.MODE_PRIVATE)
    }

    // ==================== 米游社 ====================

    data class MihoyoAccount(
        val id: String,
        val nickname: String,
        val stoken: String,
        val stuid: String,
        val mid: String,
        val cookie: String, // web cookie（含 cookie_token 等）
    )

    fun mihoyoAccounts(): List<MihoyoAccount> {
        val raw = prefs.getString("sec.mhy.accounts", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MihoyoAccount(
                    id = o.optString("id"),
                    nickname = o.optString("nickname"),
                    stoken = o.optString("stoken"),
                    stuid = o.optString("stuid"),
                    mid = o.optString("mid"),
                    cookie = o.optString("cookie"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun addMihoyoAccount(account: MihoyoAccount) {
        val list = mihoyoAccounts().toMutableList()
        list.removeAll { it.id == account.id }
        list.add(account)
        saveMihoyo(list)
    }

    fun removeMihoyoAccount(id: String) {
        saveMihoyo(mihoyoAccounts().filter { it.id != id })
    }

    private fun saveMihoyo(list: List<MihoyoAccount>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("nickname", a.nickname)
                    .put("stoken", a.stoken)
                    .put("stuid", a.stuid)
                    .put("mid", a.mid)
                    .put("cookie", a.cookie)
            )
        }
        prefs.edit { putString("sec.mhy.accounts", arr.toString()) }
    }

    fun mihoyoConfigured(): Boolean = mihoyoAccounts().isNotEmpty()

    // ==================== WorkBuddy ====================

    data class WorkBuddyAccount(
        val id: String,
        val label: String,
        val token: String,
    )

    fun workBuddyAccounts(): List<WorkBuddyAccount> {
        val raw = prefs.getString("sec.wb.accounts", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                WorkBuddyAccount(
                    id = o.optString("id"),
                    label = o.optString("label"),
                    token = o.optString("token"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun addWorkBuddyAccount(account: WorkBuddyAccount) {
        val list = workBuddyAccounts().toMutableList()
        list.removeAll { it.id == account.id }
        list.add(account)
        saveWorkBuddy(list)
    }

    fun removeWorkBuddyAccount(id: String) {
        saveWorkBuddy(workBuddyAccounts().filter { it.id != id })
    }

    private fun saveWorkBuddy(list: List<WorkBuddyAccount>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("label", a.label)
                    .put("token", a.token)
            )
        }
        prefs.edit { putString("sec.wb.accounts", arr.toString()) }
    }

    fun workBuddyConfigured(): Boolean = workBuddyAccounts().isNotEmpty()

    // ==================== 本地完成标记 ====================

    private fun doneKey(task: String) = "done.$task"

    fun todayStamp(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())

    fun isDoneToday(task: String): Boolean = prefs.getString(doneKey(task), null) == todayStamp()

    fun markDoneToday(task: String) {
        prefs.edit { putString(doneKey(task), todayStamp()) }
    }

    fun clearDone(task: String) {
        prefs.edit { remove(doneKey(task)) }
    }
}
