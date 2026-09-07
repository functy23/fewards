package com.functy.fewards.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.functy.fewards.core.AppLog
import com.functy.fewards.ui.viewmodel.TaskRunner
import java.util.Calendar

/**
 * 精确闹钟调度：每天 HH:mm 触发 TaskReceiver 执行已勾选任务。
 * 使用 AlarmManager（setExactAndAllowWhileIdle + 开机自启重注册）。
 */
object TaskScheduler {

    private const val REQUEST_CODE = 1001
    const val ACTION_RUN_TASKS = "com.functy.fewards.action.RUN_TASKS"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // 无精确闹钟权限时退化为非精确
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
        AppLog.i("SYS", "定时任务已设置：每日 %02d:%02d".format(hour, minute))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
        AppLog.i("SYS", "定时任务已取消")
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TaskReceiver::class.java).setAction(ACTION_RUN_TASKS)
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class TaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TaskScheduler.ACTION_RUN_TASKS) return
        val result = goAsync()
        TaskRunner.runScheduledTasks(
            context = context,
            onFinished = { result.finish() }
        )
    }
}
