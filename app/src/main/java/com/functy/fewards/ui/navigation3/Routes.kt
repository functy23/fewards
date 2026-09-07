package com.functy.fewards.ui.navigation3

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize

/**
 * Type-safe navigation keys for Navigation3.
 */
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    data object Main : Route

    @Parcelize
    data object Home : Route

    @Parcelize
    data object Account : Route

    @Parcelize
    data object Settings : Route

    @Parcelize
    data object About : Route

    @Parcelize
    data object ColorPalette : Route
}
