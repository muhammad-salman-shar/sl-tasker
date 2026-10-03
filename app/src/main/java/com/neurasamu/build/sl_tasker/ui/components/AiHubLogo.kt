package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val AiCyan = Color(0xFF00D4FF)
private val AiViolet = Color(0xFF7C3AED)
private val AiPink = Color(0xFFFF006E)
private val AiChipDark = Color(0xFF0A0A0A)

@Composable
fun AiHubLogo(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    animated: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "aiHub")
    val shine by transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "shine"
    )
    val orbit1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit1"
    )
    val orbit2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit2"
    )

    Canvas(modifier = modifier.size(size)) {
        val k = this.size.minDimension / 64f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val px: (Float) -> Float = { v -> cx + (v - 32f) * k }
        val py: (Float) -> Float = { v -> cy + (v - 32f) * k }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AiCyan.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(cx, cy),
                radius = 30f * k
            ),
            radius = 30f * k,
            center = Offset(cx, cy)
        )

        val gradientBrush = Brush.linearGradient(
            colors = listOf(AiCyan, AiViolet, AiPink),
            start = Offset(px(0f), py(0f)),
            end = Offset(px(64f), py(64f))
        )

        drawCircle(
            brush = gradientBrush,
            radius = 27f * k,
            center = Offset(cx, cy),
            style = Stroke(width = 1f * k),
            alpha = 0.55f
        )

        val chipRect = Rect(px(14f), py(14f), px(50f), py(50f))
        val chipCorner = CornerRadius(11f * k, 11f * k)

        drawRoundRect(
            color = AiChipDark,
            topLeft = chipRect.topLeft,
            size = chipRect.size,
            cornerRadius = chipCorner
        )
        drawRoundRect(
            brush = gradientBrush,
            topLeft = chipRect.topLeft,
            size = chipRect.size,
            cornerRadius = chipCorner,
            style = Stroke(width = 2f * k)
        )

        val monogram = Path().apply {
            moveTo(px(22f), py(42f))
            lineTo(px(29.5f), py(23f))
            lineTo(px(37f), py(42f))
            moveTo(px(25.2f), py(35f))
            lineTo(px(33.8f), py(35f))
            moveTo(px(42f), py(23f))
            lineTo(px(42f), py(42f))
        }
        drawPath(
            path = monogram,
            brush = gradientBrush,
            style = Stroke(width = 3.2f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (animated) {
            val chipPath = Path().apply { addRoundRect(RoundRect(chipRect, chipCorner)) }
            val shift = shine * chipRect.width
            clipPath(chipPath) {
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            0.5f to Color.White.copy(alpha = 0.8f),
                            0.55f to Color.Transparent,
                            1f to Color.Transparent
                        ),
                        start = Offset(chipRect.left + shift, chipRect.top),
                        end = Offset(chipRect.right + shift, chipRect.bottom)
                    ),
                    topLeft = chipRect.topLeft,
                    size = chipRect.size
                )
            }

            val r = 28f * k
            val a1 = orbit1 * PI.toFloat() / 180f
            drawCircle(
                color = AiCyan,
                radius = 1.6f * k,
                center = Offset(cx + r * cos(a1), cy + r * sin(a1)),
                alpha = 0.9f
            )
            val a2 = (orbit2 + 88f) * PI.toFloat() / 180f
            drawCircle(
                color = AiPink,
                radius = 1.3f * k,
                center = Offset(cx + r * cos(a2), cy + r * sin(a2)),
                alpha = 0.9f
            )
        }
    }
}
