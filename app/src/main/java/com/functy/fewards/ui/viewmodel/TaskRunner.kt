package com.functy.fewards.ui.viewmodel

import com.functy.fewards.core.AppLog
import com.functy.fewards.core.mihoyo.MihoyoApi
import com.functy.fewards.core.mihoyo.MihoyoEngine
import com.functy.fewards.core.workbuddy.WorkBuddyEngine
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.work.TaskNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 任务执行逻辑：wb + mhy 并行（async）。
 * 入队入口在 work.TaskWorker.enqueue，保证进程被杀后仍可执行。
 */
object TaskRunner {

    enum class TaskStatus { UNCONFIGURED, QUERYING, DONE, NOT_DONE }

    data class TaskUiState(
        val wbStatus: TaskStatus = TaskStatus.UNCONFIGURED,
        val mhyStatus: TaskStatus = TaskStatus.UNCONFIGURED,
        val wbRunning: Boolean = false,
        val mhyRunning: Boolean = false,
        val running: Boolean = false,
        val lastRunSummary: String = "",
    )

    private val _state = MutableStateFlow(TaskUiState())
    val state: StateFlow<TaskUiState> = _state.asStateFlow()

    private val repo by lazy { SettingsRepositoryImpl() }
    private val accounts by lazy { AccountRepository() }

    /** 按本地完成标记刷新状态卡（跨天自动重置）。 */
    fun refreshStatus() {
        _state.value = _state.value.copy(
            wbStatus = resolveStatus(configured = accounts.workBuddyConfigured(), done = accounts.isDoneToday("wb")),
            mhyStatus = resolveStatus(configured = accounts.mihoyoConfigured(), done = accounts.isDoneToday("mhy")),
        )
    }

    private fun resolveStatus(configured: Boolean, done: Boolean): TaskStatus = when {
        !configured -> TaskStatus.UNCONFIGURED
        done -> TaskStatus.DONE
        else -> TaskStatus.NOT_DONE
    }

    /** 直接执行（Worker 调用；wb 与 mhy 并行）。 */
    suspend fun execute(runWb: Boolean, runMhy: Boolean): String {
        if (_state.value.running) return "已有任务在执行中"
        _state.value = _state.value.copy(
            running = true,
            wbStatus = if (runWb && _state.value.wbStatus != TaskStatus.UNCONFIGURED) TaskStatus.QUERYING else _state.value.wbStatus,
            mhyStatus = if (runMhy && _state.value.mhyStatus != TaskStatus.UNCONFIGURED) TaskStatus.QUERYING else _state.value.mhyStatus,
        )
        val totalSteps = listOf(runWb, runMhy).count { it }
        var doneSteps = 0
        var allOk = true
        if (totalSteps > 0) {
            TaskNotifier.startRun("正在执行…")
        }
        val summaries = mutableListOf<String>()
        try {
            coroutineScope {
                fun onStepDone(ok: Boolean) {
                    doneSteps++
                    if (!ok) allOk = false
                    TaskNotifier.onStep(doneSteps, totalSteps, summaries.joinToString("；"))
                }
                val wbJob: Job = if (runWb) launchJobWb(summaries, ::onStepDone) else Job()
                val mhyJob: Job = if (runMhy) launchJobMhy(summaries, ::onStepDone) else Job()
                listOf(wbJob, mhyJob).filter { it.isActive || it.isCompleted }.forEach { }
                // 等待两个并行分支结束
                if (runWb) wbJob.join()
                if (runMhy) mhyJob.join()
            }
        } finally {
            _state.value = _state.value.copy(running = false)
        }
        val summary = summaries.joinToString("；").ifEmpty { "未选择任何任务" }
        _state.value = _state.value.copy(lastRunSummary = summary)
        if (totalSteps > 0) {
            TaskNotifier.complete(allOk, summary)
        }
        return summary
    }

    private fun MutableStateFlow<TaskUiState>.updateWbRunning(v: Boolean) {
        value = value.copy(wbRunning = v)
    }

    private fun MutableStateFlow<TaskUiState>.updateMhyRunning(v: Boolean) {
        value = value.copy(mhyRunning = v)
    }

