package com.functy.fewards.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.ui.viewmodel.TaskRunner

/**
 * 开机自启 / 应用更新后重新注册定时任务。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val repo = SettingsRepositoryImpl()
        if (repo.scheduleEnabled) {
            TaskScheduler.schedule(context, repo.scheduleHour, repo.scheduleMinute)
            AppLog.i("SYS", "已重新注册每日定时任务 ${repo.scheduleHour}:${repo.scheduleMinute}")
        }
    }
}
