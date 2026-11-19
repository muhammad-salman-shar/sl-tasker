package com.neurasamu.build.sl_tasker.ui.stats

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class HunterRankTier(
    val code: String,
    val title: String,
    val minLevel: Int,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val frameBg: Color,
    val tierIndex: Int, // 0=E .. 9=LEGEND (3+ = B-Rank and above premium frames)
    val isPremiumFrame: Boolean
)

object HunterRankSystem {

    val tiers: List<HunterRankTier> = listOf(
        HunterRankTier("E", "AWAKENED NOVICE", 1, Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFF0C1220), 0, false),
        HunterRankTier("D", "IRON STRIKER", 3, Color(0xFFB45309), Color(0xFFD97706), Color(0xFFFCD34D), Color(0xFF14100C), 1, false),
        HunterRankTier("C", "STEEL VANGUARD", 5, Color(0xFF38BDF8), Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFF0A1624), 2, false),
        HunterRankTier("B", "EMERALD KNIGHT", 10, Color(0xFF10B981), Color(0xFFFFD700), Color(0xFF6EE7B7), Color(0xFF071A15), 3, true),
        HunterRankTier("A", "SAPPHIRE ELITE", 15, Color(0xFF3B82F6), Color(0xFF00F3FF), Color(0xFFBAE6FD), Color(0xFF08142C), 4, true),
        HunterRankTier("S", "CRIMSON MONARCH", 20, Color(0xFFEF4444), Color(0xFFFFB800), Color(0xFFFCA5A5), Color(0xFF1E080C), 5, true),
        HunterRankTier("S1", "AMETHYST GRANDMASTER", 30, Color(0xFFA855F7), Color(0xFFEC4899), Color(0xFFF0ABFC), Color(0xFF190A26), 6, true),
        HunterRankTier("S2", "SOLAR SOVEREIGN", 40, Color(0xFFFFD700), Color(0xFFFF6B00), Color(0xFFFFF5B8), Color(0xFF1C1405), 7, true),
        HunterRankTier("ULTRA", "CYBER MYTHIC ULTRA", 50, Color(0xFF00F3FF), Color(0xFFD946EF), Color(0xFFFFD700), Color(0xFF091326), 8, true),
        HunterRankTier("LEGEND", "ETERNAL LEGEND", 70, Color(0xFFFFD700), Color(0xFFFFFFFF), Color(0xFFFF3D71), Color(0xFF140D05), 9, true)
    )

    fun forLevel(level: Int): HunterRankTier {
        return tiers.lastOrNull { level >= it.minLevel } ?: tiers.first()
    }

    fun nextTier(level: Int): HunterRankTier? {
        return tiers.firstOrNull { it.minLevel > level }
    }
}

