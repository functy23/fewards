package com.functy.fewards.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.functy.fewards.ui.component.bottombar.BottomBar
import com.functy.fewards.ui.component.bottombar.MainPagerState
import com.functy.fewards.ui.component.bottombar.SideRail
import com.functy.fewards.ui.component.bottombar.rememberMainPagerState
import com.functy.fewards.ui.component.bottombar.useNavigationRail
import com.functy.fewards.ui.navigation.LocalNavigator
import com.functy.fewards.ui.navigation.Navigator
import com.functy.fewards.ui.navigation.Route
import com.functy.fewards.ui.screen.account.AccountPager
import com.functy.fewards.ui.screen.about.AboutScreen
import com.functy.fewards.ui.screen.colorpalette.ColorPaletteScreen
import com.functy.fewards.ui.screen.home.HomePager
import com.functy.fewards.ui.screen.settings.SettingPager
import com.functy.fewards.ui.theme.FewardsTheme
import com.functy.fewards.ui.theme.LocalColorMode
import com.functy.fewards.ui.theme.LocalEnableBlur
import com.functy.fewards.ui.theme.LocalEnableFloatingBottomBar
import com.functy.fewards.ui.theme.LocalEnableFloatingBottomBarBlur
import com.functy.fewards.ui.animation.predictiveback.PredictiveBackAnimation
import com.functy.fewards.ui.animation.predictiveback.installerNavTransition
import com.functy.fewards.ui.util.rememberBlurBackdrop
import com.functy.fewards.ui.util.rememberContentReady
import com.functy.fewards.ui.util.rememberDeviceCornerRadius
import com.functy.fewards.ui.viewmodel.MainActivityViewModel
import com.functy.fewards.ui.viewmodel.MainPagerConfig
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

class MainActivity : ComponentActivity() {

    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !contentReady }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
            val appSettings = uiState.appSettings
            val darkMode = appSettings.colorMode.isDark || (appSettings.colorMode.isSystem && isSystemInDarkTheme())

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { darkMode },
                )
                window.isNavigationBarContrastEnforced = false
                onDispose { }
            }

            // 不用 rememberNavBackStack（rememberSaveable 恢复版）：recreate 后恢复栈与初始值
            // 叠加会触发 NavDisplay「Duplicate contentKey」崩溃。改为进程内保存，recreate 重置主页。
            val backStack = remember { top.yukonga.miuix.kmp.nav.core.navBackStackOf(Route.Main) }
            val navigator = remember(backStack) { Navigator(backStack) }
            val systemDensity = LocalDensity.current
            val density = remember(systemDensity, uiState.pageScale) {
                Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
            }

            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDensity provides density,
                LocalColorMode provides appSettings.colorMode.value,
                LocalEnableBlur provides uiState.enableBlur,
                LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
                LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
            ) {
                FewardsTheme(appSettings = appSettings) {
                    val mainScreenEntry = @Composable {
                        MainScreen(
                            initialPage = selectedMainPage,
                            onPageChanged = viewModel::setSelectedMainPage,
                        )
                    }

                    val navDisplay = @Composable {
                        val navCornerRadius = rememberDeviceCornerRadius()
                        val backdropColor = MiuixTheme.colorScheme.surface
                        val animation = PredictiveBackAnimation.entries[
                            uiState.predictiveBackAnimation.coerceIn(0, PredictiveBackAnimation.entries.lastIndex)
                        ]
                        val roundAllCorners = animation == PredictiveBackAnimation.AOSP ||
                            animation == PredictiveBackAnimation.Scale ||
                            animation == PredictiveBackAnimation.Classic
                        val effects = remember(navCornerRadius, backdropColor, roundAllCorners, animation) {
                            NavDisplayEffects(
                                enableCornerClip = true,
                                cornerClipRadius = if (roundAllCorners && navCornerRadius <= 0.dp) 32.dp else navCornerRadius,
                                cornerClipMode = if (roundAllCorners) NavCornerClipMode.All else NavCornerClipMode.Leading,
                                dimAmount = 0.5f,
                                backdropColor = backdropColor,
                                blockInputDuringTransition = false,
                            )
                        }
                        val transition = remember(animation) { installerNavTransition(animation) }
                        NavDisplay(
                            backStack = backStack,
                            onBack = { navigator.pop() },
                            transition = transition,
                            effects = effects,
                        ) {
                            entry<Route.Main> { mainScreenEntry() }
                            // 入场层第一帧就铺满 surface，盖住库 scrim 首帧 alpha=1 的全黑。
                            entry<Route.About> {
                                Box(Modifier.fillMaxSize().background(backdropColor)) { AboutScreen() }
                            }
                            entry<Route.ColorPalette> {
                                Box(Modifier.fillMaxSize().background(backdropColor)) { ColorPaletteScreen() }
                            }
                        }
                    }

                    // 与 InstallerX 一致：NavDisplay 直接铺在 surface 上，不再套一层空 Scaffold。
                    // 空 Scaffold 在二级页入场首帧会露出默认深色窗体，表现为「闪一下」。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface),
                    ) { navDisplay() }
                    SideEffect { contentReady = true }
                }
            }
        }
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

@Composable
fun MainScreen(
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    val useNavigationRail = useNavigationRail(enableFloatingBottomBar)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(
        pagerState = pagerState,
        animatePageChanges = !useNavigationRail,
    )

    val surfaceColor = MiuixTheme.colorScheme.surface
    val blurBackdrop = rememberBlurBackdrop(enableBlur)

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        val contentReady = rememberContentReady()
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .then(if (enableFloatingBottomBar && enableFloatingBottomBarBlur) Modifier.layerBackdrop(backdrop) else Modifier),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 2 else 0,
                    overscrollEffect = null,
                    userScrollEnabled = true,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (isCurrentPage || contentReady) HomePager(bottomInnerPadding, isCurrentPage)
                        1 -> if (isCurrentPage || contentReady) AccountPager(bottomInnerPadding, isCurrentPage)
                        2 -> if (isCurrentPage || contentReady) SettingPager(navController, bottomInnerPadding)
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Start)
                val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                Scaffold { _ ->
                    Row {
                        SideRail()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            } else {
                val bottomBar = @Composable {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BottomBar(
                            blurBackdrop = blurBackdrop,
                            backdrop = backdrop,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }

                Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}


@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 && mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}
