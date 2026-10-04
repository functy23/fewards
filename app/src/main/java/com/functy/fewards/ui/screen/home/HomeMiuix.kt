package com.functy.fewards.ui.screen.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import com.functy.fewards.ui.theme.isInDarkTheme
import com.functy.fewards.ui.theme.themeTransitionSpec
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
                    // 总开关关掉的任务整行不出现，也不参与「全部完成」判定。
                    val summary = state.summary()
                    StatusCard(
                        running = state.running,
                        allDone = summary.allDone,
                        hasAnyConfigured = summary.hasAnyConfigured,
                    )
                    TaskListCard(state = state, summary = summary)
                    if (summary.anyVisible) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TaskPickerCard(
                                state = state,
                                summary = summary,
                                onToggleWb = actions.onToggleWb,
                                onToggleMhy = actions.onToggleMhy,
                                modifier = Modifier.weight(1f),
                            )
                            ExecuteCard(
                                wbChecked = summary.runWb,
                                mhyChecked = summary.runMhy,
                                enabled = !state.running && summary.canRun,
                                running = state.running,
                                onRun = actions.onRun,
                                modifier = Modifier.weight(1f),
                            )
                        }
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
 * 状态大卡的语义色：深浅两套写死。
 *
 * 刻意不走 Miuix 的动态取色——状态色（蓝=进行中 / 绿=完成 / 红=未完成）要的是
 * 语义稳定，被用户取色染成紫色就失去意义了。但**必须**按明暗分两套：
 * 浅色那套是给白底调的，放到深色底上会亮得刺眼（这正是「深色下还是亮色样式」的原因）。
 */
private data class StatusCardColors(
    val container: Color,
    val accent: Color,
    val title: Color,
)

private val StatusCardRunningColorsLight = StatusCardColors(
    container = Color(0xFFE8F1FF),
    accent = Color(0xFF1A73E8),
    title = Color(0xFF0D47A1),
)

private val StatusCardRunningColorsDark = StatusCardColors(
    container = Color(0xFF1E2A38),
    accent = Color(0xFF7AA9F7),
    title = Color(0xFFBBD3FF),
)

private val StatusCardDoneColorsLight = StatusCardColors(
    container = Color(0xFFDFFAE4),
    accent = Color(0xFF36D167),
    title = Color(0xFF1A3825),
)

private val StatusCardDoneColorsDark = StatusCardColors(
    container = Color(0xFF1A3825),
    accent = Color(0xFF6BD68C),
    title = Color(0xFFB6EFC6),
)

private val StatusCardUndoneColorsLight = StatusCardColors(
    container = Color(0xFFFDEBEA),
    accent = Color(0xFFEA4335),
    title = Color(0xFF5C1A17),
)

private val StatusCardUndoneColorsDark = StatusCardColors(
    container = Color(0xFF3A1F1E),
    accent = Color(0xFFF2847A),
    title = Color(0xFFFFC6C1),
)

/**
 * KSU StatusCard 移植：状态大卡（对号=已完成 / 错号=未完成）。
 */
@Composable
private fun StatusCard(
    running: Boolean,
    allDone: Boolean,
    hasAnyConfigured: Boolean,
) {
    val done = allDone && hasAnyConfigured
    val dark = isInDarkTheme()
    val target = when {
        running -> if (dark) StatusCardRunningColorsDark else StatusCardRunningColorsLight
        done -> if (dark) StatusCardDoneColorsDark else StatusCardDoneColorsLight
        else -> if (dark) StatusCardUndoneColorsDark else StatusCardUndoneColorsLight
    }

    // 跟着主题过渡一起淡，别在整屏换色时硬切一下。
    val container by animateColorAsState(target.container, themeTransitionSpec(), label = "status_card_container")
    val accent by animateColorAsState(target.accent, themeTransitionSpec(), label = "status_card_accent")
    val titleColor by animateColorAsState(target.title, themeTransitionSpec(), label = "status_card_title")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container),
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
                    imageVector = when {
                        running -> Icons.Rounded.Sync
                        done -> Icons.Rounded.CheckCircleOutline
                        else -> Icons.Rounded.ErrorOutline
                    },
                    tint = accent,
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
                            when {
                                running -> R.string.home_tasks_running
                                done -> R.string.home_tasks_all_done
                                else -> R.string.home_tasks_not_done
                            }
                        ),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 15.sp,
                        color = titleColor,
                    )
                }
            }
        }
    }
}

/** 图标+名称+状态 列表卡（图标为各软件 squircle 图标）。 */
@Composable
private fun TaskListCard(state: HomeUiState, summary: HomeSummary) {
    if (!summary.anyVisible) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (summary.wbVisible) {
                TaskRow(
                    resId = R.drawable.workbuddy,
                    name = stringResource(R.string.workbuddy),
                    status = statusText(state.wbStatus),
                )
            }
            if (summary.mhyVisible) {
                TaskRow(
                    resId = R.drawable.miyoushe,
                    name = stringResource(R.string.miyoushe),
                    status = statusText(state.mhyStatus),
                )
            }
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
        SquircleIcon(resId = resId, size = 28.dp)
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
    summary: HomeSummary,
    onToggleWb: (Boolean) -> Unit,
    onToggleMhy: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp, 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (summary.wbVisible) {
                PickerRow(
                    resId = R.drawable.workbuddy,
                    name = stringResource(R.string.workbuddy),
                    checked = state.wbChecked,
                    onCheckedChange = onToggleWb,
                )
            }
            if (summary.wbVisible && summary.mhyVisible) {
                Spacer(Modifier.height(8.dp))
            }
            if (summary.mhyVisible) {
                PickerRow(
                    resId = R.drawable.miyoushe,
                    name = stringResource(R.string.miyoushe),
                    checked = state.mhyChecked,
                    onCheckedChange = onToggleMhy,
                )
            }
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
        SquircleIcon(resId = resId, size = 24.dp, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Checkbox(
            state = androidx.compose.ui.state.ToggleableState(checked),
            onClick = { onCheckedChange(!checked) },
            modifier = Modifier.scale(0.85f),
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
                // 可滚动日志区：高度随内容实时增长，超过 260dp 后内部滚动，自动吸底到最新一条
                val logScrollState = rememberScrollState()
                androidx.compose.runtime.LaunchedEffect(logs.size) {
                    if (logScrollState.maxValue > 0) {
                        logScrollState.animateScrollTo(logScrollState.maxValue)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .background(colorScheme.surfaceContainer.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .verticalScroll(logScrollState)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    logs.takeLast(200).forEach { line ->
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
