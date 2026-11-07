package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.ui.theme.liquidGlass
import kotlin.math.roundToInt

data class DockItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun FloatingDock(
    items: List<DockItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF3B82F6)
) {
    val isLight = ThemeState.isLight
    val dockShape = RoundedCornerShape(32.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        val totalWidth = maxWidth
        val itemWidth = totalWidth / items.size
        val pillWidth = itemWidth

        // Animated pill offset
        val pillOffset = remember { Animatable(0f) }
        LaunchedEffect(selectedIndex) {
            pillOffset.animateTo(
                targetValue = itemWidth.value * selectedIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .liquidGlass(
                    shape = dockShape,
                    tint = if (isLight) Color.White else Color.White,
                    fillTop = if (isLight) 0.55f else 0.12f,
                    fillBottom = if (isLight) 0.30f else 0.04f,
                    rimAlpha = if (isLight) 0.9f else 0.55f,
                    shadowElevation = 24.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sliding pill indicator
            Box(
                modifier = Modifier
                    .offset { IntOffset((pillOffset.value * 1.5f).roundToInt(), 0) }
                    .width(itemWidth)
                    .height(58.dp)
                    .padding(horizontal = 8.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(24.dp),
                        tint = accent,
                        fillTop = if (isLight) 0.35f else 0.28f,
                        fillBottom = if (isLight) 0.18f else 0.12f,
                        rimAlpha = 0.85f,
                        shadowElevation = 12.dp
                    )
            )

            // Items
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) {
                            if (isLight) Color.White else Color.White
                        } else {
                            if (isLight) Color(0xFF3B3B3B) else Color(0xFF8B9AB8)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = item.label,
                        color = if (isSelected) {
                            if (isLight) Color.White else Color.White
                        } else {
                            if (isLight) Color(0xFF3B3B3B) else Color(0xFF8B9AB8)
                        },
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
