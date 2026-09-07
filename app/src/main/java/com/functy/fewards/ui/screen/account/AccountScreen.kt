package com.functy.fewards.ui.screen.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.LocalUiMode
import com.functy.fewards.ui.UiMode
import com.functy.fewards.ui.viewmodel.AccountViewModel

@Composable
fun AccountPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val accountViewModel = viewModel<AccountViewModel>()
    when (LocalUiMode.current) {
        UiMode.Miuix -> AccountPagerMiuix(accountViewModel, bottomInnerPadding)
        UiMode.Material -> AccountPagerMaterial(accountViewModel, bottomInnerPadding)
    }
}
