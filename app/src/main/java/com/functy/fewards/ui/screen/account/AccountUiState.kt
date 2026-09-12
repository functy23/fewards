package com.functy.fewards.ui.screen.account

import androidx.compose.runtime.Immutable
import com.functy.fewards.data.repository.AccountRepository

@Immutable
data class AccountUiState(
    // 米游社
    val mihoyoLoggedIn: Boolean = false,
    val mihoyoAccounts: List<AccountRepository.MihoyoAccount> = emptyList(),
    val mhyHydratingIds: Set<String> = emptySet(),
    val loginMode: Int = 0, // 0 扫码；1 Cookie
    val qrState: QrState = QrState.Idle,
    val qrContent: String = "",
    // WorkBuddy
    val wbLoggedIn: Boolean = false,
    val wbAccounts: List<AccountRepository.WorkBuddyAccount> = emptyList(),
    val wbBusy: Boolean = false,
) {
    enum class QrState { Idle, Loading, Waiting, Scanned, Confirmed, Expired, Error }
}

@Immutable
data class AccountActions(
    val onSetLoginMode: (Int) -> Unit,
    val onStartQr: () -> Unit,
    val onCancelQr: () -> Unit,
    val onImportCookie: (String) -> Unit,
    val onRemoveMihoyo: (String) -> Unit,
    val onImportWbToken: (String) -> Unit,
    val onRemoveWb: (String) -> Unit,
)
