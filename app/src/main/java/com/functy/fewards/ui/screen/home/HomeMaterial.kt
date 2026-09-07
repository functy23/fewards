package com.functy.fewards.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import com.functy.fewards.R
import com.functy.fewards.ui.component.SquircleIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.functy.fewards.core.AppLog
import com.functy.fewards.ui.component.material.SegmentedColumn
import com.functy.fewards.ui.component.material.SegmentedListItem
import com.functy.fewards.ui.component.material.TonalCard
import com.functy.fewards.ui.component.material.expressiveTopAppBarColors
import com.functy.fewards.ui.viewmodel.TaskRunner
import com.functy.fewards.ui.viewmodel.TaskViewModel

@Composable
fun HomePagerMaterial(
    taskViewModel: TaskViewModel,
    bottomInnerPadding: androidx.compose.ui.unit.Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val state by taskViewModel.uiState.collectAsStateWithLifecycle()
    val logs by AppLog.logs.collectAsStateWithLifecycle()
    val actions = taskViewModel.homeActions

    Column(
        modifier = Modifier
            .padding(bottom = bottomInnerPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        TopBar(scrollBehavior = scrollBehavior)
        StatusCardMaterial(
            allDone = state.wbStatus == TaskRunner.TaskStatus.DONE && state.mhyStatus == TaskRunner.TaskStatus.DONE,
            hasAnyConfigured = state.wbStatus != TaskRunner.TaskStatus.UNCONFIGURED ||
                state.mhyStatus != TaskRunner.TaskStatus.UNCONFIGURED,
        )
        TaskListCardMaterial(state)
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            TonalCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.workbuddy)) },
                        leadingContent = { SquircleIcon(resId = R.drawable.workbuddy, size = 24.dp) },
                        trailingContent = {
                            Checkbox(checked = state.wbChecked, onCheckedChange = actions.onToggleWb)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.miyoushe)) },
                        leadingContent = { SquircleIcon(resId = R.drawable.miyoushe, size = 24.dp) },
                        trailingContent = {
                            Checkbox(checked = state.mhyChecked, onCheckedChange = actions.onToggleMhy)
                        },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    )
                }
            }
            TonalCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(16.dp, 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.home_will_run_checked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    val haptic = LocalHapticFeedback.current
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onRun(state.wbChecked, state.mhyChecked)
                        },
                        enabled = !state.running && (state.wbChecked || state.mhyChecked),
                    ) {
                        Text(stringResource(if (state.running) R.string.home_running else R.string.home_start_execute))
                    }
                }
            }
        }
        TonalCard {
            Column(modifier = Modifier.padding(16.dp, 12.dp)) {
                Text(
                    text = stringResource(R.string.home_log_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                if (logs.isEmpty()) {
                    Text(
                        text = stringResource(R.string.home_no_logs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val logScrollState = androidx.compose.foundation.rememberScrollState()
                    androidx.compose.runtime.LaunchedEffect(logs.size) {
                        if (logScrollState.maxValue > 0) logScrollState.animateScrollTo(logScrollState.maxValue)
                    }
                    Column(
                        modifier = Modifier
                            .heightIn(max = 260.dp)
                            .verticalScroll(logScrollState)
                    ) {
                        logs.takeLast(200).forEach { entry ->
                            Text(
                                text = entry.format(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        SupportLinksMaterial(uriHandler)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopBar(scrollBehavior: TopAppBarScrollBehavior? = null) {
    LargeFlexibleTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = expressiveTopAppBarColors(),
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun StatusCardMaterial(allDone: Boolean, hasAnyConfigured: Boolean) {
    val done = allDone && hasAnyConfigured
    val containerColor = if (done) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val statusIcon = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Error
    TonalCard(containerColor = containerColor) {
        ListItem(
            headlineContent = {
                Text(
                    stringResource(if (done) R.string.home_tasks_all_done else R.string.home_tasks_not_done),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = { Text(stringResource(R.string.app_name)) },
            leadingContent = { Icon(statusIcon, null, Modifier.size(42.dp)) },
            colors = ListItemDefaults.colors(containerColor = containerColor),
        )
    }
}

@Composable
private fun TaskListCardMaterial(state: HomeUiState) {
    TonalCard {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.workbuddy)) },
                trailingContent = { Text(statusText(state.wbStatus)) },
                leadingContent = { SquircleIcon(resId = R.drawable.workbuddy, size = 32.dp) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.miyoushe)) },
                trailingContent = { Text(statusText(state.mhyStatus)) },
                leadingContent = { SquircleIcon(resId = R.drawable.miyoushe, size = 32.dp) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        }
    }
}

private fun statusText(status: TaskRunner.TaskStatus): String = when (status) {
    TaskRunner.TaskStatus.UNCONFIGURED -> "未配置"
    TaskRunner.TaskStatus.QUERYING -> "执行中"
    TaskRunner.TaskStatus.DONE -> "已完成"
    TaskRunner.TaskStatus.NOT_DONE -> "未完成"
}

@Composable
private fun SupportLinksMaterial(uriHandler: androidx.compose.ui.platform.UriHandler) {
    TonalCard {
        SegmentedColumn {
            item {
                SegmentedListItem(
                    onClick = { uriHandler.openUri("https://github.com/functy23/fewards") },
                    headlineContent = { Text(stringResource(R.string.home_support_title)) },
                    supportingContent = { Text(stringResource(R.string.home_support_content)) },
                    leadingContent = { Icon(Icons.Filled.VolunteerActivism, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            }
            item {
                SegmentedListItem(
                    onClick = { uriHandler.openUri("https://github.com/functy23/fewards/blob/main/README.md") },
                    headlineContent = { Text(stringResource(R.string.home_learn_more)) },
                    supportingContent = { Text(stringResource(R.string.home_learn_more_content)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.MenuBook, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
            }
        }
    }
}
