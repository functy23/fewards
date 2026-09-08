package com.functy.fewards.ui.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.functy.fewards.fewardsApp
import com.functy.fewards.work.TaskNotifier
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functy.fewards.R
import com.functy.fewards.ui.UiMode
import com.functy.fewards.ui.component.miuix.MultilineInputField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.viewmodel.ConfigTransferViewModel
import com.functy.fewards.ui.theme.LocalEnableBlur
import com.functy.fewards.ui.util.BlurredBar
import com.functy.fewards.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 设置页（Miuix）：大标题「设置」，从上到下为界面组、米游社板块、WorkBuddy 板块、定时任务、关于。
 */
@Composable
fun SettingPagerMiuix(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    color = barColor,
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
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
                    // ==================== 界面 ====================
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_theme),
                            summary = stringResource(id = R.string.settings_theme_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Palette,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_theme),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenTheme
                        )
                    }

                    // ==================== 米游社 ====================
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_master),
                            summary = stringResource(id = R.string.settings_mhy_master_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Shield,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_master),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.mhyMasterEnabled,
                            onCheckedChange = actions.onSetMhyMaster
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_game_sign),
                            summary = stringResource(id = R.string.settings_mhy_game_sign_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Gamepad,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_game_sign),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyGameSign,
                            onCheckedChange = actions.onSetMhyGameSign
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_bbs_sign),
                            summary = stringResource(id = R.string.settings_mhy_bbs_sign_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.WorkspacePremium,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_bbs_sign),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyBbsSign,
                            onCheckedChange = actions.onSetMhyBbsSign
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_read),
                            summary = stringResource(id = R.string.settings_mhy_read_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_read),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyRead,
                            onCheckedChange = actions.onSetMhyRead
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_like),
                            summary = stringResource(id = R.string.settings_mhy_like_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.ThumbUp,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_like),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyLike,
                            onCheckedChange = actions.onSetMhyLike
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_cancel_like),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Recommend,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_cancel_like),
                                    tint = if (uiState.mhyMasterEnabled && uiState.mhyLike) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled && uiState.mhyLike,
                            checked = uiState.mhyCancelLike,
                            onCheckedChange = actions.onSetMhyCancelLike
                        )
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_mhy_share),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Person,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_share),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyShare,
                            onCheckedChange = actions.onSetMhyShare
                        )
                        OverlayDropdownPreference(
                            title = stringResource(id = R.string.settings_mhy_captcha),
                            items = listOf(
                                stringResource(id = R.string.settings_mhy_captcha_skip),
                                stringResource(id = R.string.settings_mhy_captcha_api),
                            ),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Shield,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_mhy_captcha),
                                    tint = if (uiState.mhyMasterEnabled) colorScheme.onBackground else colorScheme.disabledOnSecondaryVariant
                                )
                            },
                            enabled = uiState.mhyMasterEnabled,
                            selectedIndex = uiState.mhyCaptchaPolicy,
                            onSelectedIndexChange = actions.onSetMhyCaptchaPolicy
                        )
                        if (uiState.mhyCaptchaPolicy == 1) {
                            MultilineInputField(
                                value = uiState.mhyCaptchaApiUrl,
                                onValueChange = actions.onSetMhyCaptchaApiUrl,
                                label = stringResource(id = R.string.settings_mhy_captcha_api_url),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }

                    // ==================== WorkBuddy ====================
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_wb_master),
                            summary = stringResource(id = R.string.settings_wb_master_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Shield,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_wb_master),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.wbMasterEnabled,
                            onCheckedChange = actions.onSetWbMaster
                        )
                    }

                    // ==================== 定时 ====================
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        SwitchPreference(
                            title = stringResource(id = R.string.settings_notification),
                            summary = stringResource(id = R.string.settings_notification_summary),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_notification),
                                    tint = colorScheme.onBackground
                                )
                            },
                            checked = uiState.taskNotification,
                            onCheckedChange = actions.onSetTaskNotification
                        )
                        val context = LocalContext.current
                        var granted = remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(fewardsApp, Manifest.permission.POST_NOTIFICATIONS) ==
                                    PackageManager.PERMISSION_GRANTED
                            )
                        }
                        val launcher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { result -> granted.value = result }
                        ArrowPreference(
                            title = stringResource(id = R.string.settings_request_notification),
                            summary = stringResource(
                                if (granted.value) R.string.settings_notification_granted
                                else R.string.settings_notification_denied
                            ),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.settings_request_notification),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= 33 && !granted.value) {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_notification_granted),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        )
                    }

                    // ==================== 导入 / 导出 ====================
                    Card(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        val transferViewModel = viewModel<ConfigTransferViewModel>()
                        val transferStatus by transferViewModel.status.collectAsStateWithLifecycle()
                        var importText by rememberSaveable { mutableStateOf("") }
                        if (transferStatus.isNotEmpty()) {
                            Text(
                                text = transferStatus,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            TextButton(
                                text = "导出配置",
                                onClick = { transferViewModel.exportToClipboard() },
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            TextButton(
                                text = "从剪贴板导入",
                                onClick = { transferViewModel.importFromClipboard() },
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MultilineInputField(
                                value = importText,
                                onValueChange = { importText = it },
                                label = "粘贴配置 JSON / Cookie / Token 导入",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(12.dp))
                            TextButton(
                                text = "导入",
                                onClick = {
                                    transferViewModel.importFromText(importText)
                                    importText = ""
                                },
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        }
                    }

                    // ==================== 关于 ====================
                    Card(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                    ) {
                        ArrowPreference(
                            title = stringResource(id = R.string.about),
                            startAction = {
                                Icon(
                                    Icons.Rounded.Info,
                                    modifier = Modifier.padding(end = 6.dp),
                                    contentDescription = stringResource(id = R.string.about),
                                    tint = colorScheme.onBackground
                                )
                            },
                            onClick = actions.onOpenAbout,
                        )
                    }
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}
