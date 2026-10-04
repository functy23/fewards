package com.functy.fewards.ui.screen.home

import com.functy.fewards.ui.viewmodel.TaskRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 首页「总开关关掉的任务整行消失」的判定。
 *
 * 关掉的任务既不出现在列表 / 复选框里，也不参与「全部完成」的统计——
 * 否则一个关掉的、今天没跑的任务会把顶部大卡永远按在「未完成」。
 *
 * 本文件前半段的用例考察的是这条规则本身，与任务个数无关，因此
 * [state] 默认把第三个任务（Bing）关掉；专门考察三任务的用例显式打开它。
 */
class HomeSummaryTest {

    private fun state(
        wbStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
        mhyStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
        bingStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
        wbChecked: Boolean = true,
        mhyChecked: Boolean = true,
        bingChecked: Boolean = true,
        wbEnabled: Boolean = true,
        mhyEnabled: Boolean = true,
        bingEnabled: Boolean = false,
    ) = HomeUiState(
        wbStatus = wbStatus,
        mhyStatus = mhyStatus,
        bingStatus = bingStatus,
        wbChecked = wbChecked,
        mhyChecked = mhyChecked,
        bingChecked = bingChecked,
        wbEnabled = wbEnabled,
        mhyEnabled = mhyEnabled,
        bingEnabled = bingEnabled,
    )

    // ==================== 两条任务的既有行为 ====================

    @Test
    fun bothEnabledBothVisible() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
        ).summary()
        assertTrue(s.wbVisible)
        assertTrue(s.mhyVisible)
        assertTrue(s.anyVisible)
        assertTrue(s.allDone)
        assertTrue(s.hasAnyConfigured)
    }

    @Test
    fun disabledTaskRowIsHidden() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            mhyEnabled = false,
        ).summary()
        assertTrue(s.wbVisible)
        assertFalse(s.mhyVisible)
    }

    @Test
    fun disabledTaskDoesNotBlockAllDone() {
        // 米游社关掉且今天没跑（NOT_DONE），不该拖住「全部完成」。
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.NOT_DONE,
            mhyEnabled = false,
        ).summary()
        assertTrue(s.allDone)
        assertTrue(s.hasAnyConfigured)
    }

    @Test
    fun disabledTaskIsNotCountedAsConfigured() {
        // 只有关掉的那个任务配置了账号，大卡应当当作「没有可执行任务」。
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.UNCONFIGURED,
            wbEnabled = false,
            mhyEnabled = true,
        ).summary()
        assertFalse(s.allDone)
        assertFalse(s.hasAnyConfigured)
    }

    @Test
    fun bothDisabledHidesEverythingAndCannotRun() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            wbEnabled = false,
            mhyEnabled = false,
        ).summary()
        assertFalse(s.anyVisible)
        assertFalse(s.allDone)
        assertFalse(s.hasAnyConfigured)
        assertFalse(s.canRun)
    }

    @Test
    fun disabledTaskNeverRunsEvenIfChecked() {
        val s = state(wbChecked = true, mhyChecked = true, mhyEnabled = false).summary()
        assertTrue(s.runWb)
        assertFalse(s.runMhy)
        assertTrue(s.canRun)
    }

    @Test
    fun uncheckedEnabledTaskDoesNotRun() {
        val s = state(wbChecked = false, mhyChecked = true).summary()
        assertFalse(s.runWb)
        assertTrue(s.runMhy)
    }

    @Test
    fun onlyDisabledLeftMeansCannotRun() {
        val s = state(wbChecked = false, mhyChecked = true, mhyEnabled = false).summary()
        assertFalse(s.canRun)
    }

    @Test
    fun visibleButUnconfiguredIsNotAllDone() {
        // 任务开着但没配账号：不该显示「全部完成」。
        val s = state(
            wbStatus = TaskRunner.TaskStatus.UNCONFIGURED,
            mhyStatus = TaskRunner.TaskStatus.DONE,
        ).summary()
        assertEquals(false, s.allDone)
        assertTrue(s.hasAnyConfigured)
    }

    // ==================== Bing（第三个任务） ====================

    @Test
    fun bingRowAppearsWhenEnabled() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            bingStatus = TaskRunner.TaskStatus.DONE,
            bingEnabled = true,
        ).summary()
        assertTrue(s.bingVisible)
        assertTrue(s.anyVisible)
        assertTrue(s.allDone)
    }

    @Test
    fun bingRowIsHiddenWhenDisabled() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            bingStatus = TaskRunner.TaskStatus.NOT_DONE,
            bingEnabled = false,
        ).summary()
        assertFalse(s.bingVisible)
        // 关掉的 bing 今天没跑，也不该拖住「全部完成」
        assertTrue(s.allDone)
    }

    @Test
    fun enabledButUndoneBingBlocksAllDone() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            bingStatus = TaskRunner.TaskStatus.NOT_DONE,
            bingEnabled = true,
        ).summary()
        assertFalse(s.allDone)
    }

    @Test
    fun unconfiguredBingDoesNotCountAsConfigured() {
        // 三个都开着但只有 bing 没配账号 = 没有可执行任务
        val s = state(
            bingStatus = TaskRunner.TaskStatus.UNCONFIGURED,
            bingEnabled = true,
        ).summary()
        assertTrue(s.bingVisible)
        assertFalse(s.hasAnyConfigured)
        assertFalse(s.allDone)
    }

    @Test
    fun bingAloneConfiguredMeansConfigured() {
        // 只留 Bing 一个任务：它配了账号且今天跑完，就是「全部完成」。
        // （另外两个任务仍开着但没配账号时不算「全部完成」，见 visibleButUnconfiguredIsNotAllDone）
        val s = state(
            bingStatus = TaskRunner.TaskStatus.DONE,
            wbEnabled = false,
            mhyEnabled = false,
            bingEnabled = true,
        ).summary()
        assertTrue(s.hasAnyConfigured)
        assertTrue(s.allDone)
        assertTrue(s.runBing)
    }

    @Test
    fun bingRunsOnlyWhenEnabledAndChecked() {
        val enabled = state(bingEnabled = true, bingChecked = true).summary()
        assertTrue(enabled.runBing)

        val unchecked = state(bingEnabled = true, bingChecked = false).summary()
        assertFalse(unchecked.runBing)

        // 关掉的 bing 即使勾着也不跑
        val disabled = state(bingEnabled = false, bingChecked = true).summary()
        assertFalse(disabled.runBing)
    }

    @Test
    fun canRunWithOnlyBingChecked() {
        val s = state(
            wbChecked = false,
            mhyChecked = false,
            bingChecked = true,
            bingEnabled = true,
        ).summary()
        assertTrue(s.canRun)
    }

    @Test
    fun allThreeDisabledHidesEverything() {
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            bingStatus = TaskRunner.TaskStatus.DONE,
            wbEnabled = false,
            mhyEnabled = false,
            bingEnabled = false,
        ).summary()
        assertFalse(s.anyVisible)
        assertFalse(s.canRun)
        assertFalse(s.allDone)
    }

    @Test
    fun bingUnconfiguredStillAllowsRunningTheOthers() {
        // bing 开着但没配账号：不显示「全部完成」，其余两个仍可勾选执行
        val s = state(
            wbStatus = TaskRunner.TaskStatus.DONE,
            mhyStatus = TaskRunner.TaskStatus.DONE,
            bingStatus = TaskRunner.TaskStatus.UNCONFIGURED,
            bingEnabled = true,
        ).summary()
        assertTrue(s.canRun)
        assertTrue(s.runWb && s.runMhy)
        assertFalse(s.allDone)
    }
}