@Composable
fun HunterRankBadge(
    tier: HunterRankTier,
    size: Dp = 70.dp,
    animate: Boolean = true
) {
    val isLite = ThemeState.isLiteMode
    val runAnim = animate && !isLite && tier.isPremiumFrame

    val transition = rememberInfiniteTransition(label = "rankBadge_${tier.code}")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (runAnim) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (tier.tierIndex >= 8) 3200 else 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val textBrush = Brush.linearGradient(
        listOf(tier.accent, tier.primary, tier.secondary)
    )
    val fontSize = when {
        tier.code.length >= 6 -> (size.value * 0.165f).sp
        tier.code.length >= 5 -> (size.value * 0.19f).sp
        tier.code.length == 2 -> (size.value * 0.30f).sp
        else -> (size.value * 0.36f).sp
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val r = this.size.minDimension / 2f

            // Ambient aura for B rank and above
            if (tier.isPremiumFrame) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tier.primary.copy(alpha = 0.34f * pulse),
                            tier.secondary.copy(alpha = 0.12f * pulse),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }

            // Rotating outer energy ring for S and above
            if (tier.tierIndex >= 5) {
                val ringR = r * 0.94f
                val ringBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.35f to tier.primary.copy(alpha = 0.8f),
                    0.50f to tier.accent,
                    0.65f to Color.Transparent,
                    0.85f to tier.secondary.copy(alpha = 0.85f),
                    1.0f to Color.Transparent
                )
                rotate(degrees = rotation, pivot = Offset(cx, cy)) {
                    drawCircle(
                        brush = ringBrush,
                        radius = ringR,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating inner star halo for ULTRA & LEGEND
            if (tier.tierIndex >= 8) {
                rotate(degrees = -rotation * 1.4f, pivot = Offset(cx, cy)) {
                    val starPath = Path()
                    val pts = if (tier.tierIndex == 9) 8 else 6
                    for (i in 0 until pts * 2) {
                        val rad = if (i % 2 == 0) r * 0.90f else r * 0.70f
                        val ang = (i * PI / pts) - PI / 2.0
                        val px = cx + (rad * cos(ang)).toFloat()
                        val py = cy + (rad * sin(ang)).toFloat()
                        if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                    }
                    starPath.close()
                    drawPath(
                        path = starPath,
                        brush = Brush.linearGradient(listOf(tier.primary, tier.secondary, tier.accent)),
                        style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Miter)
                    )
                }
            }

            // Side Wing Blades for B-Rank and above (FreeFire crest aesthetic)
            if (tier.isPremiumFrame) {
                val wingSpan = if (tier.tierIndex >= 7) 0.98f else 0.90f
                val wingColor = Brush.linearGradient(listOf(tier.secondary, tier.primary))
                // Left wing
                val lw = Path().apply {
                    moveTo(cx - r * 0.56f, cy - r * 0.22f)
                    lineTo(cx - r * wingSpan, cy - r * 0.06f)
                    lineTo(cx - r * 0.72f, cy + r * 0.14f)
                    lineTo(cx - r * 0.54f, cy + r * 0.04f)
                    close()
                }
                // Right wing
                val rw = Path().apply {
                    moveTo(cx + r * 0.56f, cy - r * 0.22f)
                    lineTo(cx + r * wingSpan, cy - r * 0.06f)
                    lineTo(cx + r * 0.72f, cy + r * 0.14f)
                    lineTo(cx + r * 0.54f, cy + r * 0.04f)
                    close()
                }
                drawPath(lw, brush = wingColor)
                drawPath(rw, brush = wingColor)
            }

            // Main Crest Shape (escalates from circle -> octagon -> hexagonal shield)
            val crestR = if (tier.isPremiumFrame) r * 0.74f else r * 0.80f
            val borderBrush = Brush.linearGradient(
                colors = listOf(tier.accent, tier.primary, tier.secondary, tier.primary)
            )
            if (tier.tierIndex == 0) {
                drawCircle(color = tier.frameBg, radius = crestR, center = Offset(cx, cy))
                drawCircle(
                    brush = borderBrush,
                    radius = crestR,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.2.dp.toPx())
                )
            } else {
                val shield = Path()
                if (tier.tierIndex <= 2) {
                    // Octagon / Steel Shield
                    val sides = if (tier.tierIndex == 1) 8 else 6
                    for (i in 0 until sides) {
                        val ang = (2.0 * PI * i / sides) - PI / 2.0
                        val px = cx + (crestR * cos(ang)).toFloat()
                        val py = cy + (crestR * sin(ang)).toFloat()
                        if (i == 0) shield.moveTo(px, py) else shield.lineTo(px, py)
                    }
                    shield.close()
                } else {
                    // Armored FreeFire Crest Shield (B -> LEGEND)
                    shield.moveTo(cx, cy - crestR * 1.05f)
                    shield.lineTo(cx + crestR * 0.86f, cy - crestR * 0.52f)
                    shield.lineTo(cx + crestR * 0.76f, cy + crestR * 0.42f)
                    shield.lineTo(cx, cy + crestR * 1.05f)
                    shield.lineTo(cx - crestR * 0.76f, cy + crestR * 0.42f)
                    shield.lineTo(cx - crestR * 0.86f, cy - crestR * 0.52f)
                    shield.close()
                }

                drawPath(
                    path = shield,
                    brush = Brush.verticalGradient(
                        listOf(tier.frameBg, Color(0xFF03060C))
                    )
                )
                drawPath(
                    path = shield,
                    brush = borderBrush,
                    style = Stroke(
                        width = if (tier.isPremiumFrame) 2.8.dp.toPx() else 2.2.dp.toPx(),
                        join = StrokeJoin.Miter
                    )
                )

                // Top Crown Gem for A Rank and above
                if (tier.tierIndex >= 4) {
                    val gemY = cy - crestR * 1.02f
                    val gemR = r * 0.11f
                    val gem = Path().apply {
                        moveTo(cx, gemY - gemR)
                        lineTo(cx + gemR, gemY)
                        lineTo(cx, gemY + gemR)
                        lineTo(cx - gemR, gemY)
                        close()
                    }
                    drawPath(gem, color = tier.accent)
                }
            }
        }

        Text(
            text = tier.code,
            style = TextStyle(
                brush = textBrush,
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = if (tier.code.length > 2) 0.5.sp else 0.sp
            )
        )
    }
}

