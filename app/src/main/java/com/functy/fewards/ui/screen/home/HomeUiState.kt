package com.functy.fewards.ui.screen.home

import androidx.compose.runtime.Immutable
import com.functy.fewards.ui.viewmodel.TaskRunner

@Immutable
data class HomeUiState(
    val wbStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
    val mhyStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
    val wbRunning: Boolean = false,
    val mhyRunning: Boolean = false,
    val running: Boolean = false,
    val wbChecked: Boolean = true,
    val mhyChecked: Boolean = true,
    val lastRunSummary: String = "",
)

@Immutable
data class HomeActions(
    val onRun: (runWb: Boolean, runMhy: Boolean) -> Unit,
    val onToggleWb: (Boolean) -> Unit,
    val onToggleMhy: (Boolean) -> Unit,
)
