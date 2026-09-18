package com.neurasamu.build.solo_leveling_tasker.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.solo_leveling_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBorder
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkCard
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkSurface
import com.neurasamu.build.solo_leveling_tasker.ui.theme.GoldWarning
import com.neurasamu.build.solo_leveling_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankA
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankB
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankC
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankD
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankE
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankS
import com.neurasamu.build.solo_leveling_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextMuted
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextPrimary
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextSecondary

@Composable
fun HunterHud(
    stats: PlayerStatsEntity,
    modifier: Modifier = Modifier
) {
    val rankLetter = getHunterRank(stats.level)
    val rankColor = getHunterRankColor(rankLetter)

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
            .background(DarkCard)
            .border(1.dp, if (stats.recoveryModeActive) DangerPenaltyRed else DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(2.dp, rankColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rankLetter,
                            style = MaterialTheme.typography.titleLarge,
                            color = rankColor
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LEVEL ${stats.level}",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (stats.recoveryModeActive) "⚠️ RECOVERY MODE" else "RANK $rankLetter HUNTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stats.recoveryModeActive) DangerPenaltyRed else TextSecondary
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

private fun getHunterRank(level: Int): String = when {
    level >= 70 -> "S"
    level >= 50 -> "A"
    level >= 35 -> "B"
    level >= 20 -> "C"
    level >= 10 -> "D"
    else -> "E"
}

private fun getHunterRankColor(rank: String): Color = when (rank) {
    "S" -> RankS
    "A" -> RankA
    "B" -> RankB
    "C" -> RankC
    "D" -> RankD
    else -> RankE
}
