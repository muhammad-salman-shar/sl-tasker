package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass material modifier (iOS 26 / VisionOS style).
 * - Translucent vertical gradient fill
 * - Directional specular rim (bright top-left -> transparent bottom-right)
 * - Soft ambient elevation shadow
 *
 * Perf-safe: no RenderEffect/blur, fake-glass approach for low-end devices.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White,
    fillTop: Float = 0.10f,
    fillBottom: Float = 0.03f,
    rimAlpha: Float = 0.55f,
    shadowElevation: Dp = 16.dp
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        clip = false
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            colors = listOf(
                tint.copy(alpha = fillTop),
                tint.copy(alpha = fillBottom)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = rimAlpha),
            0.45f to Color.White.copy(alpha = 0.12f),
            1f to Color.White.copy(alpha = 0.02f),
            start = Offset.Zero,
            end = Offset(1500f, 1500f)
        ),
        shape = shape
    )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    padding: PaddingValues = PaddingValues(16.dp),
    shadowElevation: Dp = 16.dp,
    fillTop: Float = 0.10f,
    fillBottom: Float = 0.03f,
    tint: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                tint = tint,
                fillTop = fillTop,
                fillBottom = fillBottom,
                shadowElevation = shadowElevation
            )
            .padding(padding),
        content = content
    )
}
