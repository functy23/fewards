package com.functy.fewards.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.dynamiccolor.ColorSpec
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

@Composable
fun MiuixFewardsTheme(
    appSettings: AppSettings,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && systemDarkTheme)
    val colorStyle = appSettings.paletteStyle
    val colorSpec = appSettings.colorSpec

    val miuixPaletteStyle = try {
        ThemePaletteStyle.valueOf(colorStyle.name)
    } catch (_: Exception) {
        ThemePaletteStyle.TonalSpot
    }

    val miuixColorSpec = if (colorSpec.effectiveFor(colorStyle) == ColorSpec.SpecVersion.SPEC_2025) {
        ThemeColorSpec.Spec2025
    } else {
        ThemeColorSpec.Spec2021
    }

    val resolvedKeyColor: Color? = when {
        appSettings.keyColor != 0 -> Color(appSettings.keyColor)
        appSettings.colorMode.isMonet ->
            if (darkTheme) dynamicDarkColorScheme(context).primary
            else dynamicLightColorScheme(context).primary

        else -> null
    }

    val colorSchemeMode = when (appSettings.colorMode) {
        ColorMode.SYSTEM -> ColorSchemeMode.System
        ColorMode.LIGHT -> ColorSchemeMode.Light
        ColorMode.DARK -> ColorSchemeMode.Dark
        ColorMode.MONET_SYSTEM -> ColorSchemeMode.MonetSystem
        ColorMode.MONET_LIGHT -> ColorSchemeMode.MonetLight
        ColorMode.MONET_DARK, ColorMode.DARK_AMOLED -> ColorSchemeMode.MonetDark
    }

    // remember：本组合会随设置项 / 系统明暗重组，而 ThemeController 每次构造都要
    // 现算浅色 + 深色两套色板（各 53 个角色）。
    val controller = remember(colorSchemeMode, resolvedKeyColor, darkTheme, miuixPaletteStyle, miuixColorSpec) {
        ThemeController(
            colorSchemeMode = colorSchemeMode,
            keyColor = resolvedKeyColor,
            isDark = darkTheme,
            paletteStyle = miuixPaletteStyle,
            colorSpec = miuixColorSpec,
        )
    }

    // 明暗 / 取色切换时给整套颜色角色插值（见 AnimatedThemeColors.kt）。
    // 用 colors 重载下发：库会把它按值拷进自己的 Colors 实例（不持有引用），
    // 插值期间每帧换一套新颜色即可；stable 期 animateColorAsState 不产生每帧工作。
    val targetColors = controller.currentColors()
    val animatedColors = animateThemeColors(targetColors)

    MiuixTheme(
        colors = animatedColors,
        content = {
            SystemBarsAppearance(surface = animatedColors.surface)
            CompositionLocalProvider(
                LocalThemeTargetColors provides targetColors,
                // colors 重载不像 controller 重载那样自带 LocalContentColor，需要自己补。
                LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    )
}

/**
 * 本轮切换的**目标**色板（不含过渡插值）。
 *
 * 给「拿主题色当输入再算一整套派生配色」的地方用（主题页的预览卡会喂给
 * materialkolor 重新生成整套 MD3 配色）。若让它们吃插值中的颜色，过渡期间会每帧
 * 重算一次完整配色方案；吃目标值则只在切换那一刻算一次。
 */
internal val LocalThemeTargetColors = staticCompositionLocalOf<Colors?> { null }

/**
 * 系统栏图标的明暗跟着**过渡后的**表面亮度走，而不是跟着 [darkTheme] 立刻翻。
 *
 * 过渡中途底色会经过中间灰，若在切换瞬间就把图标翻成浅色，会出现一段
 * 「浅底白图标」的不可读时间；按实际亮度判定，翻面正好落在过渡中段，全程可读。
 */
@Composable
private fun SystemBarsAppearance(surface: Color) {
    val context = LocalContext.current
    val lightSystemBarIcons = surface.luminance() > 0.5f
    LaunchedEffect(lightSystemBarIcons) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightSystemBarIcons
            isAppearanceLightNavigationBars = lightSystemBarIcons
        }
    }
}
