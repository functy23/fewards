package com.functy.fewards.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.functy.fewards.R
import com.functy.fewards.core.AppLog
import com.functy.fewards.ui.component.SquircleIcon
import com.functy.fewards.ui.viewmodel.TaskRunner
import com.functy.fewards.ui.viewmodel.TaskViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    taskViewModel: TaskViewModel,
    bottomInnerPadding: androidx.compose.ui.unit.Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val state by taskViewModel.uiState.collectAsStateWithLifecycle()
    val logs by AppLog.logs.collectAsStateWithLifecycle()
    val actions = taskViewModel.homeActions

    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = stringResource(R.string.app_name),
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatusCard(
                        allDone = state.wbStatus == TaskRunner.TaskStatus.DONE &&
                            state.mhyStatus == TaskRunner.TaskStatus.DONE,
                        hasAnyConfigured = state.wbStatus != TaskRunner.TaskStatus.UNCONFIGURED ||
                            state.mhyStatus != TaskRunner.TaskStatus.UNCONFIGURED,
                    )
                    TaskListCard(state = state)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TaskPickerCard(
                            state = state,
                            onToggleWb = actions.onToggleWb,
                            onToggleMhy = actions.onToggleMhy,
                            modifier = Modifier.weight(1f),
                        )
                        ExecuteCard(
                            wbChecked = state.wbChecked,
                            mhyChecked = state.mhyChecked,
                            enabled = !state.running && (state.wbChecked || state.mhyChecked),
                            running = state.running,
                            onRun = actions.onRun,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    LogCard(
                        logs = logs.map { it.format() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SupportLinks(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

/**
 * KSU StatusCard 移植：状态大卡（对号=已完成 / 错号=未完成）。
 */
@Composable
private fun StatusCard(
    allDone: Boolean,
    hasAnyConfigured: Boolean,
) {
    val done = allDone && hasAnyConfigured
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = if (done) Color(0xFFDFFAE4) else Color(0xFFFDEBEA)
        ),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = if (done) Icons.Rounded.CheckCircleOutline else Icons.Rounded.ErrorOutline,
                    tint = if (done) Color(0xFF36D167) else Color(0xFFEA4335),
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 14.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                Column {
                    Text(
                        text = stringResource(
                            if (done) R.string.home_tasks_all_done else R.string.home_tasks_not_done
                        ),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (done) Color(0xFF1A3825) else Color(0xFF5C1A17),
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 15.sp,
                        color = if (done) Color(0xFF1A3825) else Color(0xFF5C1A17),
                    )
                }
            }
        }
    }
}

/** 图标+名称+状态 列表卡（图标为各软件 squircle 图标）。 */
@Composable
private fun TaskListCard(state: HomeUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskRow(
                resId = R.drawable.workbuddy,
                name = stringResource(R.string.workbuddy),
                status = statusText(state.wbStatus),
            )
            TaskRow(
                resId = R.drawable.miyoushe,
                name = stringResource(R.string.miyoushe),
                status = statusText(state.mhyStatus),
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
private fun TaskRow(
    resId: Int,
    name: String,
    status: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleIcon(resId = resId, size = 32.dp)
        Spacer(Modifier.size(12.dp))
        Text(
            text = name,
            fontSize = MiuixTheme.textStyles.headline1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = status,
            fontSize = MiuixTheme.textStyles.body2.fontSize,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 左侧：两个复选框行（squircle 图标 + 名字 + 右侧复选框，固定间距防重叠）。 */
@Composable
private fun TaskPickerCard(
    state: HomeUiState,
    onToggleWb: (Boolean) -> Unit,
    onToggleMhy: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(modifier = Modifier.padding(12.dp, 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PickerRow(
                resId = R.drawable.workbuddy,
                name = stringResource(R.string.workbuddy),
                checked = state.wbChecked,
                onCheckedChange = onToggleWb,
            )
            PickerRow(
                resId = R.drawable.miyoushe,
                name = stringResource(R.string.miyoushe),
                checked = state.mhyChecked,
                onCheckedChange = onToggleMhy,
            )
        }
    }
}

@Composable
private fun PickerRow(
    resId: Int,
    name: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquircleIcon(resId = resId, size = 22.dp, modifier = Modifier.padding(end = 6.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Checkbox(
            state = androidx.compose.ui.state.ToggleableState(checked),
            onClick = { onCheckedChange(!checked) },
        )
    }
}

/** 右侧：提示文字 + 开始执行按钮（点击震动）。 */
@Composable
private fun ExecuteCard(
    wbChecked: Boolean,
    mhyChecked: Boolean,
    enabled: Boolean,
    running: Boolean,
    onRun: (Boolean, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp, 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_will_run_checked),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(
                text = stringResource(if (running) R.string.home_running else R.string.home_start_execute),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRun(wbChecked, mhyChecked)
                },
                enabled = enabled,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 日志框：高度随内容自适应（上限 260dp），实时更新。 */
@Composable
private fun LogCard(
    logs: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp, 12.dp)) {
            Text(
                text = stringResource(R.string.home_log_title),
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            if (logs.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_no_logs),
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .background(colorScheme.surfaceContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    logs.takeLast(30).forEach { line ->
                        Text(
                            text = line,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** KSU SupportLinks 原样，链接指向 Fewards GitHub 仓库。 */
@Composable
private fun SupportLinks(modifier: Modifier = Modifier) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = stringResource(R.string.home_support_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { uriHandler.openUri("https://github.com/functy23/fewards") },
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_more),
            summary = stringResource(R.string.home_learn_more_content),
            startAction = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.home_learn_more),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { uriHandler.openUri("https://github.com/functy23/fewards/blob/main/README.md") },
        )
    }
}
