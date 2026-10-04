package com.functy.fewards.ui.screen.account

import androidx.compose.runtime.Immutable
import com.functy.fewards.data.repository.AccountRepository

/**
 * 登录流程的通用阶段：米游社扫码、WorkBuddy 扫码、Bing 的 WebView 授权共用。
 *
 * Bing 没有二维码，但阶段语义一一对应：
 * Waiting = 授权页已展示、等待用户登录；Loading = 正在换 token。
 */
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
    // Bing
    val bingAccounts: List<AccountRepository.BingAccount> = emptyList(),
    val bingLoginMode: Int = 0, // 0 微软登录（WebView）；1 refresh_token
    /** 授权页地址（含一次性 state）。空串表示还没生成。 */
    val bingAuthUrl: String = "",
    /** 本次授权的一次性 state，回跳时校验，防串号。 */
    val bingAuthCsrf: String = "",
    val bingAuthPhase: QrState = QrState.Idle,
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
    // Bing
    val onSetBingLoginMode: (Int) -> Unit,
    /** 生成授权页并进入等待登录。 */
    val onStartBingAuth: () -> Unit,
    val onCancelBingAuth: () -> Unit,
    /** WebView 拦到回跳地址时调用；返回 true 表示已消费该回跳。 */
    val onBingRedirect: (String) -> Boolean,
    /** 返回 true 表示凭据被接受（弹窗据此关闭）。 */
    val onImportBingToken: (String) -> Boolean,
    val onRemoveBing: (String) -> Unit,
)
