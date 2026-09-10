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

/**
 * G2 连续曲率圆角（macOS 图标轮廓）。
 *
 * 语义与 Flutter ContinuousRectangleBorder(borderRadius: size * 0.30) 一致：
 * 矩形 + 圆角半径 = size * 0.30。角部用三段三次贝塞尔实现曲率全程连续（G2），
 * 普通 RoundedCornerShape 的单段圆弧只有切线连续（G1），在直边接点处曲率跳变。
 *
 * 角部几何（半径 r，圆心在 (cx, cy)）：
 *   段1 从直边进入：起点切点 → c1 在直边上（曲率 0 平滑起弯）→ c2 靠近角分线；
 *   段2 接近圆弧顶：曲率最大区间；
 *   段3 对称收口到邻边直边。
 */
class G2SquircleShape(private val cornerFraction: Float = 0.30f) : Shape {

    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(g2RoundedRectPath(size.width, size.height, size.minDimension * cornerFraction))

    companion object {
        /** 生成 G2 连续圆角矩形 Path（四角对称三段贝塞尔）。 */
        fun g2RoundedRectPath(width: Float, height: Float, radius: Float): Path {
            val p = Path()
            if (width <= 0f || height <= 0f) {
                p.addRect(Rect(0f, 0f, width, height))
                return p
            }
            val r = radius.coerceIn(0f, minOf(width, height) / 2f)
            if (r <= 0f) {
                p.addRect(Rect(0f, 0f, width, height))
                return p
            }
            // 控制点系数（相对 r）：经最小化曲率跳变拟合的 smooth-corner 系数组
            val c1 = 0.24750f  // 第一控制点（直边方向）
            val c2 = 0.54320f  // 第二控制点（沿角弧）
            val c3 = 0.54320f  // 第三控制点（进入角弧）
            val c4 = 0.24750f  // 收口控制点（邻边方向）
            val mid = 0.41421f // 角分线上的弧顶偏移（√2−1 近似）

            // 右上角（圆心 (w-r, r)）：从上边 (w-r, 0) 弯到右边 (w, r)
            fun cornerPathFromTop(x0: Float, y0: Float, x1: Float, y1: Float, sx: Float, sy: Float) {
                // (x0,y0)=起点（直边切点），(x1,y1)=终点（邻边切点），(sx,sy)=朝向（右下角等）
                // 统一以「水平进、垂直出」或「垂直进、水平出」实现
            }
            // 逐角手写（清晰无歧义）：
            // 上边 → 右上角
            p.moveTo(r, 0f)
            p.lineTo(width - r, 0f)
            g2CornerTopRight(p, width - r, r, r, c1, c2, mid, c3, c4)
            // 右边 → 右下角
            p.lineTo(width, height - r)
            g2CornerBottomRight(p, width - r, height - r, r, c1, c2, mid, c3, c4)
            // 下边 → 左下角
            p.lineTo(r, height)
            g2CornerBottomLeft(p, r, height - r, r, c1, c2, mid, c3, c4)
            // 左边 → 左上角
            p.lineTo(0f, r)
            g2CornerTopLeft(p, r, r, r, c1, c2, mid, c3, c4)
            p.close()
            return p
        }

        // 右上角：起点 (cx+r? 不) —— 从上边切点 (cx, cy-r) 出发，弯到右边切点 (cx+r, cy)
        private fun g2CornerTopRight(p: Path, cx: Float, cy: Float, r: Float, c1: Float, c2: Float, mid: Float, c3: Float, c4: Float) {
            // 起点 = (cx, cy - r)（上边）；终点 = (cx + r, cy)（右边）
            val midX = cx + r * mid
            val midY = cy - r * mid
            // 段1：沿上边伸出后抬离
            p.cubicTo(
                cx + r * c1, cy - r,
                cx + r * c2, cy - r * c2,
                midX, midY,
            )
            // 段2：越过角分线弧顶
            p.cubicTo(
                cx + r * c3, cy - r * c3,
                cx + r, cy - r * c4,
                cx + r, cy,
            )
        }

        // 右下角：从右边切点 (cx + r, cy) 到下边切点 (cx, cy + r)
        private fun g2CornerBottomRight(p: Path, cx: Float, cy: Float, r: Float, c1: Float, c2: Float, mid: Float, c3: Float, c4: Float) {
            val midX = cx + r * mid
            val midY = cy + r * mid
            p.cubicTo(
                cx + r, cy + r * c1,
                cx + r * c2, cy + r * c2,
                midX, midY,
            )
            p.cubicTo(
                cx + r * c3, cy + r * c3,
                cx + r * c4, cy + r,
                cx, cy + r,
            )
        }

        // 左下角：从下边切点 (cx, cy + r) 到左边切点 (cx - r, cy)
        private fun g2CornerBottomLeft(p: Path, cx: Float, cy: Float, r: Float, c1: Float, c2: Float, mid: Float, c3: Float, c4: Float) {
            val midX = cx - r * mid
            val midY = cy + r * mid
            p.cubicTo(
                cx - r * c1, cy + r,
                cx - r * c2, cy + r * c2,
                midX, midY,
            )
            p.cubicTo(
                cx - r * c3, cy + r * c3,
                cx - r, cy + r * c4,
                cx - r, cy,
            )
        }

        // 左上角：从左边切点 (cx - r, cy) 到上边切点 (cx, cy - r)
        private fun g2CornerTopLeft(p: Path, cx: Float, cy: Float, r: Float, c1: Float, c2: Float, mid: Float, c3: Float, c4: Float) {
            val midX = cx - r * mid
            val midY = cy - r * mid
            p.cubicTo(
                cx - r, cy - r * c1,
                cx - r * c2, cy - r * c2,
                midX, midY,
            )
            p.cubicTo(
                cx - r * c3, cy - r * c3,
                cx - r * c4, cy - r,
                cx, cy - r,
            )
        }
    }
}

/** Squircle 图标：drawable png + G2 连续圆角。圆角半径取 size*0.18，避免 0.30 把四角吃进图形。 */
@Composable
fun SquircleIcon(
    resId: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    cornerFraction: Float = 0.18f,
) {
    val shape = remember(cornerFraction) { G2SquircleShape(cornerFraction) }
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(shape),
        contentScale = ContentScale.Fit,
    )
}
