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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.EventType
import com.neurasamu.build.sl_tasker.data.player.PlayerProfile
import com.neurasamu.build.sl_tasker.data.player.PlayerProfileStore
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.util.ShareCardGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

// Cyan accent from HTML design
private val CyberCyan = Color(0xFF00F3FF)
private val CyberBg = Color(0xFF050810)

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val stats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val events by statsViewModel.recentEvents.collectAsStateWithLifecycle()

    val ctx = LocalContext.current
    var profile by remember { mutableStateOf<PlayerProfile?>(null) }
    LaunchedEffect(Unit) {
        try {
            PlayerProfileStore(ctx).profile.collect { p -> profile = p }
        } catch (_: Throwable) { }
    }

    Box(modifier = modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Transparent)) {
        // Background grid + radial glow (HTML replica)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Radial glow at top
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width / 2, 0f),
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = Offset(size.width / 2, 0f)
            )
            // Grid lines
            val step = 60f
            val gridColor = CyberCyan.copy(alpha = 0.03f)
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Identity card
            profile?.let { p ->
                if (p.introCompleted) {
                    item {
                        IdentityCard(
                            profile = p,
                            onShare = {
                                try { ShareCardGenerator.share(ctx, stats, p) } catch (_: Throwable) {}
                            }
                        )
                    }
                }
            }

            // HUNTER PROFILE title
            item {
                Column {
                    Text(
                        "HUNTER PROFILE",
                        color = CyberCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Permanent progression record",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            // Rank card
            item {
                RankCard(
                    level = stats.level,
                    streak = stats.streak
                )
            }

            // HP + EP combined card
            item {
                BarsCard(
                    hp = stats.health,
                    ep = stats.ep
                )
            }

            // Stat grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallStatBox(
                        value = "${stats.totalCompleted}",
                        label = "COMPLETED",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatBox(
                        value = "${stats.totalMissed}",
                        label = "MISSED",
                        color = DangerPenaltyRed,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatBox(
                        value = "${stats.totalRecoveries}",
                        label = "RECOVERIES",
                        color = GoldWarning,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // RECENT ACTIVITY title
            item {
                Text(
                    "RECENT ACTIVITY",
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (events.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkCard)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No activity yet.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else {
                items(items = events.take(10), key = { it.id }) { event ->
                    ActivityRow(event)
                }
            }
        }
    }
}

@Composable
private fun IdentityCard(profile: PlayerProfile, onShare: () -> Unit) {
    val genderStr = when (profile.gender.lowercase()) {
        "male" -> "MALE"
        "female" -> "FEMALE"
        else -> profile.gender.uppercase()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.7f))
            .border(1.dp, CyberCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "PLAYER IDENTITY",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onShare, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Share, contentDescription = "Share", tint = CyberCyan, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                IdentityField("NAME", profile.name.uppercase())
                Spacer(Modifier.height(10.dp))
                IdentityField("GENDER", genderStr)
            }
            Column(modifier = Modifier.weight(1f)) {
                IdentityField("PLAYER ID", profile.playerId, cyan = true)
                Spacer(Modifier.height(10.dp))
                IdentityField("AGE", profile.age.toString())
            }
        }
    }
}

@Composable
private fun IdentityField(label: String, value: String, cyan: Boolean = false) {
    Column {
        Text(label, color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
        Text(
            value.ifBlank { "—" },
            color = if (cyan) CyberCyan else TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun RankCard(level: Int, streak: Int) {
    val rank = when {
        level >= 20 -> "S"
        level >= 15 -> "A"
        level >= 10 -> "B"
        level >= 5 -> "C"
        level >= 3 -> "D"
        else -> "E"
    }

    val transition = rememberInfiniteTransition(label = "rankPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(CyberCyan.copy(alpha = 0.08f), Color.Transparent)
                )
            )
            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge with dashed border
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(64.dp)) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.25f * (pulse + 0.2f)),
                    radius = size.minDimension / 2f
                )
                drawCircle(
                    color = CyberCyan,
                    radius = size.minDimension / 2f - 2.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                rank,
                color = CyberCyan,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "LEVEL $level",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                "RANK $rank HUNTER",
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text("STREAK", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$streak",
                    color = GoldWarning,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(Modifier.width(4.dp))
                Text("🔥", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun BarsCard(hp: Int, ep: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        BarRow(
            label = "HEALTH",
            value = "$hp / 100",
            progress = hp / 100f,
            colorStart = Color(0xFF00B33C),
            colorEnd = SuccessGreen
        )
        Spacer(Modifier.height(14.dp))
        BarRow(
            label = "ENERGY",
            value = "$ep / ${GamificationEngine.EP_PER_LEVEL} EP",
            progress = ep.toFloat() / GamificationEngine.EP_PER_LEVEL,
            colorStart = Color(0xFF0066CC),
            colorEnd = Color(0xFF00B3FF)
        )
    }
}

@Composable
private fun BarRow(
    label: String,
    value: String,
    progress: Float,
    colorStart: Color,
    colorEnd: Color
) {
    val smooth by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "smooth_$label"
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                color = colorEnd,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                color = colorEnd,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(smooth)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        Brush.horizontalGradient(listOf(colorStart, colorEnd))
                    )
            )
        }
    }
}

@Composable
private fun SmallStatBox(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun ActivityRow(event: com.neurasamu.build.sl_tasker.data.model.EventEntity) {
    val (icon, color, title) = when (event.type) {
        EventType.OCCURRENCE_COMPLETED -> Triple(Icons.Rounded.CheckCircle, SuccessGreen, "Quest Completed")
        EventType.OCCURRENCE_MISSED -> Triple(Icons.Rounded.Warning, DangerPenaltyRed, "Quest Missed")
        EventType.PENALTY_APPLIED -> Triple(Icons.Rounded.Warning, DangerPenaltyRed, "Penalty Applied")
        EventType.RECOVERY_COMPLETED -> Triple(Icons.Rounded.ArrowUpward, GoldWarning, "Recovery Completed")
        EventType.LEVEL_UP -> Triple(Icons.Rounded.ArrowUpward, CyberCyan, "Level Up!")
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard.copy(alpha = 0.7f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (event.note.isNotBlank()) {
                Text(
                    event.note,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        Text(
            dateFormat.format(Date(event.timestamp)),
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}
