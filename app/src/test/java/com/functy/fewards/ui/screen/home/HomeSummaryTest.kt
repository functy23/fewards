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
 */
class HomeSummaryTest {

    private fun state(
        wbStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
        mhyStatus: TaskRunner.TaskStatus = TaskRunner.TaskStatus.UNCONFIGURED,
        wbChecked: Boolean = true,
        mhyChecked: Boolean = true,
        wbEnabled: Boolean = true,
        mhyEnabled: Boolean = true,
    ) = HomeUiState(
        wbStatus = wbStatus,
        mhyStatus = mhyStatus,
        wbChecked = wbChecked,
        mhyChecked = mhyChecked,
        wbEnabled = wbEnabled,
        mhyEnabled = mhyEnabled,
    )

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
}
