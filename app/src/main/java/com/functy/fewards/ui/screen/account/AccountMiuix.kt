package com.functy.fewards.ui.screen.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.functy.fewards.R
import com.functy.fewards.ui.component.miuix.MultilineInputField
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import com.functy.fewards.ui.viewmodel.AccountViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 账号页（Miuix）：大标题「账号」；从上到下为米游社区域、WorkBuddy 区域。
 * 扫码登录弹出独立二维码窗口（WindowDialog）。
 */
@Composable
fun AccountPagerMiuix(
    accountViewModel: AccountViewModel,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val state by accountViewModel.uiState.collectAsStateWithLifecycle()
    val actions = accountViewModel.accountActions

    // 二维码弹窗：Loading/Waiting/Scanned 时弹出，Confirmed/Error/Expired/Idle 关闭
    var showQrDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.qrState) {
        showQrDialog = when (state.qrState) {
            AccountUiState.QrState.Loading,
            AccountUiState.QrState.Waiting,
            AccountUiState.QrState.Scanned -> true
            else -> false
        }
    }
    if (showQrDialog) {
        WindowDialog(
            show = true,
            title = stringResource(R.string.miyoushe_login_qr),
            onDismissRequest = {
                actions.onCancelQr()
            },
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 固定 240dp 二维码区：Loading 时显示加载提示 + 加载动画，加载完成原位替换二维码
                    Box(
                        modifier = Modifier.size(240.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.qrContent.isNotEmpty() &&
                            (state.qrState == AccountUiState.QrState.Waiting || state.qrState == AccountUiState.QrState.Scanned)
                        ) {
                            QrImage(content = state.qrContent)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                InfiniteProgressIndicator(color = colorScheme.primary)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.processing),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            when (state.qrState) {
                                AccountUiState.QrState.Scanned -> R.string.miyoushe_qr_scanned
                                else -> R.string.miyoushe_qr_wait_scan
                            }
                        ),
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(14.dp))
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { actions.onCancelQr() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = stringResource(R.string.account_title),
                scrollBehavior = scrollBehavior,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ==================== 米游社 ====================
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                            Text(
                                text = stringResource(R.string.miyoushe),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    if (state.mihoyoLoggedIn) R.string.miyoushe_status_logged_in
                                    else R.string.miyoushe_status_not_logged_in
                                ),
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.height(12.dp))
                            // 扫码 / Cookie 切换：选中项主色高亮
                            TabRow(
                                tabs = listOf(
                                    stringResource(R.string.miyoushe_login_qr),
                                    stringResource(R.string.miyoushe_login_cookie),
                                ),
                                selectedTabIndex = state.loginMode,
                                onTabSelected = actions.onSetLoginMode,
                                colors = TabRowDefaults.tabRowColors(
                                    backgroundColor = Color.Transparent,
                                    selectedContentColor = colorScheme.primary,
                                    contentColor = colorScheme.onSurfaceVariantSummary,
                                ),
                            )
                            Spacer(Modifier.height(12.dp))
                            if (state.loginMode == 0) {
                                // 扫码登录：按钮弹出独立二维码窗口
                                TextButton(
                                    text = stringResource(
                                        when (state.qrState) {
                                            AccountUiState.QrState.Waiting, AccountUiState.QrState.Scanned -> R.string.miyoushe_qr_wait_scan
                                            AccountUiState.QrState.Expired -> R.string.miyoushe_qr_refresh
                                            else -> R.string.miyoushe_qr_generate
                                        }
                                    ),
                                    onClick = actions.onStartQr,
                                    enabled = state.qrState != AccountUiState.QrState.Waiting &&
                                        state.qrState != AccountUiState.QrState.Scanned,
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                CookieSection(actions)
                            }
                        }
                    }

                    // 已登录账号列表
                    if (state.mihoyoAccounts.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                state.mihoyoAccounts.forEach { account ->
                                    ArrowPreference(
                                        title = account.nickname,
                                        summary = "stuid=${account.stuid}",
                                        startAction = {
                                            Icon(
                                                Icons.Rounded.Person,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = account.nickname,
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        endActions = {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(R.string.miyoushe_logout),
                                                tint = colorScheme.onSurfaceVariantSummary,
                                            )
                                        },
                                        onClick = { actions.onRemoveMihoyo(account.id) },
                                    )
                                }
                            }
                        }
                    }

                    // ==================== WorkBuddy ====================
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                            Text(
                                text = stringResource(R.string.workbuddy),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    if (state.wbLoggedIn) R.string.workbuddy_status_logged_in
                                    else R.string.workbuddy_status_not_logged_in
                                ),
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(Modifier.height(12.dp))
                            var wbToken by rememberSaveable { mutableStateOf("") }
                            MultilineInputField(
                                value = wbToken,
                                onValueChange = { wbToken = it },
                                label = stringResource(R.string.workbuddy_token_hint),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(
                                    text = stringResource(R.string.workbuddy_token_import),
                                    onClick = {
                                        actions.onImportWbToken(wbToken)
                                        wbToken = ""
                                    },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                            }
                        }
                    }

                    if (state.wbAccounts.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                state.wbAccounts.forEach { account ->
                                    ArrowPreference(
                                        title = account.label,
                                        summary = "token=${account.token.take(6)}****",
                                        startAction = {
                                            Icon(
                                                Icons.Rounded.Person,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = account.label,
                                                tint = colorScheme.onBackground,
                                            )
                                        },
                                        endActions = {
                                            Icon(
                                                Icons.Rounded.Delete,
                                                modifier = Modifier.padding(end = 6.dp),
                                                contentDescription = stringResource(R.string.workbuddy_logout),
                                                tint = colorScheme.onSurfaceVariantSummary,
                                            )
                                        },
                                        onClick = { actions.onRemoveWb(account.id) },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun CookieSection(
    actions: AccountActions,
) {
    Column {
        var cookie by rememberSaveable { mutableStateOf("") }
        MultilineInputField(
            value = cookie,
            onValueChange = { cookie = it },
            label = stringResource(R.string.miyoushe_cookie_hint),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(
                text = stringResource(R.string.miyoushe_cookie_import),
                onClick = {
                    if (cookie.contains("stoken")) {
                        actions.onImportCookie(cookie)
                        cookie = ""
                    }
                },
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

/** zxing 生成二维码位图。 */
@Composable
private fun QrImage(content: String) {
    if (content.isEmpty()) return
    val size = 240.dp
    val bitmap = remember(content) {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 512, 512,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        val bmp = android.graphics.Bitmap.createBitmap(matrix.width, matrix.height, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "QR",
        modifier = Modifier.size(size),
    )
}
