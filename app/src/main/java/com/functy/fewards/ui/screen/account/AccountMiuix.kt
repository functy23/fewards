package com.functy.fewards.ui.screen.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.functy.fewards.R
import com.functy.fewards.ui.component.IconSquircleCornerFraction
import com.functy.fewards.ui.util.rememberBlurBackdrop
import com.functy.fewards.ui.component.miuix.AddAccountDialog
import com.functy.fewards.ui.viewmodel.AccountViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 账号页（Miuix）：
 *  - 只展示已登录账号，米游社与 WorkBuddy 用灰色小标题分组；
 *  - 右上角「+」弹出列表，选择「添加米游社账号 / 添加 WorkBuddy 账号」；
 *  - 两者都打开同一个 AddAccountDialog，弹窗内可切换扫码 / 凭据登录。
 */
@Composable
fun AccountPagerMiuix(
    accountViewModel: AccountViewModel,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val state by accountViewModel.uiState.collectAsStateWithLifecycle()
    val actions = accountViewModel.accountActions
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val dismissInput = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        view.clearFocus()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        dismissInput()
    }

    val backdrop = rememberBlurBackdrop()

    val addMenuAnchor = rememberGlassPopupAnchor()
    var showAddMenu by remember { mutableStateOf(false) }
    // show* 驱动 GlassDialog 的开合；mounted* 保证关闭动画播完之前不卸载弹窗
    // （切到 Cookie/Token Tab 会立刻把 qrState 归零，只靠 qrState 判断会动画演一半就消失）
    var showMhyDialog by remember { mutableStateOf(false) }
    var showWbDialog by remember { mutableStateOf(false) }
    var mhyDialogMounted by remember { mutableStateOf(false) }
    var wbDialogMounted by remember { mutableStateOf(false) }

    // 扫码成功才自动关弹窗；过期 / 失败保持打开以露出「重新获取」
    LaunchedEffect(state.qrState) {
        if (state.qrState == QrState.Confirmed) showMhyDialog = false
    }
    LaunchedEffect(state.wbQrState) {
        if (state.wbQrState == QrState.Confirmed) showWbDialog = false
    }

    // GlassDialog 没有 onDismissFinished（那是 WindowDialog 的），所以关闭后的收尾放在这里：
    // 等它的淡出动画（GlassMotion.fadeOut 150ms）播完再卸载弹窗并停掉轮询。
    // 之前用 WindowDialog 时这段在 onDismissFinished 里，换成玻璃弹窗后必须自己等。
    LaunchedEffect(showMhyDialog) {
        if (!showMhyDialog) {
            kotlinx.coroutines.delay(200)
            mhyDialogMounted = false
            actions.onCancelQr()
        }
    }
    LaunchedEffect(showWbDialog) {
        if (!showWbDialog) {
            kotlinx.coroutines.delay(200)
            wbDialogMounted = false
            actions.onCancelWbQr()
        }
    }
    // 停在扫码 Tab 且没有活着的会话时申请二维码。
    // 以「弹窗开合 + Tab」为 key：弹窗弹出动画（folme spring）结束后才申请，
    // 切走再切回扫码 Tab 也会重新申请一张新码；过期 / 失败后重开同样能自愈。
    LaunchedEffect(showMhyDialog, state.loginMode) {
        if (!showMhyDialog || state.loginMode != 0) return@LaunchedEffect
        if (state.qrState == QrState.Waiting || state.qrState == QrState.Scanned) return@LaunchedEffect
        kotlinx.coroutines.delay(450)
        if (showMhyDialog && state.loginMode == 0) actions.onStartQr()
    }
    LaunchedEffect(showWbDialog, state.wbLoginMode) {
        if (!showWbDialog || state.wbLoginMode != 0) return@LaunchedEffect
        if (state.wbQrState == QrState.Waiting || state.wbQrState == QrState.Scanned) return@LaunchedEffect
        kotlinx.coroutines.delay(450)
        if (showWbDialog && state.wbLoginMode == 0) actions.onStartWbQr()
    }

    if (mhyDialogMounted) {
        AddAccountDialog(
            show = showMhyDialog,
            backdrop = backdrop,
            title = stringResource(R.string.account_add_miyoushe),
            tabs = listOf(
                stringResource(R.string.miyoushe_login_qr),
                stringResource(R.string.miyoushe_login_cookie),
            ),
            selectedTab = state.loginMode,
            onTabSelected = { tab ->
                dismissInput()
                actions.onSetLoginMode(tab)
                // 离开扫码 Tab 时停掉轮询；再切回扫码时下面那个 LaunchedEffect 会重新申请一张新二维码
                if (tab != 0) actions.onCancelQr()
            },
            // 空闲 / 加载中不显示上一轮的二维码（切走再切回时先转圈，而不是闪一张旧码）；
            // 过期 / 失败仍保留那张码，配合「已失效 + 重新获取」。
            qrContent = when (state.qrState) {
                QrState.Idle, QrState.Loading -> ""
                else -> state.qrContent
            },
            qrMessage = stringResource(
                when (state.qrState) {
                    QrState.Scanned -> R.string.miyoushe_qr_scanned
                    QrState.Expired -> R.string.qr_expired
                    QrState.Error -> R.string.qr_failed
                    else -> R.string.miyoushe_qr_wait_scan
                }
            ),
            canRetry = state.qrState == QrState.Expired || state.qrState == QrState.Error,
            credentialLabel = stringResource(R.string.miyoushe_cookie_hint),
            credentialAction = stringResource(R.string.miyoushe_cookie_import),
            onImportCredential = { raw ->
                dismissInput()
                val accepted = actions.onImportCookie(raw)
                // 接受后关弹窗（走 GlassDialog 的淡出动画）；失败则留在原地让用户改
                if (accepted) showMhyDialog = false
                accepted
            },
            onDismissRequest = { showMhyDialog = false },
            onRetry = { actions.onStartQr() },
        )
    }

    if (wbDialogMounted) {
        AddAccountDialog(
            show = showWbDialog,
            backdrop = backdrop,
            title = stringResource(R.string.account_add_workbuddy),
            tabs = listOf(
                stringResource(R.string.workbuddy_login_qr),
                stringResource(R.string.workbuddy_login_token),
            ),
            selectedTab = state.wbLoginMode,
            onTabSelected = { tab ->
                dismissInput()
                actions.onSetWbLoginMode(tab)
                if (tab != 0) actions.onCancelWbQr()
            },
            qrContent = when (state.wbQrState) {
                QrState.Idle, QrState.Loading -> ""
                else -> state.wbQrContent
            },
            qrMessage = stringResource(
                when (state.wbQrState) {
                    QrState.Expired -> R.string.qr_expired
                    QrState.Error -> R.string.qr_failed
                    else -> R.string.workbuddy_qr_wait_scan
                }
            ),
            canRetry = state.wbQrState == QrState.Expired || state.wbQrState == QrState.Error,
            credentialLabel = stringResource(R.string.workbuddy_token_hint),
            credentialAction = stringResource(R.string.workbuddy_token_import),
            onImportCredential = { raw ->
                dismissInput()
                val accepted = actions.onImportWbToken(raw)
                if (accepted) showWbDialog = false
                accepted
            },
            onDismissRequest = { showWbDialog = false },
            onRetry = { actions.onStartWbQr() },
        )
    }

    Scaffold(
        topBar = {
            // miuix-glass 顶栏（PR #423）。「+」改由顶栏自带的玻璃圆钮承载：
            // 之前的 offset 补正是为了把它从「收起高度垂直居中」挪到大标题的中线上，
            // 那是普通 TopAppBar 的排法，GlassTopAppBar 会自己处理这一层。
            GlassTopAppBar(
                title = stringResource(R.string.account_title),
                isContentScrolled = listState.canScrollBackward,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                actions = {
                    // 菜单本体是 GlassTransformPopup（面板从按钮里「长出来」）。
                    // 它是 BoxScope 扩展，而 actions 槽是 RowScope，所以这里得套一层 Box；
                    // anchor 让 GlassIconButton 把自己的材质与 backdrop 共享给面板。
                    val addOptions = listOf(
                        stringResource(R.string.account_add_miyoushe),
                        stringResource(R.string.account_add_workbuddy),
                    )
                    Box {
                        GlassIconButton(
                            onClick = {
                                dismissInput()
                                showAddMenu = true
                            },
                            modifier = Modifier.glassPopupAnchor(
                                anchor = addMenuAnchor,
                                cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2,
                            ),
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.account_add),
                                tint = colorScheme.onSurface,
                            )
                        }
                        GlassTransformPopup(
                            show = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                            anchor = addMenuAnchor,
                            backdrop = backdrop,
                            anchorContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = colorScheme.onSurface,
                                )
                            },
                        ) {
                            addOptions.forEachIndexed { index, text ->
                                GlassPopupItem(
                                    text = text,
                                    onClick = {
                                        showAddMenu = false
                                        when (index) {
                                            0 -> {
                                                actions.onSetLoginMode(0)
                                                mhyDialogMounted = true
                                                showMhyDialog = true
                                            }
                                            else -> {
                                                actions.onSetWbLoginMode(0)
                                                wbDialogMounted = true
                                                showWbDialog = true
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .layerBackdrop(backdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { dismissInput() })
                }
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            // ==================== 米游社 ====================
            if (state.mihoyoAccounts.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.miyoushe)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.mihoyoAccounts.forEach { account ->
                                BasicComponent(
                                    title = account.nickname,
                                    summary = "stuid=${account.stuid}",
                                    startAction = {
                                        AccountFace(
                                            url = account.avatarUrl,
                                            label = account.nickname,
                                            hydrating = account.id in state.mhyHydratingIds,
                                        )
                                    },
                                    endActions = {
                                        IconButton(
                                            onClick = {
                                                dismissInput()
                                                actions.onRemoveMihoyo(account.id)
                                            },
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = stringResource(R.string.miyoushe_logout),
                                                tint = colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    },
                                    onClick = { dismissInput() },
                                )
                            }
                        }
                    }
                }
            }

            // ==================== WorkBuddy ====================
            if (state.wbAccounts.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.workbuddy)) }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            state.wbAccounts.forEach { account ->
                                BasicComponent(
                                    title = account.label,
                                    summary = if (account.uid.isNotEmpty()) {
                                        "uid=${account.uid}"
                                    } else {
                                        "token=${account.token.take(6)}****"
                                    },
                                    startAction = {
                                        AccountFace(
                                            url = account.avatarUrl,
                                            label = account.label,
                                            hydrating = false,
                                        )
                                    },
                                    endActions = {
                                        IconButton(
                                            onClick = {
                                                dismissInput()
                                                actions.onRemoveWb(account.id)
                                            },
                                        ) {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                contentDescription = stringResource(R.string.workbuddy_logout),
                                                tint = colorScheme.onSurfaceVariantSummary,
                                            )
                                        }
                                    },
                                    onClick = { dismissInput() },
                                )
                            }
                        }
                    }
                }
            }

            if (state.mihoyoAccounts.isEmpty() && state.wbAccounts.isEmpty()) {
                item { EmptyHint() }
            }

            item { Spacer(Modifier.height(bottomInnerPadding)) }
        }
    }
}

