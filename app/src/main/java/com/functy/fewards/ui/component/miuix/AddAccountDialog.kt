package com.functy.fewards.ui.component.miuix

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.functy.fewards.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 「添加账号」弹窗（米游社 / WorkBuddy 共用同一套控件）。
 *
 * 弹窗内用 TabRowWithContour 切换登录方式：
 *  - 扫码 Tab：显示二维码（[qrContent] 为空时显示加载圈），带复制链接 / 浏览器打开 / 重新获取；
 *  - 凭据 Tab：一个输入框 + 导入按钮（米游社 = Cookie，WorkBuddy = Access Token）。
 *
 * 状态由调用方驱动；关闭走 WindowDialog 自带下滑淡出动画，
 * 动画结束后才回调 [onDismissFinished]（调用方在那里停轮询）。
 */
@Composable
fun AddAccountDialog(
    show: Boolean,
    title: String,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    qrContent: String,
    qrMessage: String,
    canRetry: Boolean,
    credentialLabel: String,
    credentialAction: String,
    /** 返回 true 表示凭据被接受，弹窗自动关闭；false 保留输入。 */
    onImportCredential: (String) -> Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    onRetry: () -> Unit,
) {
    var credential by rememberSaveable { mutableStateOf("") }
    // 关闭后清空输入，避免下次打开残留上一次的 Cookie / Token
    LaunchedEffect(show) {
        if (!show) credential = ""
    }

    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TabRowWithContour(
                    tabs = tabs,
                    selectedTabIndex = selectedTab,
                    onTabSelected = onTabSelected,
                )
                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    QrSection(
                        content = qrContent,
                        message = qrMessage,
                        canRetry = canRetry,
                        onRetry = onRetry,
                    )
                } else {
                    CredentialSection(
                        value = credential,
                        onValueChange = { credential = it },
                        label = credentialLabel,
                        action = credentialAction,
                        onImport = {
                            // 只有被接受才清空 + 交给调用方关闭；失败时保留输入让用户改
                            if (onImportCredential(credential)) credential = ""
                        },
                    )
                }

                Spacer(Modifier.height(14.dp))
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun QrSection(
    content: String,
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 固定 240dp 二维码区，避免各阶段弹窗高度跳动
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (content.isNotEmpty()) {
                QrImage(content = content)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    InfiniteProgressIndicator(color = colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.processing),
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )

        if (canRetry) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                text = stringResource(R.string.qr_retry),
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CredentialSection(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    action: String,
    onImport: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            singleLine = true,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            text = action,
            onClick = onImport,
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}

/** zxing 生成二维码位图。 */
@Composable
private fun QrImage(content: String) {
    if (content.isEmpty()) return
    val bitmap = remember(content) {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, 512, 512,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        val bmp = android.graphics.Bitmap.createBitmap(
            matrix.width, matrix.height, android.graphics.Bitmap.Config.ARGB_8888
        )
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp.asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = "QR",
        modifier = Modifier
            .size(240.dp)
            .clip(RoundedCornerShape(20.dp)),
    )
}
