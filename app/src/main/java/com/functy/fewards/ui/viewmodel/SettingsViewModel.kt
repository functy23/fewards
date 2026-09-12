package com.functy.fewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.data.repository.SettingsRepository
import com.functy.fewards.data.repository.SettingsRepositoryImpl
import com.functy.fewards.ui.screen.settings.SettingsUiState
import com.functy.fewards.ui.theme.ColorMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    themeMode = repo.themeMode,
                    miuixMonet = repo.miuixMonet,
                    keyColor = repo.keyColor,
                    predictiveBackAnimation = repo.predictiveBackAnimation,
                    enableBlur = repo.enableBlur,
                    enableFloatingBottomBar = repo.enableFloatingBottomBar,
                    enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur,
                    pageScale = repo.pageScale,
                    // 米游社
                    mhyMasterEnabled = repo.mhyMasterEnabled,
                    mhyGameSign = repo.mhyGameSign,
                    mhyBbsSign = repo.mhyBbsSign,
                    mhyRead = repo.mhyRead,
                    mhyLike = repo.mhyLike,
                    mhyCancelLike = repo.mhyCancelLike,
                    mhyShare = repo.mhyShare,
                    mhyCaptchaPolicy = repo.mhyCaptchaPolicy,
                    mhyCaptchaApiUrl = repo.mhyCaptchaApiUrl,
                    // WorkBuddy
                    wbMasterEnabled = repo.wbMasterEnabled,
                    // 通知与完成总览
                    taskNotification = repo.taskNotification,
                    overviewAutoDismiss = repo.overviewAutoDismiss,
                    overviewHoldSeconds = repo.overviewHoldSeconds,
                )
            }
        }
    }

    // ==================== 界面 ====================

    fun setThemeMode(mode: Int) {
        val effectiveMode = if (_uiState.value.miuixMonet) {
            mode + 3
        } else {
            mode
        }
        repo.themeMode = effectiveMode
        _uiState.update { it.copy(themeMode = effectiveMode) }
    }

    fun setColorMode(mode: ColorMode) {
        repo.themeMode = mode.value
        _uiState.update { it.copy(themeMode = mode.value) }
    }

    fun setMiuixMonet(enabled: Boolean) {
        val currentThemeMode = repo.themeMode
        val colorMode = ColorMode.fromValue(currentThemeMode)
        val newThemeMode = if (enabled) {
            if (!colorMode.isMonet) colorMode.toMonetMode() else currentThemeMode
        } else {
            if (colorMode.isMonet) colorMode.toNonMonetMode() else currentThemeMode
        }
        repo.miuixMonet = enabled
        repo.themeMode = newThemeMode
        _uiState.update { it.copy(miuixMonet = enabled, themeMode = newThemeMode) }
    }

    fun setKeyColor(color: Int) {
        repo.keyColor = color
        _uiState.update { it.copy(keyColor = color) }
    }

    fun setColorStyle(style: String) {
        repo.colorStyle = style
        _uiState.update { it.copy(colorStyle = style) }
    }

    fun setColorSpec(spec: String) {
        repo.colorSpec = spec
        _uiState.update { it.copy(colorSpec = spec) }
    }

    fun setPredictiveBackAnimation(index: Int) {
        repo.predictiveBackAnimation = index
        _uiState.update { it.copy(predictiveBackAnimation = index) }
    }

    fun setEnableBlur(enabled: Boolean) {
        repo.enableBlur = enabled
        _uiState.update { it.copy(enableBlur = enabled) }
    }

    fun setEnableFloatingBottomBar(enabled: Boolean) {
        repo.enableFloatingBottomBar = enabled
        _uiState.update { it.copy(enableFloatingBottomBar = enabled) }
    }

    fun setEnableFloatingBottomBarBlur(enabled: Boolean) {
        repo.enableFloatingBottomBarBlur = enabled
        _uiState.update { it.copy(enableFloatingBottomBarBlur = enabled) }
    }

    fun setPageScale(scale: Float) {
        repo.pageScale = scale
        _uiState.update { it.copy(pageScale = scale) }
    }

    // ==================== 米游社 ====================

    fun setMhyMasterEnabled(enabled: Boolean) {
        repo.mhyMasterEnabled = enabled
        _uiState.update { it.copy(mhyMasterEnabled = enabled) }
    }

    fun setMhyGameSign(enabled: Boolean) {
        repo.mhyGameSign = enabled
        _uiState.update { it.copy(mhyGameSign = enabled) }
    }

    fun setMhyBbsSign(enabled: Boolean) {
        repo.mhyBbsSign = enabled
        _uiState.update { it.copy(mhyBbsSign = enabled) }
    }

    fun setMhyRead(enabled: Boolean) {
        repo.mhyRead = enabled
        _uiState.update { it.copy(mhyRead = enabled) }
    }

    fun setMhyLike(enabled: Boolean) {
        repo.mhyLike = enabled
        _uiState.update { it.copy(mhyLike = enabled) }
    }

    fun setMhyCancelLike(enabled: Boolean) {
        repo.mhyCancelLike = enabled
        _uiState.update { it.copy(mhyCancelLike = enabled) }
    }

    fun setMhyShare(enabled: Boolean) {
        repo.mhyShare = enabled
        _uiState.update { it.copy(mhyShare = enabled) }
    }

    fun setMhyCaptchaPolicy(policy: Int) {
        repo.mhyCaptchaPolicy = policy
        _uiState.update { it.copy(mhyCaptchaPolicy = policy) }
    }

    fun setMhyCaptchaApiUrl(url: String) {
        repo.mhyCaptchaApiUrl = url
        _uiState.update { it.copy(mhyCaptchaApiUrl = url) }
    }

    // ==================== WorkBuddy ====================

    fun setWbMasterEnabled(enabled: Boolean) {
        repo.wbMasterEnabled = enabled
        _uiState.update { it.copy(wbMasterEnabled = enabled) }
    }

    // ==================== 通知与完成总览 ====================

    fun setTaskNotification(enabled: Boolean) {
        repo.taskNotification = enabled
        _uiState.update { it.copy(taskNotification = enabled) }
    }

    fun setOverviewAutoDismiss(enabled: Boolean) {
        repo.overviewAutoDismiss = enabled
        _uiState.update { it.copy(overviewAutoDismiss = enabled) }
    }

    fun setOverviewHoldSeconds(seconds: Float) {
        repo.overviewHoldSeconds = seconds.coerceIn(0.05f, 30f)
        _uiState.update { it.copy(overviewHoldSeconds = repo.overviewHoldSeconds) }
    }
}
