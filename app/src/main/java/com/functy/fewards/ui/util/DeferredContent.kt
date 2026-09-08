package com.functy.fewards.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 轻量内容就绪开关：迁移到 InstallerX-Revived 的 miuix-nav 导航体系后，
 * 转场为 graphics 层驱动，无需延迟占位；保留接口兼容，恒为 true。
 */
@Composable
fun rememberContentReady(): Boolean {
    var ready by remember { mutableStateOf(true) }
    return ready
}