@Composable
fun RankShowcaseDialog(
    currentLevel: Int,
    onDismiss: () -> Unit
) {
    val activeTier = HunterRankSystem.forLevel(currentLevel)
    val nextTier = HunterRankSystem.nextTier(currentLevel)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A1022), Color(0xFF040710))
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(activeTier.accent, activeTier.primary, activeTier.secondary)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "HUNTER RANK HALL",
                        color = activeTier.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "E-Rank to Eternal Legend",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFE8EEF9)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Current Rank Hero Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(activeTier.primary.copy(alpha = 0.22f), Color(0xFF080E1D))
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.linearGradient(listOf(activeTier.accent, activeTier.primary)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HunterRankBadge(tier = activeTier, size = 76.dp, animate = true)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "YOUR CURRENT RANK",
                        color = activeTier.accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "${activeTier.code} • ${activeTier.title}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (nextTier != null) {
                            "Level $currentLevel  •  Reach Level ${nextTier.minLevel} for ${nextTier.code}"
                        } else {
                            "Level $currentLevel  •  MAX TIER ACHIEVED"
                        },
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(HunterRankSystem.tiers) { tier ->
                    val isCurrent = tier.code == activeTier.code
                    val isUnlocked = currentLevel >= tier.minLevel
                    val borderBrush = if (isCurrent || tier.isPremiumFrame) {
                        Brush.linearGradient(
                            listOf(
                                tier.accent.copy(alpha = if (isUnlocked) 0.95f else 0.45f),
                                tier.primary.copy(alpha = if (isUnlocked) 0.85f else 0.35f),
                                tier.secondary.copy(alpha = if (isUnlocked) 0.85f else 0.35f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(tier.primary.copy(alpha = 0.35f), tier.secondary.copy(alpha = 0.25f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        tier.frameBg.copy(alpha = if (isCurrent) 0.95f else 0.75f),
                                        DarkCard.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                width = if (isCurrent) 1.6.dp else 1.dp,
                                brush = borderBrush,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HunterRankBadge(
                            tier = tier,
                            size = 58.dp,
                            animate = isCurrent || tier.tierIndex >= 7
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    tier.code,
                                    color = tier.accent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(6.dp))
                                if (tier.isPremiumFrame) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(tier.primary.copy(alpha = 0.2f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            if (tier.tierIndex >= 8) "MYTHIC" else "PREMIUM",
                                            color = tier.accent,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                tier.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlocks at Level ${tier.minLevel}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(tier.primary.copy(alpha = 0.25f))
                                    .border(1.dp, tier.accent, CircleShape)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "ACTIVE",
                                    color = tier.accent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else if (!isUnlocked) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = "Locked",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                "UNLOCKED",
                                color = Color(0xFF4ADE80),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
