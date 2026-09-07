package com.functy.fewards.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.functy.fewards.core.AppLog
import com.functy.fewards.data.repository.ConfigTransferRepository
import com.functy.fewards.fewardsApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 配置导入/导出 ViewModel：导出到剪贴板 / 从剪贴板或粘贴文本导入。
 */
class ConfigTransferViewModel : ViewModel() {

    private val transfer = ConfigTransferRepository()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    /** 导出配置 JSON 并复制到剪贴板。 */
    fun exportToClipboard() {
        viewModelScope.launch {
            val json = transfer.export()
            val cm = fewardsApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("fewards-config", json))
            _status.value = "配置已复制到剪贴板（含敏感凭据，请妥善保管）"
            AppLog.i("SYS", "配置导出到剪贴板成功")
        }
    }

    /** 从剪贴板导入配置。 */
    fun importFromClipboard() {
        viewModelScope.launch {
            val cm = fewardsApp.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (text.isEmpty()) {
                _status.value = "剪贴板为空"
                return@launch
            }
            val result = transfer.import(text)
            _status.value = buildString {
                append("导入完成：米游社 ${result.mihoyo} 个，WorkBuddy ${result.workbuddy} 个")
                result.errors.forEach { append("；$it") }
            }
        }
    }

    /** 从粘贴文本导入。 */
    fun importFromText(text: String) {
        viewModelScope.launch {
            if (text.isEmpty()) {
                _status.value = "内容为空"
                return@launch
            }
            val result = transfer.import(text)
            _status.value = buildString {
                append("导入完成：米游社 ${result.mihoyo} 个，WorkBuddy ${result.workbuddy} 个")
                result.errors.forEach { append("；$it") }
            }
        }
    }

    fun consumeStatus() {
        _status.value = ""
    }
}
