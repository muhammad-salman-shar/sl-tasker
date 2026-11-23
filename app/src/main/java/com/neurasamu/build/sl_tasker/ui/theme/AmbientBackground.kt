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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val baseBg = ThemeState.tokens.background
    val isLight = ThemeState.isLight

    if (ThemeState.isLiteMode) {
        Box(modifier = modifier.fillMaxSize().background(baseBg)) {
            content()
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val alphaScale = if (isLight) 0.55f else 1.0f

    Box(modifier = modifier.fillMaxSize().background(baseBg)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTintBlue.copy(alpha = 0.20f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.15f + 0.08f * drift), h * 0.12f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * (0.15f + 0.08f * drift), h * 0.12f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTintViolet.copy(alpha = 0.16f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * (0.42f - 0.06f * drift)),
                    radius = w * 0.60f
                ),
                radius = w * 0.60f,
                center = Offset(w * 0.88f, h * (0.42f - 0.06f * drift))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassTintCyan.copy(alpha = 0.13f * alphaScale),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * (0.86f + 0.04f * drift)),
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = Offset(w * 0.22f, h * (0.86f + 0.04f * drift))
            )
        }
        content()
    }
}