    private suspend fun launchJobWb(summaries: MutableList<String>, onStepDone: (Boolean) -> Unit): Job =
        kotlinx.coroutines.coroutineScope {
            val deferred = async {
                _state.value = _state.value.copy(wbRunning = true, wbStatus = TaskStatus.QUERYING)
                try {
                    var list = accounts.workBuddyAccounts()
                    // WB label：优先用 JWT 内实际昵称；历史占位名一并更新
                    list.forEach { acc ->
                        if (acc.label == "WorkBuddy 账号" || acc.label == "Work Buddy 账号") {
                            val real = com.functy.fewards.core.workbuddy.WorkBuddyLabel.decode(acc.token)
                            if (real != null) {
                                accounts.addWorkBuddyAccount(acc.copy(label = real))
                            }
                        }
                    }
                    if (list.isEmpty() || !repo.wbMasterEnabled) {
                        if (list.isEmpty()) AppLog.w("WB", "WorkBuddy 未配置账号，跳过")
                        _state.value = _state.value.copy(wbRunning = false, wbStatus = TaskStatus.UNCONFIGURED)
                        return@async
                    }
                    val engine = WorkBuddyEngine(MihoyoApi.defaultClient(), list)
                    val ok = engine.runAll()
                    if (ok) accounts.markDoneToday("wb")
                    _state.value = _state.value.copy(
                        wbRunning = false,
                        wbStatus = if (ok) TaskStatus.DONE else TaskStatus.NOT_DONE
                    )
                    summaries.add("WorkBuddy: ${if (ok) "完成" else "存在失败项"}")
                    onStepDone(ok)
                } catch (t: Throwable) {
                    AppLog.e("WB", "WorkBuddy 执行异常: ${t.message}")
                    _state.value = _state.value.copy(wbRunning = false, wbStatus = TaskStatus.NOT_DONE)
                    summaries.add("WorkBuddy: 异常")
                }
            }
            deferred
        }

    private suspend fun launchJobMhy(summaries: MutableList<String>, onStepDone: (Boolean) -> Unit): Job =
        kotlinx.coroutines.coroutineScope {
            val deferred = async {
                _state.value = _state.value.copy(mhyRunning = true, mhyStatus = TaskStatus.QUERYING)
                try {
                    val list = accounts.mihoyoAccounts()
                    if (list.isEmpty() || !repo.mhyMasterEnabled) {
                        if (list.isEmpty()) AppLog.w("MHY", "米游社未配置账号，跳过")
                        _state.value = _state.value.copy(mhyRunning = false, mhyStatus = TaskStatus.UNCONFIGURED)
                        return@async
                    }
                    // self-heal：旧版导入的账号缺 cookie_token/ltoken，先补全再执行
                    val api = MihoyoApi(MihoyoApi.defaultClient())
                    val healed = list.map { acc ->
                        var fixed = acc
                        if (!acc.cookie.contains("cookie_token")) {
                            val deviceId = com.functy.fewards.core.mihoyo.DsSign.deviceId(acc.stoken + acc.stuid)
                            val deviceFp = com.functy.fewards.core.mihoyo.DsSign.deviceFp(deviceId)
                            val full = api.fetchWebCookie(acc.stoken, acc.stuid, acc.mid, deviceId, deviceFp)
                            if (full != null) fixed = fixed.copy(cookie = full)
                        }
                        if (acc.nickname.startsWith("账号") || acc.avatarUrl.isEmpty()) {
                            val deviceId = com.functy.fewards.core.mihoyo.DsSign.deviceId(acc.stoken + acc.stuid)
                            val deviceFp = com.functy.fewards.core.mihoyo.DsSign.deviceFp(deviceId)
                            val info = api.fetchUserInfo(acc.stoken, acc.stuid, acc.mid, deviceId, deviceFp)
                            if (info != null) {
                                if (info.nickname.isNotEmpty() && acc.nickname.startsWith("账号")) {
                                    fixed = fixed.copy(nickname = info.nickname)
                                }
                                if (info.avatarUrl.isNotEmpty() && fixed.avatarUrl.isEmpty()) {
                                    fixed = fixed.copy(avatarUrl = info.avatarUrl)
                                }
                            }
                        }
                        if (fixed != acc) accounts.addMihoyoAccount(fixed)
                        fixed
                    }
                    val engine = MihoyoEngine(api, repo, healed)
                    val ok = engine.runAll()
                    if (ok) accounts.markDoneToday("mhy")
                    _state.value = _state.value.copy(
                        mhyRunning = false,
                        mhyStatus = if (ok) TaskStatus.DONE else TaskStatus.NOT_DONE
                    )
                    summaries.add("米游社: ${if (ok) "完成" else "存在失败项"}")
                    onStepDone(ok)
                } catch (t: Throwable) {
                    AppLog.e("MHY", "米游社执行异常: ${t.message}")
                    _state.value = _state.value.copy(mhyRunning = false, mhyStatus = TaskStatus.NOT_DONE)
                    summaries.add("米游社: 异常")
                }
            }
            deferred
        }
}
