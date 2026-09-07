package com.functy.fewards.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow
import kotlin.math.sign

/**
 * G2 连续曲率圆角（macOS / iOS 应用图标同款 squircle）。
 *
 * 实现为超椭圆（superellipse）：|x/a|^n + |y/b|^n = 1，n ≈ 5 即 Apple 图标遮罩轮廓，
 * 曲率全程连续（G2），与 macOS 系统圆角观感一致。采样 64 点/象限用三次贝塞尔平滑连接。
 */
class G2SquircleShape(private val exponent: Float = 5.0f) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(superellipsePath(size.width, size.height, exponent))

    companion object {
        fun superellipsePath(width: Float, height: Float, exponent: Float = 5.0f, samplesPerQuadrant: Int = 64): Path {
            val p = Path()
            if (width <= 0f || height <= 0f) {
                p.addRect(Rect(0f, 0f, width, height))
                return p
            }
            val a = width / 2f
            val b = height / 2f
            val n = exponent.toDouble()

            fun x(t: Double): Float {
                val v = sign(cos(t)) * abs(cos(t)).pow(2.0 / n)
                return (a + a * v).toFloat().coerceIn(0f, width)
            }

            fun y(t: Double): Float {
                val v = sign(sin(t)) * abs(sin(t)).pow(2.0 / n)
                return (b + b * v).toFloat().coerceIn(0f, height)
            }

            val steps = samplesPerQuadrant * 4
            p.moveTo(x(0.0), y(0.0))
            var prevX = x(0.0).toDouble()
            var prevY = y(0.0).toDouble()
            for (i in 1..steps) {
                val t = (i * 2 * Math.PI) / steps
                val nx = x(t).toDouble()
                val ny = y(t).toDouble()
                // 用中点法构造平滑三次贝塞尔（Catmull-Rom 转 Bezier）
                val tPrev = ((i - 1) * 2 * Math.PI) / steps
                val tNext = if (i + 1 <= steps) (i + 1) * 2 * Math.PI / steps else 0.0
                val px = x(tPrev).toDouble()
                val py = y(tPrev).toDouble()
                val qx = x(tNext).toDouble()
                val qy = y(tNext).toDouble()
                val c1x = prevX + (nx - px) / 6.0
                val c1y = prevY + (ny - py) / 6.0
                val c2x = nx - (qx - prevX) / 6.0
                val c2y = ny - (qy - prevY) / 6.0
                p.cubicTo(
                    c1x.toFloat(), c1y.toFloat(),
                    c2x.toFloat(), c2y.toFloat(),
                    nx.toFloat(), ny.toFloat(),
                )
                prevX = nx
                prevY = ny
            }
            p.close()
            return p
        }
    }
}

/** Squircle 图标：drawable png + G2 连续圆角裁剪（macOS 图标同款轮廓）。 */
@Composable
fun SquircleIcon(
    resId: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val shape = G2SquircleShape()
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Crop,
    )
}
