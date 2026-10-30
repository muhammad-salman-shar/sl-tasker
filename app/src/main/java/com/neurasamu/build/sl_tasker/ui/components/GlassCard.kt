package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder

/**
 * Solo-Leveling style shape: 3 rounded corners + top-right notched diagonal cut.
 */
class NotchedShape(
    private val notch: Dp = 14.dp,
    private val corner: Dp = 12.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val n = with(density) { notch.toPx() }
        val c = with(density) { corner.toPx() }
        val path = Path().apply {
            moveTo(c, 0f)
            lineTo(size.width - n, 0f)
            lineTo(size.width, n)               // diagonal notch
            lineTo(size.width, size.height - c)
            quadraticBezierTo(size.width, size.height, size.width - c, size.height)
            lineTo(c, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - c)
            lineTo(0f, c)
            quadraticBezierTo(0f, 0f, c, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    notch: Dp = 14.dp,
    corner: Dp = 12.dp,
    borderColor: Color = DarkBorder,
    topColor: Color = DarkCard.copy(alpha = 0.9f),
    bottomColor: Color = DarkCard.copy(alpha = 0.6f),
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = NotchedShape(notch = notch, corner = corner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(listOf(topColor, bottomColor))
            )
            .border(1.dp, borderColor, shape)
            .padding(padding),
        content = content
    )
}
