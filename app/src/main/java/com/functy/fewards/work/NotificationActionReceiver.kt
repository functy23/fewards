package com.functy.fewards.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIRM) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(TaskNotifier.NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_CONFIRM = "com.functy.fewards.NOTIFY_CONFIRM"
    }
}
