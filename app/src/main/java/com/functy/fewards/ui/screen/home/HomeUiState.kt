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
    /** 设置里的 WorkBuddy 总开关；关掉后首页不显示它的状态与勾选框。 */
    val wbEnabled: Boolean = true,
    /** 设置里的米游社总开关；关掉后首页不显示它的状态与勾选框。 */
    val mhyEnabled: Boolean = true,
)

@Immutable
data class HomeActions(
    val onRun: (runWb: Boolean, runMhy: Boolean) -> Unit,
    val onToggleWb: (Boolean) -> Unit,
    val onToggleMhy: (Boolean) -> Unit,
)

/**
 * 首页各卡片的可见性与完成判定。
 *
 * 总开关关掉的任务不参与统计：既不在列表 / 复选框里出现，也不算进「全部完成」，
 * 否则关掉的任务会永远把顶部大卡按在「未完成」。
 */
internal data class HomeSummary(
    /** 该任务的完成状态行是否显示。 */
    val wbVisible: Boolean,
    val mhyVisible: Boolean,
    /** 是否有任一任务可见；两个都关时为 false，整块「选择并执行」也一并隐藏。 */
    val anyVisible: Boolean,
    /** 所有**可见**任务都已完成。 */
    val allDone: Boolean,
    /** 至少一个可见任务已配置账号（大卡显示绿/红的前提）。 */
    val hasAnyConfigured: Boolean,
    /** 本次「开始执行」实际要跑的任务。 */
    val runWb: Boolean,
    val runMhy: Boolean,
) {
    val canRun: Boolean get() = runWb || runMhy
}

internal fun HomeUiState.summary(): HomeSummary {
    val wbVisible = wbEnabled
    val mhyVisible = mhyEnabled
    val anyVisible = wbVisible || mhyVisible
    return HomeSummary(
        wbVisible = wbVisible,
        mhyVisible = mhyVisible,
        anyVisible = anyVisible,
        allDone = anyVisible &&
            (!wbVisible || wbStatus == TaskRunner.TaskStatus.DONE) &&
            (!mhyVisible || mhyStatus == TaskRunner.TaskStatus.DONE),
        hasAnyConfigured = (wbVisible && wbStatus != TaskRunner.TaskStatus.UNCONFIGURED) ||
            (mhyVisible && mhyStatus != TaskRunner.TaskStatus.UNCONFIGURED),
        runWb = wbVisible && wbChecked,
        runMhy = mhyVisible && mhyChecked,
    )
}
