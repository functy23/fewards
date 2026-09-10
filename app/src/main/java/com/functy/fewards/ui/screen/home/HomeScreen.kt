package com.functy.fewards.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.LocalUiMode
import com.functy.fewards.ui.UiMode
import com.functy.fewards.ui.viewmodel.TaskRunner
import com.functy.fewards.ui.viewmodel.TaskViewModel

@Composable
fun HomePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val taskViewModel = viewModel<TaskViewModel>()
    LifecycleResumeEffect(Unit) {
        TaskRunner.refreshStatus()
        taskViewModel.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            TaskRunner.refreshStatus()
            taskViewModel.refresh()
        }
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(taskViewModel, bottomInnerPadding)
        UiMode.Material -> HomePagerMaterial(taskViewModel, bottomInnerPadding)
    }
}
