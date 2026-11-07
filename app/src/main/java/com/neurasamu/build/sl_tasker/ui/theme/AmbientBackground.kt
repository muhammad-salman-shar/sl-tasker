package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = ThemeState.isLight
    val base = if (isLight) Color(0xFFEAF0FA) else Color(0xFF03060F)
    val blueA = if (isLight) 0.20f else 0.32f
    val violetA = if (isLight) 0.16f else 0.26f
    val emeraldA = if (isLight) 0.14f else 0.20f
    val cyanA = if (isLight) 0.12f else 0.18f

    val transition = rememberInfiniteTransition(label = "ambient")
    val t1 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Restart),
        label = "t2"
    )
    val t3 by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(33000, easing = LinearEasing), RepeatMode.Restart),
        label = "t3"
    )

    Box(modifier = modifier.fillMaxSize().background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val x1 = w * (0.20f + 0.14f * sin(t1 * 6.283f))
            val y1 = h * (0.22f + 0.10f * cos(t1 * 4.0f))
            val r1 = w * 0.95f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF3B82F6).copy(alpha = blueA), Color.Transparent),
                    center = Offset(x1, y1), radius = r1
                ),
                radius = r1, center = Offset(x1, y1)
            )

            val x2 = w * (0.78f + 0.09f * cos(t2 * 5.0f))
            val y2 = h * (0.72f + 0.10f * sin(t2 * 3.5f))
            val r2 = w * 0.85f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF8B5CF6).copy(alpha = violetA), Color.Transparent),
                    center = Offset(x2, y2), radius = r2
                ),
                radius = r2, center = Offset(x2, y2)
            )

            val x3 = w * (0.55f + 0.15f * sin(t3 * 4.0f))
            val y3 = h * (0.48f + 0.12f * cos(t3 * 3.0f))
            val r3 = w * 0.70f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF10B981).copy(alpha = emeraldA), Color.Transparent),
                    center = Offset(x3, y3), radius = r3
                ),
                radius = r3, center = Offset(x3, y3)
            )

            val x4 = w * (0.85f + 0.07f * sin(t1 * 3.0f))
            val y4 = h * (0.12f + 0.08f * cos(t1 * 5.0f))
            val r4 = w * 0.55f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFF00D9FF).copy(alpha = cyanA), Color.Transparent),
                    center = Offset(x4, y4), radius = r4
                ),
                radius = r4, center = Offset(x4, y4)
            )
        }
        content()
    }
}
