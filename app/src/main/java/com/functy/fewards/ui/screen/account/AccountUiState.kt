package com.functy.fewards.ui.screen.account

import androidx.compose.runtime.Immutable
import com.functy.fewards.data.repository.AccountRepository

/** 扫码登录的通用阶段：米游社与 WorkBuddy 共用同一套状态机。 */
enum class QrState { Idle, Loading, Waiting, Scanned, Confirmed, Expired, Error }

@Immutable
data class AccountUiState(
    // 米游社
    val mihoyoAccounts: List<AccountRepository.MihoyoAccount> = emptyList(),
    val mhyHydratingIds: Set<String> = emptySet(),
    val loginMode: Int = 0, // 0 扫码；1 Cookie
    val qrState: QrState = QrState.Idle,
    val qrContent: String = "",
    // WorkBuddy
    val wbAccounts: List<AccountRepository.WorkBuddyAccount> = emptyList(),
    val wbLoginMode: Int = 0, // 0 扫码；1 Token
    val wbQrState: QrState = QrState.Idle,
    val wbQrContent: String = "",
)

@Immutable
data class AccountActions(
    val onSetLoginMode: (Int) -> Unit,
    val onStartQr: () -> Unit,
    val onCancelQr: () -> Unit,
    /** 返回 true 表示凭据被接受（弹窗据此关闭）；false 保留输入让用户改。 */
    val onImportCookie: (String) -> Boolean,
    val onRemoveMihoyo: (String) -> Unit,
    /** 同上：返回 true 表示 token 被接受。 */
    val onImportWbToken: (String) -> Boolean,
    val onRemoveWb: (String) -> Unit,
    val onSetWbLoginMode: (Int) -> Unit,
    val onStartWbQr: () -> Unit,
    val onCancelWbQr: () -> Unit,
)
