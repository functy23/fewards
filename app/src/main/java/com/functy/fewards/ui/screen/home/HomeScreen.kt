package com.functy.fewards.ui.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.LocalUiMode
import com.functy.fewards.ui.viewmodel.TaskViewModel
import com.functy.fewards.ui.UiMode

@Composable
fun HomePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val taskViewModel = viewModel<TaskViewModel>()
    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(taskViewModel, bottomInnerPadding)
        UiMode.Material -> HomePagerMaterial(taskViewModel, bottomInnerPadding)
    }
}
