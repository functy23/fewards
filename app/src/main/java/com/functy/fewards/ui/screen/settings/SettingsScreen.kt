package com.functy.fewards.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.LocalUiMode
import com.functy.fewards.ui.UiMode
import com.functy.fewards.ui.navigation.Navigator
import com.functy.fewards.ui.navigation.Route
import com.functy.fewards.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(
    navigator: Navigator,
    bottomInnerPadding: Dp
) {
    val viewModel = viewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val actions = SettingsScreenActions(
        onSetUiModeIndex = { index ->
            viewModel.setUiMode(if (index == 0) UiMode.Miuix.value else UiMode.Material.value)
        },
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onSetMhyMaster = viewModel::setMhyMasterEnabled,
        onSetMhyGameSign = viewModel::setMhyGameSign,
        onSetMhyBbsSign = viewModel::setMhyBbsSign,
        onSetMhyRead = viewModel::setMhyRead,
        onSetMhyLike = viewModel::setMhyLike,
        onSetMhyCancelLike = viewModel::setMhyCancelLike,
        onSetMhyShare = viewModel::setMhyShare,
        onSetMhyCaptchaPolicy = viewModel::setMhyCaptchaPolicy,
        onSetMhyCaptchaApiUrl = viewModel::setMhyCaptchaApiUrl,
        onSetWbMaster = viewModel::setWbMasterEnabled,
        onSetScheduleEnabled = viewModel::setScheduleEnabled,
        onSetScheduleTime = viewModel::setScheduleTime,
        onSetTaskNotification = viewModel::setTaskNotification,
        onOpenAbout = { navigator.push(Route.About) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(uiState, actions, bottomInnerPadding)
        UiMode.Material -> SettingPagerMaterial(uiState, actions, bottomInnerPadding)
    }
}
