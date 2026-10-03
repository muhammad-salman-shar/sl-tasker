package com.neurasamu.build.sl_tasker.ui.stats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.EventType
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.RankA
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val stats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val events by statsViewModel.recentEvents.collectAsStateWithLifecycle()

    val healthProgress = (stats.health.coerceIn(0, 100)) / 100f
    val epProgress = (stats.ep.coerceIn(0, GamificationEngine.EP_PER_LEVEL)).toFloat() /
            GamificationEngine.EP_PER_LEVEL.toFloat()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "HUNTER PROFILE",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryManaBlue,
                letterSpacing = 2.sp
            )
            Text(
                text = "Permanent progression record",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        item {
            RankBanner(
                level = stats.level,
                streak = stats.streak
            )
        }

        item {
            ProgressCard(
                label = "HEALTH",
                value = "${stats.health} / 100",
                progress = healthProgress,
                color = when {
                    stats.health <= 30 -> DangerPenaltyRed
                    stats.health <= 70 -> GoldWarning
                    else -> SuccessGreen
                }
            )
        }

        item {
            ProgressCard(
                label = "ENERGY",
                value = "${stats.ep} / ${GamificationEngine.EP_PER_LEVEL} EP",
                progress = epProgress,
                color = PrimaryManaBlue
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = "COMPLETED",
                    value = "${stats.totalCompleted}",
                    color = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "MISSED",
                    value = "${stats.totalMissed}",
                    color = DangerPenaltyRed,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "RECOVERIES",
                    value = "${stats.totalRecoveries}",
                    color = RankA,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "RECENT ACTIVITY",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryManaBlue,
                fontSize = 12.sp
            )
        }

        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity yet. Complete a quest to begin.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(items = events.take(15), key = { it.id }) { event ->
                EventRow(event)
            }
        }
    }
}

@Composable
private fun RankBanner(level: Int, streak: Int) {
    val rank = when {
        level >= 20 -> "S"
        level >= 15 -> "A"
        level >= 10 -> "B"
        level >= 5 -> "C"
        level >= 3 -> "D"
        else -> "E"
    }
    val rankColor = when (rank) {
        "S" -> com.neurasamu.build.sl_tasker.ui.theme.RankS
        "A" -> RankA
        "B" -> com.neurasamu.build.sl_tasker.ui.theme.RankB
        "C" -> com.neurasamu.build.sl_tasker.ui.theme.RankC
        "D" -> com.neurasamu.build.sl_tasker.ui.theme.RankD
        else -> com.neurasamu.build.sl_tasker.ui.theme.RankE
    }

    val transition = rememberInfiniteTransition(label = "rank")
    val glow by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rankGlow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, rankColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawCircle(
                    color = rankColor.copy(alpha = 0.15f * (glow + 0.3f)),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = rankColor,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            Text(
                text = rank,
                color = rankColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "LEVEL $level",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RANK $rank HUNTER",
                color = rankColor,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "STREAK",
                color = TextMuted,
                fontSize = 10.sp
            )
            Text(
                text = "$streak",
                color = GoldWarning,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressCard(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    val smooth by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "progress_$label"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DarkSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(smooth)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun EventRow(event: com.neurasamu.build.sl_tasker.data.model.EventEntity) {
    val (icon, color, title) = when (event.type) {
        EventType.OCCURRENCE_COMPLETED -> Triple("+", SuccessGreen, "Quest Completed")
        EventType.OCCURRENCE_MISSED -> Triple("-", DangerPenaltyRed, "Quest Missed")
        EventType.PENALTY_APPLIED -> Triple("-", DangerPenaltyRed, "Penalty Applied")
        EventType.RECOVERY_COMPLETED -> Triple("*", RankA, "Recovery Completed")
        EventType.LEVEL_UP -> Triple("^", PrimaryManaBlue, "Level Up!")
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            if (event.note.isNotBlank()) {
                Text(
                    text = event.note,
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (event.epDelta != 0) {
                Text(
                    text = if (event.epDelta > 0) "+${event.epDelta} EP" else "${event.epDelta} EP",
                    color = if (event.epDelta > 0) SuccessGreen else DangerPenaltyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (event.healthDelta != 0) {
                Text(
                    text = if (event.healthDelta > 0) "+${event.healthDelta} HP" else "${event.healthDelta} HP",
                    color = if (event.healthDelta > 0) SuccessGreen else DangerPenaltyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = dateFormat.format(Date(event.timestamp)),
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}
