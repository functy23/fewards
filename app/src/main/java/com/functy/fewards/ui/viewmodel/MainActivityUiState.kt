package com.functy.fewards.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.functy.fewards.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val predictiveBackAnimation: Int,
)
