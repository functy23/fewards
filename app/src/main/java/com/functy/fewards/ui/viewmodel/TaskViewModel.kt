package com.functy.fewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.fewardsApp
import com.functy.fewards.ui.screen.home.HomeActions
import com.functy.fewards.ui.screen.home.HomeUiState
import com.functy.fewards.work.TaskNotifier
import com.functy.fewards.work.TaskWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 主页任务卡 ViewModel：勾选状态 + 状态卡刷新 + 触发执行。
 */
class TaskViewModel : ViewModel() {

    private val repo by lazy { SettingsRepositoryImpl() }
    private val accounts by lazy { AccountRepository() }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // TaskRunner.state 初始值是 UNCONFIGURED；必须先从 prefs 水合，否则 collect 会把
        // refresh() 读到的「已完成」立刻盖回「未完成/未配置」。
        TaskRunner.refreshStatus()
        refresh()
        // 执行状态跟随 TaskRunner（Worker 在另一协程里跑，完成后这里同步状态卡）
        viewModelScope.launch {
            TaskRunner.state.collect { runner ->
                _uiState.update {
                    it.copy(
                        wbStatus = runner.wbStatus,
                        mhyStatus = runner.mhyStatus,
                        wbRunning = runner.wbRunning,
                        mhyRunning = runner.mhyRunning,
                        running = runner.running,
                        lastRunSummary = runner.lastRunSummary,
                    )
                }
            }
        }
    }

    fun refresh() {
        val runner = TaskRunner.state.value
        _uiState.update {
            it.copy(
                wbStatus = if (runner.wbRunning) {
                    runner.wbStatus
                } else {
                    resolveStatus(accounts.workBuddyConfigured(), accounts.isDoneToday("wb"))
                },
                mhyStatus = if (runner.mhyRunning) {
                    runner.mhyStatus
                } else {
                    resolveStatus(accounts.mihoyoConfigured(), accounts.isDoneToday("mhy"))
                },
                wbRunning = runner.wbRunning,
                mhyRunning = runner.mhyRunning,
                running = runner.running,
                lastRunSummary = runner.lastRunSummary,
                wbChecked = it.wbChecked,
                mhyChecked = it.mhyChecked,
            )
        }
    }

    private fun resolveStatus(configured: Boolean, done: Boolean): TaskRunner.TaskStatus = when {
        !configured -> TaskRunner.TaskStatus.UNCONFIGURED
        done -> TaskRunner.TaskStatus.DONE
        else -> TaskRunner.TaskStatus.NOT_DONE
    }

    fun toggleWb(checked: Boolean) {
        _uiState.update { it.copy(wbChecked = checked) }
    }

    fun toggleMhy(checked: Boolean) {
        _uiState.update { it.copy(mhyChecked = checked) }
    }

    val homeActions = HomeActions(
        onRun = { runWb, runMhy ->
            TaskNotifier.startRun("正在执行…")
            AppLog.i("SYS", "开始执行：WorkBuddy=$runWb 米游社=$runMhy")
            TaskWorker.enqueue(fewardsApp, runWb, runMhy)
        },
        onToggleWb = ::toggleWb,
        onToggleMhy = ::toggleMhy,
    )
}
