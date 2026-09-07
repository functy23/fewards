package com.functy.fewards.ui.screen.home

import android.content.Context
import androidx.compose.runtime.Immutable

@Immutable
data class ManagerVersion(
    val versionName: String,
    val versionCode: Long
)

fun buildVersionLabel(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        "v${packageInfo.versionName}"
    } catch (_: Exception) {
        ""
    }
}
