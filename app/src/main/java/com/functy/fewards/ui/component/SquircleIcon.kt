package com.functy.fewards.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import top.yukonga.miuix.kmp.squircle.squircleClip

/** 图标 / 头像圆角：半径 = size * 0.30，走 miuix squircleClip。不要自写 G2 贝塞尔。 */
internal const val IconSquircleCornerFraction = 0.30f

@Composable
fun SquircleIcon(
    resId: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerFraction: Float = IconSquircleCornerFraction,
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .squircleClip(cornerRadius = size * cornerFraction),
        contentScale = ContentScale.Fit,
    )
}
