package com.functy.fewards.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.functy.fewards.R
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.fewardsApp
import com.functy.fewards.ui.MainActivity

/**
 * 任务进度通知。
 * - Android 13+：POST_NOTIFICATIONS 运行时权限（设置页「请求通知权限」入口）。
 * - Android 16+（Live Updates / 实时活动）：Notification.ProgressStyle 模板，
 *   参照 InstallerX-Revived 的实现方式（ProgressStyle + Segment/Point + ongoing），
 *   并在 manifest 声明 POST_PROMOTED_NOTIFICATIONS 以获得 promoted 置顶资格。
 * - 低版本：经典 determinate progress 通知。
 */
object TaskNotifier {

    const val CHANNEL_ID = "task_progress"
    private const val NOTIFICATION_ID = 1001

    // 两个任务各占一半进度
    private const val MAX_PROGRESS = 1000
    private const val STEP = MAX_PROGRESS / 2

    /** 通知权限是否已授予（Android 13 以下恒为 true）。 */
    fun permissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            fewardsApp, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel() {
        val context = fewardsApp
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.settings_notification),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.settings_notification_summary)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 更新进度通知。
     * @param doneCount 已完成任务数（0..2）
     * @param currentLabel 当前正在执行的任务名（用于 contentText）
     * @param finished true 表示全部结束（通知转为结果态，不再 ongoing）
     */
    fun notifyProgress(doneCount: Int, currentLabel: String, finished: Boolean, ok: Boolean) {
        if (!SettingsRepositoryImpl().taskNotification) return
        if (!permissionGranted()) return
        val context = fewardsApp
        ensureChannel()

        val done = (doneCount * STEP).coerceIn(0, MAX_PROGRESS)
        val contentTitle = if (finished) {
            context.getString(if (ok) R.string.notify_all_done else R.string.notify_done_with_error)
        } else {
            context.getString(R.string.notify_running)
        }
        val contentText = if (finished) currentLabel else currentLabel

        val notification: Notification = if (Build.VERSION.SDK_INT >= 36) {
            // Live Updates（实时活动）：ProgressStyle 双段进度（WorkBuddy | 米游社），
            // InstallerX-Revived 同款模板用法
            val style = Notification.ProgressStyle()
                .setProgress(MAX_PROGRESS)
                .setStyledByProgress(true)
                .setProgressSegments(
                    listOf(
                        Notification.ProgressStyle.Segment(STEP).setColor(0xFF36D167.toInt()),
                        Notification.ProgressStyle.Segment(STEP).setColor(0xFF2196F3.toInt()),
                    )
                )
                .setProgressPoints(
                    buildList {
                        if (doneCount >= 1) add(Notification.ProgressStyle.Point(STEP).setColor(0xFF36D167.toInt()))
                        if (doneCount >= 2) add(Notification.ProgressStyle.Point(MAX_PROGRESS).setColor(0xFF36D167.toInt()))
                    }
                )
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(!finished)
                .setOnlyAlertOnce(true)
                .setAutoCancel(finished)
                .setContentIntent(launchIntent(context))
                .setStyle(style)
                .build()
        } else {
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(!finished)
                .setOnlyAlertOnce(true)
                .setAutoCancel(finished)
                .setContentIntent(launchIntent(context))
                .setProgress(MAX_PROGRESS, done, !finished && done == 0)
                .build()
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun launchIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
