package com.functy.fewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.ui.screen.home.HomeActions
import com.functy.fewards.ui.screen.home.HomeUiState
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
        _uiState.update {
            it.copy(
                wbStatus = resolveStatus(accounts.workBuddyConfigured(), accounts.isDoneToday("wb")),
                mhyStatus = resolveStatus(accounts.mihoyoConfigured(), accounts.isDoneToday("mhy")),
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
            viewModelScope.launch {
                AppLog.i("SYS", "开始执行：WorkBuddy=$runWb 米游社=$runMhy")
                TaskRunner.execute(runWb, runMhy)
                refresh()
            }
        },
        onToggleWb = ::toggleWb,
        onToggleMhy = ::toggleMhy,
    )
}
