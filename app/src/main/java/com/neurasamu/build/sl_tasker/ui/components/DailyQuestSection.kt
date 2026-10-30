package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.DailyQuestEntity
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.components.GlassCard
import com.neurasamu.build.sl_tasker.ui.components.NotchedShape

@Composable
fun DailyQuestSection(
    quests: List<DailyQuestEntity>,
    onComplete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (quests.isEmpty()) return

    val done = quests.count { it.completed }
    val total = quests.size

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DAILY QUESTS",
                color = AccentGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$done / $total",
                color = if (done == total) SuccessGreen else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))

        quests.forEach { quest ->
            DailyQuestRow(quest = quest, onComplete = { onComplete(quest.id) })
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DailyQuestRow(quest: DailyQuestEntity, onComplete: () -> Unit) {
    val done = quest.completed
    val bg = if (done) DarkCard.copy(alpha = 0.5f) else DarkCard
    val borderColor = if (done) SuccessGreen.copy(alpha = 0.3f) else AccentGold.copy(alpha = 0.3f)

    val shape = NotchedShape(notch = 10.dp, corner = 10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(bg.copy(alpha = 0.9f), bg.copy(alpha = 0.6f))
                )
            )
            .border(1.dp, borderColor, shape)
            .clickable(enabled = !done) { onComplete() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    if (done) SuccessGreen.copy(alpha = 0.2f)
                    else AccentGold.copy(alpha = 0.15f)
                )
                .border(
                    1.dp,
                    if (done) SuccessGreen else AccentGold,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    "✦",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                quest.title,
                color = if (done) TextMuted else TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                quest.description,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "+${quest.bonusEp} EP",
                color = if (done) TextMuted else PrimaryBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "+${quest.bonusHp} HP",
                color = if (done) TextMuted else SuccessGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