/** 灰色小标题：区分米游社 / WorkBuddy 账号分组。 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.account_empty),
            fontSize = 14.sp,
            color = colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccountFace(
    url: String,
    label: String,
    hydrating: Boolean,
    size: Dp = 36.dp,
) {
    val modifier = Modifier.padding(end = 6.dp)
    when {
        hydrating && url.isEmpty() -> AvatarLoading(size, modifier)
        url.isNotEmpty() -> UrlImage(url = url, size = size, modifier = modifier, contentDescription = label)
        else -> LetterAvatar(label = label, size = size, modifier = modifier)
    }
}

@Composable
private fun AvatarLoading(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        InfiniteProgressIndicator(color = colorScheme.primary)
    }
}

@Composable
private fun LetterAvatar(label: String, size: Dp, modifier: Modifier = Modifier) {
    val ch = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val hue = ((label.hashCode().toLong() and 0x7fffffffL) % 360L).toFloat()
    Box(
        modifier = modifier
            .size(size)
            .squircleClip(cornerRadius = size * IconSquircleCornerFraction)
            .background(Color.hsl(hue, 0.42f, 0.46f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = ch,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/** 网络头像：OkHttp 拉取 + miuix squircleClip；拉取中显示加载占位。 */
@Composable
private fun UrlImage(
    url: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    var bitmap by remember(url) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loaded by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        loaded = false
        bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                com.functy.fewards.fewardsApp.okhttpClient.newCall(
                    okhttp3.Request.Builder().url(url).build()
                ).execute().use { resp ->
                    val bytes = resp.body.bytes()
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }.getOrNull()
        }
        loaded = true
    }
    when {
        bitmap != null -> Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier.size(size).squircleClip(cornerRadius = size * IconSquircleCornerFraction),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        !loaded -> AvatarLoading(size, modifier)
        else -> LetterAvatar(label = contentDescription.orEmpty(), size = size, modifier = modifier)
    }
}
