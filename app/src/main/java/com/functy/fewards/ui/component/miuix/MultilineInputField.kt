package com.functy.fewards.ui.component.miuix

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.TextField

/**
 * KSU SuperEditArrow 同款 miuix TextField 输入框：
 * value/onValueChange + label + 多行支持，用于 Cookie / Token 粘贴输入。
 * 与 KSU 设置页编辑框完全一致的组件（top.yukonga.miuix.kmp.basic.TextField）。
 */
@Composable
fun MultilineInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    maxLines: Int = 4,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        maxLines = maxLines,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
    )
}
