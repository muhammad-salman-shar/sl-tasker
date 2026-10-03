package com.neurasamu.build.sl_tasker.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val activeQuests by taskViewModel.activeQuests.collectAsStateWithLifecycle()
    val playerStats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val upcoming = activeQuests.take(5)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCircle(
                icon = Icons.Rounded.Favorite,
                value = "${playerStats.health}",
                label = "HP",
                progress = playerStats.health / 100f,
                primaryColor = DangerPenaltyRed
            )
            StatCircle(
                icon = Icons.Rounded.Bolt,
                value = "${playerStats.ep}",
                label = "/ ${GamificationEngine.EP_PER_LEVEL} EP",
                progress = playerStats.ep.toFloat() / GamificationEngine.EP_PER_LEVEL,
                primaryColor = PrimaryManaBlue
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "UPCOMING QUESTS",
            style = MaterialTheme.typography.labelLarge,
            color = PrimaryManaBlue,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(10.dp))

        if (upcoming.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "THE SYSTEM AWAITS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Add your first quest to begin ascension.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = upcoming, key = { it.occurrence.id }) { item ->
                    UpcomingRow(
                        title = item.task.title,
                        timeMillis = item.occurrence.deadlineAt,
                        difficulty = item.task.difficulty.name
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCircle(
    icon: ImageVector,
    value: String,
    label: String,
    progress: Float,
    primaryColor: Color
) {
    val transition = rememberInfiniteTransition(label = "circleAnim")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(DarkCard)
            .border(1.dp, primaryColor.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer breathing halo
        Canvas(modifier = Modifier.size(130.dp)) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.08f * (glow + 0.3f)),
                radius = (this.size.minDimension / 2f) * pulse
            )
        }

        Canvas(modifier = Modifier.size(130.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            // Track
            drawArc(
                color = primaryColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = arcSize
            )
            // Progress
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = arcSize
            )
            // Rotating dot on progress tip
            val angleRad = Math.toRadians((-90f + 360f * progress.coerceIn(0f, 1f)).toDouble())
            val cx = size.width / 2f + (size.width / 2f - stroke / 2f) * Math.cos(angleRad).toFloat()
            val cy = size.height / 2f + (size.height / 2f - stroke / 2f) * Math.sin(angleRad).toFloat()
            drawCircle(
                color = primaryColor.copy(alpha = glow),
                radius = stroke * 0.9f,
                center = Offset(cx, cy)
            )
        }

        // Rotating outer arc accent
        Canvas(modifier = Modifier.size(146.dp)) {
            val stroke = 2.dp.toPx()
            val inset = stroke / 2f + 6.dp.toPx()
            val arcSize = Size(size.width - stroke - 12.dp.toPx(), size.height - stroke - 12.dp.toPx())
            drawArc(
                color = primaryColor.copy(alpha = 0.5f),
                startAngle = rotation,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = arcSize
            )
            drawArc(
                color = primaryColor.copy(alpha = 0.5f),
                startAngle = rotation + 180f,
                sweepAngle = 40f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = arcSize
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun UpcomingRow(
    title: String,
    timeMillis: Long,
    difficulty: String
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = timeFormat.format(Date(timeMillis))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = difficulty,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryManaBlue
        )
    }
}
