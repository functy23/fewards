package com.functy.fewards.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 录制一层用于玻璃/模糊采样的 backdrop。
 *
 * 模糊没有开关了：它是 miuix-glass 的硬前提，关掉等于玻璃全失效。
 *
 * **不返回 null**：应用 minSdk 31，而 [isRenderEffectSupported] 就是 `SDK_INT >= 31`，
 * 所以这个判断在当前 minSdk 下恒真。之前返回可空类型是为了配合「模糊效果」开关，
 * 那个开关已删；现在保留一个恒真的判断只会让每个调用点都写一遍没意义的 `!= null`。
 * 真正的分档在更上一层：AGSL（API 33+）由 miuix-glass 内部各自再判，
 * 不支持时玻璃组件自己退回纯色 fallback。
 */
@Composable
fun rememberBlurBackdrop(): LayerBackdrop {
    // 颜色在组合里读、由 rememberLayerBackdrop 内部 rememberUpdatedState 转交，
    // 过渡期间每次重组都会换成新底色。
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = if (blurActive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = BlurColors(
                    blendColors = listOf(
                        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(0.87f)),
                    ),
                ),
            )
        } else {
            Modifier
        },
    ) {
        content()
    }
}
