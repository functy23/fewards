package com.functy.fewards.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.core.AppLog
import com.functy.fewards.core.mihoyo.MihoyoApi
import com.functy.fewards.core.mihoyo.MihoyoProfileHydrator
import com.functy.fewards.core.workbuddy.WorkBuddyLabel
import com.functy.fewards.core.workbuddy.WorkBuddyLogin
import com.functy.fewards.data.repository.AccountRepository
import com.functy.fewards.fewardsApp
import com.functy.fewards.ui.screen.account.AccountActions
import com.functy.fewards.ui.screen.account.AccountUiState
import com.functy.fewards.ui.screen.account.QrState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 账号页 ViewModel：米游社扫码 / Cookie 登录，WorkBuddy 扫码 / Token 登录。
 * 两套扫码流程状态机一致（QrState），只是协议客户端不同。
 */
class AccountViewModel : ViewModel() {

    private val accounts = AccountRepository()
    private val api = MihoyoApi(MihoyoApi.defaultClient())
    private val wbLogin = WorkBuddyLogin(fewardsApp.okhttpClient)

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    private var qrJob: Job? = null
    private var wbQrJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        publishAccounts()
        hydrateMissingProfiles()
    }

    private fun publishAccounts() {
        val mhy = accounts.mihoyoAccounts()
        val wb = accounts.workBuddyAccounts()
        _uiState.update { it.copy(mihoyoAccounts = mhy, wbAccounts = wb) }
    }

    private fun hydrateMissingProfiles() {
        accounts.mihoyoAccounts()
            .filter { MihoyoProfileHydrator.needsHydration(it) }
            .forEach { acc ->
                if (!MihoyoProfileHydrator.markAttempted(acc.id)) return@forEach
                _uiState.update { it.copy(mhyHydratingIds = it.mhyHydratingIds + acc.id) }
                viewModelScope.launch {
                    try {
                        val next = MihoyoProfileHydrator.hydrate(api, acc)
                        if (next != acc) accounts.addMihoyoAccount(next)
                    } finally {
                        _uiState.update { it.copy(mhyHydratingIds = it.mhyHydratingIds - acc.id) }
                        publishAccounts()
                    }
                }
            }
    }

    fun setLoginMode(mode: Int) {
        _uiState.update { it.copy(loginMode = mode) }
    }

    // ==================== 米游社扫码 ====================

    /** 生成二维码并轮询扫码状态。 */
    fun startQrLogin() {
        qrJob?.cancel()
        _uiState.update { it.copy(qrState = QrState.Loading) }
        qrJob = viewModelScope.launch {
            try {
                val session = api.createQrLogin()
                _uiState.update { it.copy(qrState = QrState.Waiting, qrContent = session.url) }
                val result = api.awaitQrLogin(session) { status ->
                    when (status) {
                        is MihoyoApi.QrStatus.Scanned ->
                            _uiState.update { s -> s.copy(qrState = QrState.Scanned) }
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
                _uiState.update { it.copy(qrState = QrState.Confirmed) }
                refresh()
            } catch (t: Throwable) {
                AppLog.e("MHY", "扫码登录失败: ${t.message}")
                _uiState.update {
                    it.copy(
                        qrState = if (t.message?.contains("过期") == true)
                            QrState.Expired else QrState.Error
                    )
                }
            }
        }
    }

    fun cancelQr() {
        qrJob?.cancel()
        qrJob = null
        _uiState.update { it.copy(qrState = QrState.Idle) }
    }

    /** 返回 false 表示 cookie 不可解析，弹窗保留输入让用户改。 */
    fun importCookie(cookie: String): Boolean {
        val account = MihoyoApi.parseCookie(cookie.trim())
        if (account == null) {
            AppLog.e("MHY", "Cookie 无效：缺少 stoken 或 uid")
            return false
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
        return true
    }

    fun removeMihoyo(id: String) {
        accounts.removeMihoyoAccount(id)
        refresh()
    }

    // ==================== WorkBuddy ====================

    fun setWbLoginMode(mode: Int) {
        _uiState.update { it.copy(wbLoginMode = mode) }
    }

    /**
     * WorkBuddy 扫码授权：申请授权链接 -> 轮询 -> 落库（按 uid 去重）。
     * 扫码成功后只纳管，不自动签到；签到仍由首页「开始执行」统一触发。
     */
    fun startWbQrLogin() {
        wbQrJob?.cancel()
        _uiState.update { it.copy(wbQrState = QrState.Loading, wbQrContent = "") }
        wbQrJob = viewModelScope.launch {
            try {
                val session = wbLogin.startLogin()
                _uiState.update { it.copy(wbQrState = QrState.Waiting, wbQrContent = session.authUrl) }
                val result = wbLogin.awaitLogin(session)
                val existed = accounts.workBuddyAccounts().any {
                    it.uid == result.uid || (it.uid.isEmpty() && it.label == result.nickname)
                }
                val profile = WorkBuddyLabel.decodeProfile(result.accessToken)
                accounts.addWorkBuddyAccount(
                    AccountRepository.WorkBuddyAccount(
                        id = "wb_${result.uid}",
                        label = result.nickname.ifEmpty { profile.label ?: "Work Buddy 账号" },
                        token = result.accessToken,
                        avatarUrl = profile.avatarUrl.orEmpty(),
                        uid = result.uid,
                        enterpriseId = result.enterpriseId,
                        refreshToken = result.refreshToken,
                        expiresAt = result.expiresAt,
                    )
                )
                AppLog.i("WB", "账号 ${result.nickname} 扫码授权成功${if (existed) "（已更新）" else ""}")
                _uiState.update { it.copy(wbQrState = QrState.Confirmed) }
                publishAccounts()
            } catch (t: Throwable) {
                AppLog.e("WB", "扫码授权失败: ${t.message}")
                _uiState.update {
                    it.copy(
                        wbQrState = if (t.message?.contains("过期") == true) QrState.Expired else QrState.Error,
                    )
                }
            }
        }
    }

    fun cancelWbQr() {
        wbQrJob?.cancel()
        wbQrJob = null
        _uiState.update { it.copy(wbQrState = QrState.Idle) }
    }

    /** 返回 false 表示 token 为空，弹窗保留输入让用户改。 */
    fun importWbToken(token: String): Boolean {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            AppLog.e("WB", "token 为空")
            return false
        }
        val profile = WorkBuddyLabel.decodeProfile(trimmed)
        val uid = profile.uid
        // 老账号（扫码前导入的）没有 uid，退回按昵称判定，避免同一账号出现两条
        val existed = accounts.workBuddyAccounts().any {
            (uid.isNotEmpty() && it.uid == uid) ||
                (it.uid.isEmpty() && profile.label != null && it.label == profile.label)
        }
        accounts.addWorkBuddyAccount(
            AccountRepository.WorkBuddyAccount(
                // 同账号重复导入 = 更新，不再新增一条
                id = if (uid.isNotEmpty()) "wb_$uid" else "wb_" + System.currentTimeMillis(),
                label = profile.label ?: "Work Buddy 账号",
                token = trimmed,
                avatarUrl = profile.avatarUrl.orEmpty(),
                uid = uid,
                expiresAt = profile.expiresAt,
            )
        )
        AppLog.i("WB", "WorkBuddy token 导入成功")
        refresh()
        return true
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
        onSetWbLoginMode = ::setWbLoginMode,
        onStartWbQr = ::startWbQrLogin,
        onCancelWbQr = ::cancelWbQr,
    )
}
