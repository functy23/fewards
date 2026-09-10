package com.functy.fewards.ui.screen.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.functy.fewards.ui.viewmodel.AccountViewModel

@Composable
fun AccountPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val accountViewModel = viewModel<AccountViewModel>()
    AccountPagerMiuix(accountViewModel, bottomInnerPadding)
}
