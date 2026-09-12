package com.functy.fewards.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 首帧只组当前页；下一帧再预热相邻 pager 页，避免冷启动把首页+账号+设置一次编完。
 */
@Composable
fun rememberContentReady(): Boolean {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { ready = true }
    return ready
}
