package com.functy.fewards.data.repository

import android.content.Context
import androidx.core.content.edit
import com.functy.fewards.fewardsApp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

private const val SETTINGS_PREFS = "settings"

class SettingsRepositoryImpl : SettingsRepository {

    private val prefs by lazy {
        fewardsApp.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
    }

    override var themeMode: Int
        get() = prefs.getInt("color_mode", 0)
        set(value) = prefs.edit { putInt("color_mode", value) }

    override var miuixMonet: Boolean
        get() = prefs.getBoolean("miuix_monet", false)
        set(value) = prefs.edit { putBoolean("miuix_monet", value) }

    override var keyColor: Int
        get() = prefs.getInt("key_color", 0)
        set(value) = prefs.edit { putInt("key_color", value) }

    override var colorStyle: String
        get() = prefs.getString("color_style", PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        set(value) = prefs.edit { putString("color_style", value) }

    override var colorSpec: String
        get() = prefs.getString("color_spec", ColorSpec.SpecVersion.SPEC_2025.name) ?: ColorSpec.SpecVersion.SPEC_2025.name
        set(value) = prefs.edit { putString("color_spec", value) }

    override var enableBlur: Boolean
        get() = prefs.getBoolean("enable_blur", false)
        set(value) = prefs.edit { putBoolean("enable_blur", value) }

    override var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar", value) }

    override var enableFloatingBottomBarBlur: Boolean
        get() = prefs.getBoolean("enable_floating_bottom_bar_blur", false)
        set(value) = prefs.edit { putBoolean("enable_floating_bottom_bar_blur", value) }

    override var enableNavigationBadge: Boolean
        get() = prefs.getBoolean("enable_navigation_badge", true)
        set(value) = prefs.edit { putBoolean("enable_navigation_badge", value) }

    override var navigationRailExpanded: Boolean
        get() = prefs.getBoolean("nav_rail_expanded", false)
        set(value) = prefs.edit { putBoolean("nav_rail_expanded", value) }

    override var pageScale: Float
        get() = prefs.getFloat("page_scale", 1.0f)
        set(value) = prefs.edit { putFloat("page_scale", value) }

    override var enablePredictiveBack: Boolean
        get() = prefs.getBoolean("enable_predictive_back", false)
        set(value) = prefs.edit { putBoolean("enable_predictive_back", value) }

    override var predictiveBackAnimation: Int
        get() = prefs.getInt("predictive_back_animation", 2) // 默认 AOSP
        set(value) = prefs.edit { putInt("predictive_back_animation", value) }

    override var scheduleEnabled: Boolean
        get() = prefs.getBoolean("schedule_enabled", false)
        set(value) = prefs.edit { putBoolean("schedule_enabled", value) }

    override var scheduleHour: Int
        get() = prefs.getInt("schedule_hour", 8)
        set(value) = prefs.edit { putInt("schedule_hour", value) }

    override var scheduleMinute: Int
        get() = prefs.getInt("schedule_minute", 0)
        set(value) = prefs.edit { putInt("schedule_minute", value) }

    override var taskNotification: Boolean
        get() = prefs.getBoolean("task_notification", true)
        set(value) = prefs.edit { putBoolean("task_notification", value) }

    override var overviewAutoDismiss: Boolean
        get() = prefs.getBoolean("overview_auto_dismiss", true)
        set(value) = prefs.edit { putBoolean("overview_auto_dismiss", value) }

    override var overviewHoldSeconds: Float
        get() = prefs.getFloat("overview_hold_seconds", 0.5f)
        set(value) = prefs.edit { putFloat("overview_hold_seconds", value) }

    override var mhyMasterEnabled: Boolean
        get() = prefs.getBoolean("mhy_master_enabled", true)
        set(value) = prefs.edit { putBoolean("mhy_master_enabled", value) }

    override var mhyGameSign: Boolean
        get() = prefs.getBoolean("mhy_game_sign", true)
        set(value) = prefs.edit { putBoolean("mhy_game_sign", value) }

    override var mhyBbsSign: Boolean
        get() = prefs.getBoolean("mhy_bbs_sign", true)
        set(value) = prefs.edit { putBoolean("mhy_bbs_sign", value) }

    override var mhyRead: Boolean
        get() = prefs.getBoolean("mhy_read", true)
        set(value) = prefs.edit { putBoolean("mhy_read", value) }

    override var mhyLike: Boolean
        get() = prefs.getBoolean("mhy_like", true)
        set(value) = prefs.edit { putBoolean("mhy_like", value) }

    override var mhyCancelLike: Boolean
        get() = prefs.getBoolean("mhy_cancel_like", true)
        set(value) = prefs.edit { putBoolean("mhy_cancel_like", value) }

    override var mhyShare: Boolean
        get() = prefs.getBoolean("mhy_share", true)
        set(value) = prefs.edit { putBoolean("mhy_share", value) }

    override var mhySignGames: String
        get() = prefs.getString("mhy_sign_games", "genshin,starrail,zzz") ?: "genshin,starrail,zzz"
        set(value) = prefs.edit { putString("mhy_sign_games", value) }

    override var mhyForums: String
        get() = prefs.getString("mhy_forums", "5,2") ?: "5,2"
        set(value) = prefs.edit { putString("mhy_forums", value) }

    override var mhyCaptchaPolicy: Int
        get() = prefs.getInt("mhy_captcha_policy", 0)
        set(value) = prefs.edit { putInt("mhy_captcha_policy", value) }

    override var mhyCaptchaApiUrl: String
        get() = prefs.getString("mhy_captcha_api_url", "") ?: ""
        set(value) = prefs.edit { putString("mhy_captcha_api_url", value) }

    override var wbMasterEnabled: Boolean
        get() = prefs.getBoolean("wb_master_enabled", true)
        set(value) = prefs.edit { putBoolean("wb_master_enabled", value) }
}
