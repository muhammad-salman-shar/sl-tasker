package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PaywallTrigger
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
    val isPro by LicenseManager.isProFlow.collectAsState()

    val goldBorderBrush = Brush.linearGradient(
        listOf(Color(0xFFFFF5B8), Color(0xFFFFD700), Color(0xFFFFA500))
    )
    val goldTextBrush = Brush.linearGradient(
        listOf(Color(0xFFFFFAD1), Color(0xFFFFD700), Color(0xFFFF9800))
    )
    val goldBgBrush = Brush.verticalGradient(
        listOf(Color(0xFFFFD700).copy(alpha = 0.24f), Color(0xFFFF8C00).copy(alpha = 0.12f))
    )
    val freeBgBrush = Brush.verticalGradient(
        listOf(Color(0xFF3B82F6).copy(alpha = 0.22f), Color(0xFF2563EB).copy(alpha = 0.10f))
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .liquidGlass(
                shape = RoundedCornerShape(24.dp),
                tint = Color.White,
                fillTop = 0.12f,
                fillBottom = 0.04f,
                rimAlpha = 0.5f,
                shadowElevation = 20.dp
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = Color(0xFFE8EEF9),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            subtitleSlot()
        }

        if (showSettings) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isPro) goldBgBrush else freeBgBrush)
                .border(
                    width = 1.dp,
                    brush = if (isPro) goldBorderBrush else Brush.linearGradient(
                        listOf(Color(0xFF60A5FA).copy(alpha = 0.8f), Color(0xFF3B82F6).copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(50)
                )
                .clickable {
                    if (!isPro) {
                        PaywallTrigger.show("Upgrade to SL Tasker PRO to unlock unlimited tasks, AI Hub, and Golden Hunter status.")
                    }
                }
                .padding(horizontal = 11.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPro) {
                Text(
                    text = "PRO",
                    style = TextStyle(
                        brush = goldTextBrush,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                )
            } else {
                Text(
                    text = "FREE",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
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
