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
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 任务进度通知。完成态保持 ongoing + promoted，避免掉出实时活动。
 * 自动消失：按「完成总览」停留时间后再 cancel；关闭自动消失则展开并挂确认按钮。
 */
object TaskNotifier {

    const val CHANNEL_ID = "task_progress"
    const val LIVE_CHANNEL_ID = "task_live"
    const val NOTIFICATION_ID = 1001

    private const val MAX_PROGRESS = 1000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var animJob: Job? = null
    private var displayed = 0f
    private var target = 0f
    private var label = ""
    private var finishing = false
    private var finishOk = false
    private var waitForConfirm = false

    fun permissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            fewardsApp, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel() {
        val context = fewardsApp
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.settings_notification),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.settings_notification_summary) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                LIVE_CHANNEL_ID,
                context.getString(R.string.notify_live_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notify_live_channel_summary)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    fun startRun(currentLabel: String) {
        if (!canNotify()) return
        ensureChannel()
        animJob?.cancel()
        displayed = 0f
        target = 140f
        label = currentLabel
        finishing = false
        finishOk = false
        waitForConfirm = false
        postLive()
        startAnimator()
    }

    fun onStep(doneCount: Int, totalSteps: Int, currentLabel: String) {
        if (!canNotify() || totalSteps <= 0) return
        label = currentLabel
        val floor = doneCount.toFloat() / totalSteps * MAX_PROGRESS
        target = if (doneCount >= totalSteps) {
            MAX_PROGRESS.toFloat()
        } else {
            (floor + 120f).coerceAtMost(MAX_PROGRESS * 0.88f)
        }
        startAnimator()
    }

    fun complete(ok: Boolean, currentLabel: String) {
        if (!canNotify()) return
        val repo = SettingsRepositoryImpl()
        label = currentLabel
        finishing = true
        finishOk = ok
        waitForConfirm = !repo.overviewAutoDismiss
        target = MAX_PROGRESS.toFloat()
        startAnimator()
    }

    fun notifyProgress(doneCount: Int, currentLabel: String, finished: Boolean, ok: Boolean) {
        when {
            finished -> complete(ok, currentLabel)
            doneCount <= 0 -> startRun(currentLabel)
            else -> onStep(doneCount, 2, currentLabel)
        }
    }

    private fun canNotify(): Boolean {
        if (!SettingsRepositoryImpl().taskNotification) return false
        if (!permissionGranted()) return false
        return true
    }

    private fun holdMillis(): Long {
        val seconds = SettingsRepositoryImpl().overviewHoldSeconds.coerceIn(0.05f, 30f)
        return (seconds * 1000f).toLong()
    }

    private fun startAnimator() {
        if (animJob?.isActive == true) return
        animJob = scope.launch {
            while (isActive) {
                val diff = target - displayed
                if (abs(diff) < 2f) {
                    displayed = target
                    postLive()
                    if (finishing && displayed >= MAX_PROGRESS - 1f) {
                        if (waitForConfirm) break
                        delay(holdMillis())
                        cancelNotification()
                        break
                    }
                    delay(48)
                    continue
                }
                displayed += diff * 0.14f + if (diff > 0) 6f else -6f
                displayed = displayed.coerceIn(0f, MAX_PROGRESS.toFloat())
                if (!finishing && displayed < target) {
                    target = (target + 2.2f).coerceAtMost(MAX_PROGRESS * 0.88f)
                }
                postLive()
                delay(48)
            }
        }
    }

    private fun postLive() {
        val context = fewardsApp
        val finishedVisual = finishing && displayed >= MAX_PROGRESS - 1f
        val contentTitle = if (finishedVisual) {
            context.getString(if (finishOk) R.string.notify_all_done else R.string.notify_done_with_error)
        } else {
            context.getString(R.string.notify_running)
        }
        val shortText = when {
            finishedVisual && finishOk -> context.getString(R.string.notify_live_short_done)
            finishedVisual -> context.getString(R.string.notify_live_short_error)
            else -> context.getString(R.string.notify_live_short_running)
        }
        val progress = displayed.toInt().coerceIn(0, MAX_PROGRESS)
        val brand = 0xFF1E110D.toInt()
        val showConfirm = finishedVisual && waitForConfirm

        val notification: Notification = if (Build.VERSION.SDK_INT >= 36) {
            val style = NotificationCompat.ProgressStyle()
                .setProgress(progress)
                .setStyledByProgress(true)
                .setProgressSegments(
                    listOf(NotificationCompat.ProgressStyle.Segment(MAX_PROGRESS).setColor(brand))
                )
            val builder = NotificationCompat.Builder(context, LIVE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_task)
                .setColor(brand)
                .setContentTitle(contentTitle)
                .setContentText(label)
                .setShortCriticalText(shortText)
                .setOngoing(true)
                .setRequestPromotedOngoing(true)
                .setSilent(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentIntent(launchIntent(context))
                .setStyle(style)
            if (showConfirm) {
                builder.addAction(
                    R.drawable.ic_stat_task,
                    context.getString(R.string.confirm),
                    confirmIntent(context),
                )
            }
            builder.build()
        } else {
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_task)
                .setColor(brand)
                .setContentTitle(contentTitle)
                .setContentText(label)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setAutoCancel(false)
                .setContentIntent(launchIntent(context))
                .setProgress(MAX_PROGRESS, progress, false)
            if (showConfirm) {
                builder.addAction(
                    R.drawable.ic_stat_task,
                    context.getString(R.string.confirm),
                    confirmIntent(context),
                )
            }
            builder.build()
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        val manager = fewardsApp.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun launchIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun confirmIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 1,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_CONFIRM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
