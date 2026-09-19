package com.functy.fewards.ui.component.bottombar

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.functy.fewards.R
import com.functy.fewards.ui.LocalMainPagerState
import com.functy.fewards.ui.theme.LocalEnableFloatingBottomBar
import com.functy.fewards.ui.theme.LocalFloatingBottomBarGlass
import com.functy.fewards.ui.util.BlurredBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun BottomBarMiuix(
    blurBackdrop: LayerBackdrop?,
    backdrop: Backdrop,
    modifier: Modifier,
) {
    val mainState = LocalMainPagerState.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val glass = LocalFloatingBottomBarGlass.current

    val items = BottomBarDestination.entries.map { destination ->
        NavigationItem(
            label = stringResource(destination.label),
            icon = destination.icon,
        )
    }
    if (!enableFloatingBottomBar) {
        BlurredBar(blurBackdrop) {
            NavigationBar(
                modifier = modifier,
                color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
                content = {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            icon = item.icon,
                            label = item.label,
                            selected = mainState.selectedPage == index,
                            onClick = {
                                mainState.animateToPage(index)
                            },
                        )
                    }
                }
            )
        }
    } else {
        // miuix-glass 的 GlassNavigationBar（compose-miuix-ui/miuix PR #423）：
        // 材质、指示器跟随、按压反馈都由库负责，外部位置仍由调用方给。
        // 底栏 inset < 24dp 时固定 24dp，否则 inset + 8dp —— 与库示例同一规则。
        val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            .let { inset -> if (inset < 24.dp) 24.dp else inset + 8.dp }
        // 玻璃面板靠 RuntimeShader（AGSL，API 33+）。31/32 上 drawBackdrop 会被
        // isRuntimeShaderSupported() 整条关掉，GlassNavigationBar 会只剩描边和阴影，
        // 在深色页面上读起来就是「内容被挖了个洞」。所以 31/32 退回 miuix 自带的
        // FloatingNavigationBar：同样是悬浮胶囊，只是没有折射材质。
        val glassSupported = remember { isRuntimeShaderSupported() }
        if (glass && glassSupported) {
            GlassNavigationBar(
                items = BottomBarDestination.entries.map { destination ->
                    GlassNavigationItem(
                        icon = destination.icon,
                        label = stringResource(destination.label),
                    )
                },
                selectedIndex = mainState.selectedPage,
                onSelect = { mainState.animateToPage(it) },
                backdrop = backdrop,
                modifier = modifier.padding(start = 24.dp, end = 24.dp, bottom = bottomPadding),
            )
        } else {
            FloatingNavigationBar(
                modifier = modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = bottomPadding)
                    .pointerInput(Unit) { detectTapGestures { } },
                horizontalAlignment = Alignment.CenterHorizontally,
                defaultWindowInsetsPadding = false,
            ) {
                items.forEachIndexed { index, item ->
                    FloatingNavigationBarItem(
                        selected = mainState.selectedPage == index,
                        onClick = { mainState.animateToPage(index) },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        }
    }
}

enum class BottomBarDestination(
    @get:StringRes val label: Int,
    val icon: ImageVector,
) {
    Home(R.string.home, Icons.Rounded.Cottage),
    Account(R.string.account, Icons.Rounded.Person),
    Setting(R.string.settings, Icons.Rounded.Settings)
}
