package com.functy.fewards.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.functy.fewards.ui.UiMode
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = PaletteStyle.TonalSpot.name,
    val colorSpec: String = ColorSpec.SpecVersion.SPEC_2025.name,
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val pageScale: Float = 1.0f,

    // 米游社
    val mhyMasterEnabled: Boolean = true,
    val mhyGameSign: Boolean = true,
    val mhyBbsSign: Boolean = true,
    val mhyRead: Boolean = true,
    val mhyLike: Boolean = true,
    val mhyCancelLike: Boolean = true,
    val mhyShare: Boolean = true,
    val mhySignGames: String = "genshin,starrail,zzz",
    val mhyForums: String = "5,2",
    val mhyCaptchaPolicy: Int = 0, // 0 跳过并记录；1 打码接口
    val mhyCaptchaApiUrl: String = "",

    // WorkBuddy
    val wbMasterEnabled: Boolean = true,

    // 调度
    val scheduleEnabled: Boolean = false,
    val scheduleHour: Int = 8,
    val scheduleMinute: Int = 0,
    val taskNotification: Boolean = true,
)

@Immutable
data class SettingsScreenActions(
    val onSetUiModeIndex: (Int) -> Unit,
    val onOpenTheme: () -> Unit,
    // 米游社
    val onSetMhyMaster: (Boolean) -> Unit,
    val onSetMhyGameSign: (Boolean) -> Unit,
    val onSetMhyBbsSign: (Boolean) -> Unit,
    val onSetMhyRead: (Boolean) -> Unit,
    val onSetMhyLike: (Boolean) -> Unit,
    val onSetMhyCancelLike: (Boolean) -> Unit,
    val onSetMhyShare: (Boolean) -> Unit,
    val onSetMhyCaptchaPolicy: (Int) -> Unit,
    val onSetMhyCaptchaApiUrl: (String) -> Unit,
    // WorkBuddy
    val onSetWbMaster: (Boolean) -> Unit,
    // 调度
    val onSetScheduleEnabled: (Boolean) -> Unit,
    val onSetScheduleTime: (Int, Int) -> Unit,
    val onSetTaskNotification: (Boolean) -> Unit,
    // 其他
    val onOpenAbout: () -> Unit,
)
