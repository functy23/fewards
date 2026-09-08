package com.functy.fewards.ui.navigation

import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys（miuix-nav 体系，与 InstallerX-Revived 相同）。
 */
sealed interface Route : NavKey {
    data object Main : Route

    data object Home : Route

    data object Account : Route

    data object Settings : Route

    data object About : Route

    data object ColorPalette : Route
}
