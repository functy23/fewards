package com.functy.fewards.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.functy.fewards.R
import com.functy.fewards.ui.UiMode
import com.functy.fewards.ui.component.material.ExpressiveScaffold
import com.functy.fewards.ui.component.material.SegmentedColumn
import com.functy.fewards.ui.component.material.SegmentedDropdownItem
import com.functy.fewards.ui.component.material.SegmentedListItem
import com.functy.fewards.ui.component.material.SegmentedSwitchItem
import com.functy.fewards.ui.component.material.expressiveTopAppBarColors

@Composable
fun SettingPagerMaterial(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = { TopBar(scrollBehavior = scrollBehavior) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedDropdownItem(
                            icon = Icons.Filled.Shield,
                            title = stringResource(id = R.string.settings_ui_mode),
                            summary = stringResource(id = R.string.settings_ui_mode_summary),
                            items = UiMode.entries.map { it.name },
                            selectedIndex = if (uiState.uiMode == UiMode.Material.value) 1 else 0,
                            onItemSelected = actions.onSetUiModeIndex
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = actions.onOpenTheme,
                            headlineContent = { Text(stringResource(id = R.string.settings_theme)) },
                            supportingContent = { Text(stringResource(id = R.string.settings_theme_summary)) },
                            leadingContent = { Icon(Icons.Filled.Palette, stringResource(id = R.string.settings_theme)) },
                            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                        )
                    }
                )
            )

            // 米游社
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Shield,
                            title = stringResource(id = R.string.settings_mhy_master),
                            summary = stringResource(id = R.string.settings_mhy_master_summary),
                            checked = uiState.mhyMasterEnabled,
                            onCheckedChange = actions.onSetMhyMaster
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Gamepad,
                            title = stringResource(id = R.string.settings_mhy_game_sign),
                            summary = stringResource(id = R.string.settings_mhy_game_sign_summary),
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyGameSign,
                            onCheckedChange = actions.onSetMhyGameSign
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.WorkspacePremium,
                            title = stringResource(id = R.string.settings_mhy_bbs_sign),
                            summary = stringResource(id = R.string.settings_mhy_bbs_sign_summary),
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyBbsSign,
                            onCheckedChange = actions.onSetMhyBbsSign
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Visibility,
                            title = stringResource(id = R.string.settings_mhy_read),
                            summary = stringResource(id = R.string.settings_mhy_read_summary),
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyRead,
                            onCheckedChange = actions.onSetMhyRead
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.ThumbUp,
                            title = stringResource(id = R.string.settings_mhy_like),
                            summary = stringResource(id = R.string.settings_mhy_like_summary),
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyLike,
                            onCheckedChange = actions.onSetMhyLike
                        )
                    },
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Person,
                            title = stringResource(id = R.string.settings_mhy_share),
                            enabled = uiState.mhyMasterEnabled,
                            checked = uiState.mhyShare,
                            onCheckedChange = actions.onSetMhyShare
                        )
                    },
                )
            )

            // WorkBuddy
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Shield,
                            title = stringResource(id = R.string.settings_wb_master),
                            summary = stringResource(id = R.string.settings_wb_master_summary),
                            checked = uiState.wbMasterEnabled,
                            onCheckedChange = actions.onSetWbMaster
                        )
                    }
                )
            )

            // 通知
            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(id = R.string.settings_notification),
                            summary = stringResource(id = R.string.settings_notification_summary),
                            checked = uiState.taskNotification,
                            onCheckedChange = actions.onSetTaskNotification
                        )
                    }
                )
            )

            SegmentedColumn(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 13.dp),
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = actions.onOpenAbout,
                            headlineContent = { Text(stringResource(id = R.string.about)) },
                            leadingContent = { Icon(Icons.Filled.Info, stringResource(id = R.string.about)) },
                        )
                    }
                )
            )

            Spacer(modifier = Modifier.height(bottomInnerPadding))
        }
    }
}

@Composable
private fun TopBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}
