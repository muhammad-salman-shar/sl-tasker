package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.ui.theme.liquidGlass

@Composable
fun GlassHeader(
    title: String,
    subtitleSlot: @Composable () -> Unit = {},
    showSettings: Boolean = false,
    onSettingsClick: () -> Unit = {},
    aiOrb: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLight = ThemeState.isLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                tint = Color.White,
                fillTop = if (isLight) 0.14f else 0.12f,
                fillBottom = if (isLight) 0.05f else 0.04f,
                rimAlpha = if (isLight) 0.35f else 0.5f,
                shadowElevation = 20.dp
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (isLight) Color(0xFF0B1220) else Color(0xFFE8EEF9),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            subtitleSlot()
        }

        if (showSettings) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = if (isLight) Color(0xFF0F1830) else Color(0xFF3B82F6),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            aiOrb()
        }
    }
}
