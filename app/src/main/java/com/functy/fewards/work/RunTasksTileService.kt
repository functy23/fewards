package com.functy.fewards.work

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.functy.fewards.R
import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.ui.viewmodel.TaskRunner

/**
 * 控制中心 / 快捷设置磁贴：点击后开启并执行与首页「开始执行」相同的勾选任务（默认双开）。
 * 执行中保持 STATE_ACTIVE，结束后回到 STATE_INACTIVE。执行中再点不取消。
 */
class RunTasksTileService : TileService() {

    override fun onStartListening() {
        applyState(TaskRunner.state.value.running)
    }

    override fun onClick() {
        if (TaskRunner.state.value.running) {
            applyState(true)
            return
        }
        val start = {
            val accounts = AccountRepository()
            val settings = SettingsRepositoryImpl()
            val runWb = settings.wbMasterEnabled && accounts.workBuddyConfigured()
            val runMhy = settings.mhyMasterEnabled && accounts.mihoyoConfigured()
            if (!runWb && !runMhy) {
                AppLog.w("SYS", "控制中心签到：没有可执行的已配置任务")
                applyState(false)
            } else {
                applyState(true)
                TaskNotifier.startRun(getString(R.string.notify_running))
                AppLog.i("SYS", "控制中心开始执行：WorkBuddy=$runWb 米游社=$runMhy")
                TaskWorker.enqueue(applicationContext, runWb, runMhy)
            }
        }
        if (isLocked) {
            unlockAndRun(start)
        } else {
            start()
        }
    }

    private fun applyState(running: Boolean) {
        qsTile?.apply {
            state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_run_tasks)
            subtitle = getString(
                if (running) R.string.home_running else R.string.home_start_execute
            )
            updateTile()
        }
    }

    companion object {
        fun refresh(context: Context) {
            TileService.requestListeningState(
                context,
                ComponentName(context, RunTasksTileService::class.java),
            )
        }
    }
}
