package com.functy.fewards.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors

/**
 * 明暗 / 取色切换的过渡时长。
 *
 * 与 miuix 库自身弹窗时长（`MiuixPopupUtils`：弹出 300ms、收起 250ms）同一量级，
 * 而不是 Compose `MotionScheme` 里给共享轴转场用的那套（几百毫秒起步）。
 */
internal const val THEME_TRANSITION_MILLIS = 300

/** 主题色过渡曲线：起步快、收尾稳，整屏换色不拖沓。 */
private val ThemeTransitionEasing = FastOutSlowInEasing

/**
 * 主题过渡的动画规格。
 *
 * 不跟随主题、自带深浅两套的语义色（首页状态卡那种）也用它，
 * 这样整屏换色时不会出现「其它都在淡、它硬切一下」。
 */
internal fun <T> themeTransitionSpec(): TweenSpec<T> = tween(
    durationMillis = THEME_TRANSITION_MILLIS,
    easing = ThemeTransitionEasing,
)

/**
 * 把 [Colors] 的 53 个颜色角色按「旧值 → 新值」插值，产出平滑换色的主题。
 *
 * 一直挂着即可：`animateColorAsState` 首次组合就把初值设成目标色，不会在冷启动时
 * 演一次全屏淡入；只有目标色**按值**变化（明暗档位、取色、Monet 种子变了）才真的跑动画，
 * 稳定期不产生任何每帧工作。目标在动画途中再次变化时会从当前值续上，不会跳。
 *
 * 插值走 Compose 的 `Color.lerp`（Oklab 空间），深色↔浅色之间不会出现灰色带。
 */
@Composable
internal fun animateThemeColors(target: Colors): Colors {
    @Composable
    fun animate(color: Color, label: String): Color = animateColorAsState(
        targetValue = color,
        animationSpec = themeTransitionSpec(),
        label = label,
    ).value

    return Colors(
        primary = animate(target.primary, "theme_primary"),
        onPrimary = animate(target.onPrimary, "theme_onPrimary"),
        primaryVariant = animate(target.primaryVariant, "theme_primaryVariant"),
        onPrimaryVariant = animate(target.onPrimaryVariant, "theme_onPrimaryVariant"),
        error = animate(target.error, "theme_error"),
        onError = animate(target.onError, "theme_onError"),
        errorContainer = animate(target.errorContainer, "theme_errorContainer"),
        onErrorContainer = animate(target.onErrorContainer, "theme_onErrorContainer"),
        disabledPrimary = animate(target.disabledPrimary, "theme_disabledPrimary"),
        disabledOnPrimary = animate(target.disabledOnPrimary, "theme_disabledOnPrimary"),
        disabledPrimaryButton = animate(target.disabledPrimaryButton, "theme_disabledPrimaryButton"),
        disabledOnPrimaryButton = animate(target.disabledOnPrimaryButton, "theme_disabledOnPrimaryButton"),
        disabledPrimarySlider = animate(target.disabledPrimarySlider, "theme_disabledPrimarySlider"),
        primaryContainer = animate(target.primaryContainer, "theme_primaryContainer"),
        onPrimaryContainer = animate(target.onPrimaryContainer, "theme_onPrimaryContainer"),
        secondary = animate(target.secondary, "theme_secondary"),
        onSecondary = animate(target.onSecondary, "theme_onSecondary"),
        secondaryVariant = animate(target.secondaryVariant, "theme_secondaryVariant"),
        onSecondaryVariant = animate(target.onSecondaryVariant, "theme_onSecondaryVariant"),
        disabledSecondary = animate(target.disabledSecondary, "theme_disabledSecondary"),
        disabledOnSecondary = animate(target.disabledOnSecondary, "theme_disabledOnSecondary"),
        disabledSecondaryVariant = animate(target.disabledSecondaryVariant, "theme_disabledSecondaryVariant"),
        disabledOnSecondaryVariant = animate(target.disabledOnSecondaryVariant, "theme_disabledOnSecondaryVariant"),
        secondaryContainer = animate(target.secondaryContainer, "theme_secondaryContainer"),
        onSecondaryContainer = animate(target.onSecondaryContainer, "theme_onSecondaryContainer"),
        secondaryContainerVariant = animate(target.secondaryContainerVariant, "theme_secondaryContainerVariant"),
        onSecondaryContainerVariant = animate(target.onSecondaryContainerVariant, "theme_onSecondaryContainerVariant"),
        tertiaryContainer = animate(target.tertiaryContainer, "theme_tertiaryContainer"),
        onTertiaryContainer = animate(target.onTertiaryContainer, "theme_onTertiaryContainer"),
        tertiaryContainerVariant = animate(target.tertiaryContainerVariant, "theme_tertiaryContainerVariant"),
        background = animate(target.background, "theme_background"),
        onBackground = animate(target.onBackground, "theme_onBackground"),
        onBackgroundVariant = animate(target.onBackgroundVariant, "theme_onBackgroundVariant"),
        surface = animate(target.surface, "theme_surface"),
        onSurface = animate(target.onSurface, "theme_onSurface"),
        surfaceVariant = animate(target.surfaceVariant, "theme_surfaceVariant"),
        onSurfaceSecondary = animate(target.onSurfaceSecondary, "theme_onSurfaceSecondary"),
        onSurfaceVariantSummary = animate(target.onSurfaceVariantSummary, "theme_onSurfaceVariantSummary"),
        onSurfaceVariantActions = animate(target.onSurfaceVariantActions, "theme_onSurfaceVariantActions"),
        disabledOnSurface = animate(target.disabledOnSurface, "theme_disabledOnSurface"),
        surfaceContainer = animate(target.surfaceContainer, "theme_surfaceContainer"),
        onSurfaceContainer = animate(target.onSurfaceContainer, "theme_onSurfaceContainer"),
        onSurfaceContainerVariant = animate(target.onSurfaceContainerVariant, "theme_onSurfaceContainerVariant"),
        surfaceContainerHigh = animate(target.surfaceContainerHigh, "theme_surfaceContainerHigh"),
        onSurfaceContainerHigh = animate(target.onSurfaceContainerHigh, "theme_onSurfaceContainerHigh"),
        surfaceContainerHighest = animate(target.surfaceContainerHighest, "theme_surfaceContainerHighest"),
        onSurfaceContainerHighest = animate(target.onSurfaceContainerHighest, "theme_onSurfaceContainerHighest"),
        outline = animate(target.outline, "theme_outline"),
        dividerLine = animate(target.dividerLine, "theme_dividerLine"),
        windowDimming = animate(target.windowDimming, "theme_windowDimming"),
        sliderKeyPoint = animate(target.sliderKeyPoint, "theme_sliderKeyPoint"),
        sliderKeyPointForeground = animate(target.sliderKeyPointForeground, "theme_sliderKeyPointForeground"),
        sliderBackground = animate(target.sliderBackground, "theme_sliderBackground"),
    )
}
