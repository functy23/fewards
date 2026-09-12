package com.functy.fewards.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys（miuix-nav 体系，与 InstallerX-Revived 相同）。
 * rememberNavBackStack 通过 kotlinx.serialization 持久化返回栈，故全部需要 @Serializable。
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object About : Route

    @Serializable
    data object ColorPalette : Route
}
