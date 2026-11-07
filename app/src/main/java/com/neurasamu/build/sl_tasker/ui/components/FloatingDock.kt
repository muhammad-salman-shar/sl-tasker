package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.ui.theme.liquidGlass
import kotlin.math.roundToInt

data class DockItem(val label: String, val icon: ImageVector)

@Composable
fun FloatingDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF3B82F6)
) {
    if (items.isEmpty()) return
    val isLight = ThemeState.isLight
    val dockShape = RoundedCornerShape(32.dp)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val itemWidthDp = maxWidth / items.size
        val itemWidthPx = with(density) { itemWidthDp.toPx() }
        val pillOffsetPx = remember { Animatable(0f) }
        LaunchedEffect(selectedIndex, itemWidthPx) {
            pillOffsetPx.animateTo(
                targetValue = itemWidthPx * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .liquidGlass(
                    shape = dockShape,
                    tint = Color.White,
                    fillTop = 0.12f,
                    fillBottom = 0.04f,
                    rimAlpha = 0.55f,
                    shadowElevation = 24.dp
                )
        ) {
            // Sliding pill (overlay behind items)
            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffsetPx.value.roundToInt(), 0) }
                    .width(itemWidthDp)
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        tint = accent,
                        fillTop = 0.50f,
                        fillBottom = 0.28f,
                        rimAlpha = 0.9f,
                        shadowElevation = 10.dp
                    )
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { i, item ->
                    val isSelected = i == selectedIndex
                    val fg = when {
                        isSelected -> Color.White
                        isLight -> Color(0xFF1F2937)
                        else -> Color(0xFF8B9AB8)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(i) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = fg,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = item.label,
                            color = fg,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
