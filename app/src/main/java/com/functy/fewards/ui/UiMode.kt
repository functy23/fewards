package com.functy.fewards.ui

import androidx.compose.runtime.staticCompositionLocalOf

enum class UiMode(val value: String) {
    Miuix("miuix"),
    Material("material");

    companion object {
        /** MD3 已按需求整体移除：无论存储值为何，固定使用 Miuix。 */
        fun fromValue(value: String): UiMode = Miuix

        val DEFAULT_VALUE = Miuix.value
    }
}

val LocalUiMode = staticCompositionLocalOf { UiMode.Miuix }
