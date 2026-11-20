package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.stats.HunterRankBadge
import com.neurasamu.build.sl_tasker.ui.stats.HunterRankSystem
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState

@Composable
fun HunterHud(
    stats: PlayerStatsEntity,
    modifier: Modifier = Modifier
) {
    val tier = HunterRankSystem.forLevel(stats.level)

    val healthProgress by animateFloatAsState(
        targetValue = stats.health / 100f,
        label = "hp_anim"
    )
    val epProgress by animateFloatAsState(
        targetValue = stats.ep / GamificationEngine.EP_PER_LEVEL.toFloat(),
        label = "ep_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    if (ThemeState.isLight) {
                        listOf(Color.White.copy(alpha = 0.88f), Color.White.copy(alpha = 0.62f))
                    } else {
                        listOf(
                            tier.primary.copy(alpha = if (tier.isPremiumFrame) 0.16f else 0.08f),
                            DarkCard.copy(alpha = 0.42f)
                        )
                    }
                )
            )
            .border(
                width = 1.dp,
                color = if (stats.recoveryModeActive) DangerPenaltyRed else tier.primary.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    HunterRankBadge(
                        tier = tier,
                        size = 56.dp,
                        animate = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LEVEL ${stats.level}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.SansSerif,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(tier.primary.copy(alpha = 0.25f))
                                    .border(0.8.dp, tier.accent, RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tier.code,
                                    color = tier.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (stats.recoveryModeActive) "⚠️ RECOVERY MODE" else tier.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = if (stats.recoveryModeActive) DangerPenaltyRed else tier.accent
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 ${stats.streak}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Health Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "HP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${stats.health} / 100",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (stats.health < 50) DangerPenaltyRed else SuccessGreen
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { healthProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (stats.health < 50) DangerPenaltyRed else SuccessGreen,
                trackColor = DarkSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // EP Progression Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "EXP", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text(
                    text = "${stats.ep} / ${GamificationEngine.EP_PER_LEVEL} EP",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryManaBlue
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { epProgress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryManaBlue,
                trackColor = DarkSurface
            )
        }
    }
}
