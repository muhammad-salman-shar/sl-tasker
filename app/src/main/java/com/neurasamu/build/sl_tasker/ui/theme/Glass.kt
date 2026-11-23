package com.neurasamu.build.sl_tasker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    tint: Color = GlassTintBlue,
    tintAlpha: Float = 0.07f,
    topAlpha: Float = 0.11f,
    bottomAlpha: Float = 0.04f,
    borderWidth: Dp = 1.dp,
    rimAlpha: Float = 0.28f
): Modifier {
    val isLight = ThemeState.isLight
    val cardBase = ThemeState.tokens.cardBg
    val borderCol = ThemeState.tokens.border
    return this
        .clip(shape)
        .background(
            if (isLight) {
                Brush.verticalGradient(
                    listOf(
                        cardBase.copy(alpha = 0.96f),
                        cardBase.copy(alpha = 0.90f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        GlassFill.copy(alpha = topAlpha),
                        GlassFill.copy(alpha = (topAlpha + bottomAlpha) * 0.5f),
                        GlassFill.copy(alpha = bottomAlpha)
                    )
                )
            }
        )
        .background(tint.copy(alpha = if (isLight) tintAlpha * 0.7f else tintAlpha))
        .border(
            width = borderWidth,
            color = if (isLight) borderCol else GlassRim.copy(alpha = rimAlpha),
            shape = shape
        )
        .drawWithContent {
            drawContent()
            val h = 1.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        (if (isLight) tint else GlassRim).copy(alpha = rimAlpha * 1.1f),
                        Color.Transparent
                    )
                ),
                size = size.copy(height = h)
            )
        }
}

fun Modifier.liquidGlassPill(
    tint: Color = GlassTintBlue,
    selected: Boolean = false
): Modifier = this.liquidGlass(
    shape = RoundedCornerShape(50),
    tint = tint,
    tintAlpha = if (selected) 0.22f else 0.06f,
    topAlpha = if (selected) 0.18f else 0.09f,
    bottomAlpha = if (selected) 0.08f else 0.03f,
    borderWidth = if (selected) 1.2.dp else 0.8.dp,
    rimAlpha = if (selected) 0.50f else 0.22f
)
