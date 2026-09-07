package com.functy.fewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.core.AppLog
import com.functy.fewards.core.mihoyo.MihoyoApi
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.ui.screen.account.AccountActions
import com.functy.fewards.ui.screen.account.AccountUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 账号页 ViewModel：米游社扫码 / Cookie 登录，WorkBuddy token 导入。
 */
class AccountViewModel : ViewModel() {

    private val accounts = AccountRepository()
    private val api = MihoyoApi(MihoyoApi.defaultClient())

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var qrJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        val mhy = accounts.mihoyoAccounts()
        val wb = accounts.workBuddyAccounts()
        _uiState.update {
            it.copy(
                mihoyoLoggedIn = mhy.isNotEmpty(),
                mihoyoAccounts = mhy,
                wbLoggedIn = wb.isNotEmpty(),
                wbAccounts = wb,
            )
        }
    }

    fun setLoginMode(mode: Int) {
        _uiState.update { it.copy(loginMode = mode) }
    }

    /** 生成二维码并轮询扫码状态。 */
    fun startQrLogin() {
        qrJob?.cancel()
        _uiState.update { it.copy(qrState = AccountUiState.QrState.Loading) }
        qrJob = viewModelScope.launch {
            try {
                val session = api.createQrLogin()
                _uiState.update { it.copy(qrState = AccountUiState.QrState.Waiting, qrContent = session.url) }
                val result = api.awaitQrLogin(session) { status ->
                    when (status) {
                        is MihoyoApi.QrStatus.Scanned ->
                            _uiState.update { s -> s.copy(qrState = AccountUiState.QrState.Scanned) }
                        else -> {}
                    }
                }
                val account = AccountRepository.MihoyoAccount(
                    id = "mhy_${result.mid}",
                    nickname = result.nickname,
                    stoken = result.stoken,
                    stuid = result.stuid,
                    mid = result.mid,
                    cookie = "account_id=${result.stuid}; account_id_v2=${result.stuid}; account_mid_v2=${result.mid}; ltoken=; luid=${result.stuid}; ltmid_v2=${result.mid}; stuid=${result.stuid}; stoken=${result.stoken}; mid=${result.mid}",
                )
                // 验证 stoken（老接口）
                val valid = api.validateStoken(account)
                if (!valid) {
                    AppLog.w("MHY", "stoken 验证失败，仍已保存（可能需要稍后重试）")
                }
                accounts.addMihoyoAccount(account)
                AppLog.i("MHY", "账号 ${result.nickname} 扫码登录成功")
                _uiState.update { it.copy(qrState = AccountUiState.QrState.Confirmed) }
                refresh()
            } catch (t: Throwable) {
                AppLog.e("MHY", "扫码登录失败: ${t.message}")
                _uiState.update {
                    it.copy(
                        qrState = if (t.message?.contains("过期") == true)
                            AccountUiState.QrState.Expired else AccountUiState.QrState.Error
                    )
                }
            }
        }
    }

    fun cancelQr() {
        qrJob?.cancel()
        qrJob = null
        _uiState.update { it.copy(qrState = AccountUiState.QrState.Idle) }
    }

    fun importCookie(cookie: String) {
        val account = MihoyoApi.parseCookie(cookie.trim())
        if (account == null) {
            AppLog.e("MHY", "Cookie 无效：缺少 stoken 或 uid")
            return
        }
        viewModelScope.launch {
            val valid = api.validateStoken(account)
            if (!valid) {
                AppLog.w("MHY", "stoken 验证未通过，仍已保存")
            }
            accounts.addMihoyoAccount(account)
            AppLog.i("MHY", "账号 ${account.nickname} Cookie 导入成功")
            refresh()
        }
    }

    fun removeMihoyo(id: String) {
        accounts.removeMihoyoAccount(id)
        refresh()
    }

    fun importWbToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            AppLog.e("WB", "token 为空")
            return
        }
        accounts.addWorkBuddyAccount(
            AccountRepository.WorkBuddyAccount(
                id = "wb_${System.currentTimeMillis()}",
                label = "WorkBuddy 账号",
                token = trimmed,
            )
        )
        AppLog.i("WB", "WorkBuddy token 导入成功")
        refresh()
    }

    fun removeWb(id: String) {
        accounts.removeWorkBuddyAccount(id)
        refresh()
    }

    val accountActions = AccountActions(
        onSetLoginMode = ::setLoginMode,
        onStartQr = ::startQrLogin,
        onCancelQr = ::cancelQr,
        onImportCookie = ::importCookie,
        onRemoveMihoyo = ::removeMihoyo,
        onImportWbToken = ::importWbToken,
        onRemoveWb = ::removeWb,
    )
}
