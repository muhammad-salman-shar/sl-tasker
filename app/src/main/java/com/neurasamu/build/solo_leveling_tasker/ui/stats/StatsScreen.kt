package com.neurasamu.build.solo_leveling_tasker.ui.stats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.solo_leveling_tasker.data.model.EventEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBackground
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBorder
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkCard
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkSurface
import com.neurasamu.build.solo_leveling_tasker.ui.theme.GoldWarning
import com.neurasamu.build.solo_leveling_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.solo_leveling_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextMuted
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextPrimary
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextSecondary
import com.neurasamu.build.solo_leveling_tasker.ui.viewmodel.StatsViewModel
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
    val recoveryQuests by statsViewModel.recoveryQuests.collectAsStateWithLifecycle()
    val debts by statsViewModel.unresolvedDebts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "HUNTER STATUS WINDOW",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryManaBlue
            )
            Text(
                text = "Permanent progression records and disciplinary debt audit",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }

        item {
            StatsOverviewCard(stats = stats)
        }

        if (recoveryQuests.isNotEmpty()) {
            item {
                Text(
                    text = "REDEMPTION QUESTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = DangerPenaltyRed
                )
            }
            items(recoveryQuests, key = { it.id }) { quest ->
                RecoveryQuestCard(
                    quest = quest,
                    onComplete = { statsViewModel.completeRecovery(quest.id) }
                )
            }
        }

        if (debts.isNotEmpty()) {
            item {
                Text(
                    text = "ACCUMULATED TASK DEBTS",
                    style = MaterialTheme.typography.labelLarge,
                    color = GoldWarning
                )
            }
            items(debts, key = { it.id }) { debt ->
                DebtCard(debt = debt)
            }
        }

        item {
            Text(
                text = "SYSTEM AUDIT LOG",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryManaBlue
            )
        }

        if (events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No system events logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                EventLogCard(event = event)
            }
        }
    }
}

@Composable
private fun StatsOverviewCard(stats: PlayerStatsEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatMetric(label = "COMPLETED", value = stats.totalCompleted.toString(), color = SuccessGreen)
                StatMetric(label = "MISSED", value = stats.totalMissed.toString(), color = DangerPenaltyRed)
                StatMetric(label = "RECOVERIES", value = stats.totalRecoveries.toString(), color = PrimaryManaBlue)
                StatMetric(label = "STREAK", value = stats.streak.toString(), color = GoldWarning)
            }
        }
    }
}

@Composable
private fun StatMetric(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 9.sp)
    }
}

@Composable
private fun RecoveryQuestCard(quest: RecoveryQuestEntity, onComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DangerPenaltyRed, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECOVERY PROTOCOL #${quest.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DangerPenaltyRed
                )
                Text(
                    text = "Clear to restore +20 HP and escape critical vulnerability status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerPenaltyRed,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "RESTORE", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DebtCard(debt: TaskDebtEntity) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(debt.originalDeadline))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .border(1.dp, GoldWarning, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "UNFULFILLED OBLIGATION", style = MaterialTheme.typography.labelSmall, color = GoldWarning)
                Text(text = "Original Deadline: $dateStr", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Text(text = "DEBT ACTIVE", style = MaterialTheme.typography.labelSmall, color = DangerPenaltyRed)
        }
    }
}

@Composable
private fun EventLogCard(event: EventEntity) {
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeStr = dateFormat.format(Date(event.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = event.type.name, style = MaterialTheme.typography.labelSmall, color = PrimaryManaBlue)
                Text(text = event.note, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                if (event.healthDelta != 0) {
                    val hpColor = if (event.healthDelta > 0) SuccessGreen else DangerPenaltyRed
                    val sign = if (event.healthDelta > 0) "+" else ""
                    Text(text = "$sign${event.healthDelta} HP", style = MaterialTheme.typography.labelSmall, color = hpColor)
                }
            }
        }
    }
}
