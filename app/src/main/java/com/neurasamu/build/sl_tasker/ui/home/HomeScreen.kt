package com.neurasamu.build.sl_tasker.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.data.task.HabitPresets
import com.neurasamu.build.sl_tasker.data.task.RespectfulHabitPreset
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PaywallTrigger
import com.neurasamu.build.sl_tasker.ui.components.DailyQuestSection
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.BadgeBg
import com.neurasamu.build.sl_tasker.ui.theme.BadgeText
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.viewmodel.DailyQuestViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import com.neurasamu.build.sl_tasker.util.SoundFx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    dailyQuestViewModel: DailyQuestViewModel,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val activeQuests by taskViewModel.activeQuests.collectAsStateWithLifecycle()
    val completedQuests by taskViewModel.completedQuests.collectAsStateWithLifecycle()
    val stats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val dailyQuests by dailyQuestViewModel.todayQuests.collectAsStateWithLifecycle()
    val dailyCompleted by dailyQuestViewModel.completedTodayCount.collectAsStateWithLifecycle()

    var showDailyCongrats by remember { mutableStateOf(false) }

    // Daily Completion Celebration when all scheduled tasks for today are completed
    LaunchedEffect(activeQuests.size, completedQuests.size) {
        if (activeQuests.isEmpty() && completedQuests.isNotEmpty()) {
            val cal = Calendar.getInstance()
            val dayKey = "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
            val prefs = ctx.getSharedPreferences("sl_daily_congrats", Context.MODE_PRIVATE)
            if (!prefs.getBoolean(dayKey, false)) {
                prefs.edit().putBoolean(dayKey, true).apply()
                showDailyCongrats = true
                try { SoundFx.levelUp() } catch (_: Throwable) {}
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Compact Status Card
        item {
            StatusCard(stats = stats)
        }

        // 2. Daily Quests
        item {
            DailyQuestSection(
                quests = dailyQuests,
                completedCount = dailyCompleted,
                onComplete = { dailyQuestViewModel.completeQuest(it) }
            )
        }

        // 3. Respectful Quick Routine Presets
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY ROUTINE PRESETS",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryManaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "1-tap add",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = HabitPresets.all, key = { it.id }) { preset ->
                        PresetChipCard(
                            preset = preset,
                            onAdd = {
                                if (!LicenseManager.isPro() && activeQuests.size >= 2) {
                                    PaywallTrigger.show("Free tier allows up to 2 active tasks. Upgrade to Pro for unlimited tasks.")
                                } else {
                                    taskViewModel.createQuest(
                                        title = preset.title,
                                        description = preset.subtitle,
                                        difficulty = preset.difficulty,
                                        reminderMinutesOfDay = preset.reminderMinutesOfDay,
                                        customRepeatDays = "",
                                        durationMinutes = preset.durationMinutes,
                                        isRepeat = false,
                                        allowEarlyComplete = true,
                                        userType = preset.categoryLabel,
                                        scheduleMode = preset.scheduleMode,
                                        validityDays = 1,
                                        isAdvanced = preset.isAdvanced,
                                        hpDropEnabled = true,
                                        requireQrProof = false,
                                        stepGoal = preset.stepGoal
                                    )
                                    Toast.makeText(
                                        ctx,
                                        "Added \"${preset.title}\" to today's tasks!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }

        // 4. Today's Tasks Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S TASKS",
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryManaBlue,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${activeQuests.size} pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (activeQuests.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (completedQuests.isNotEmpty()) "All Today's Quests Completed!" else "All clear",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (completedQuests.isNotEmpty())
                                "Amazing discipline! You cleared every objective for today."
                            else
                                "Tap a preset above or + on Tasks to schedule your day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = activeQuests.take(6),
                key = { it.occurrence.id }
            ) { item ->
                CompactTaskRow(
                    title = item.task.title,
                    difficulty = item.task.difficulty,
                    timeMillis = item.occurrence.scheduledAt,
                    allowEarly = item.task.allowEarlyComplete,
                    onComplete = { taskViewModel.completeQuest(item.occurrence.id) }
                )
            }
        }
    }

    if (showDailyCongrats) {
        AlertDialog(
            onDismissRequest = { showDailyCongrats = false },
            containerColor = DarkCard,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "DAILY GOALS COMPLETE!",
                        color = PrimaryManaBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Congratulations! You have completed all scheduled tasks for today (${completedQuests.size} cleared).",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Keep your streak alive — your task board will refresh cleanly at 12:00 AM midnight.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDailyCongrats = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryManaBlue,
                        contentColor = TextOnBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CELEBRATE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun PresetChipCard(
    preset: RespectfulHabitPreset,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onAdd() }
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(BadgeBg)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = preset.categoryLabel,
                color = BadgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = preset.title,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = preset.subtitle,
            color = TextSecondary,
            fontSize = 10.sp,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun StatusCard(stats: PlayerStatsEntity) {
    val rank = when {
        stats.level >= 20 -> "S"
        stats.level >= 15 -> "A"
        stats.level >= 10 -> "B"
        stats.level >= 5 -> "C"
        stats.level >= 3 -> "D"
        else -> "E"
    }
    val hpFraction = (stats.health.coerceIn(0, 100)) / 100f
    val hpColor = when {
        stats.health >= 60 -> SuccessGreen
        stats.health >= 30 -> AccentGold
        else -> DangerPenaltyRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(DarkCard, DarkSurface)
                )
            )
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryManaBlue.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryManaBlue, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank,
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryManaBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LEVEL ${stats.level}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${stats.ep} EP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat(
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = "${stats.streak}d",
                    tint = AccentGold
                )
                MiniStat(
                    icon = Icons.Rounded.TaskAlt,
                    value = "${stats.totalCompleted}",
                    tint = SuccessGreen
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("HEALTH", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
            Text("${stats.health} / 100", style = MaterialTheme.typography.labelSmall, color = hpColor, fontSize = 10.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSurface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(hpFraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hpColor)
            )
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, value: String, tint: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, color = TextPrimary, fontSize = 11.sp, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CompactTaskRow(
    title: String,
    difficulty: com.neurasamu.build.sl_tasker.data.model.Difficulty,
    timeMillis: Long,
    allowEarly: Boolean,
    onComplete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(timeMillis))

    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timeMillis) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(30_000L)
        }
    }
    val diffMs = timeMillis - nowMs
    val countdownStr = when {
        diffMs <= 0L -> "NOW"
        diffMs < 60_000L -> "<1 min"
        diffMs < 60L * 60_000L -> "${diffMs / 60_000L} min"
        diffMs < 24L * 60L * 60_000L -> "${diffMs / (60L * 60_000L)} h"
        else -> "${diffMs / (24L * 60L * 60_000L)} d"
    }

    val (diffColor, diffLabel) = when (difficulty) {
        com.neurasamu.build.sl_tasker.data.model.Difficulty.MEDIUM ->
            com.neurasamu.build.sl_tasker.ui.theme.RankC to "MEDIUM"
        com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD ->
            com.neurasamu.build.sl_tasker.ui.theme.RankA to "HARD"
        com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL ->
            com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed to "CRITICAL"
    }

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
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(diffColor.copy(alpha = 0.15f))
                        .border(1.dp, diffColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = diffLabel,
                        color = diffColor,
                        fontSize = 9.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$timeStr · $countdownStr",
                    color = if (diffMs <= 0L) PrimaryManaBlue else TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
        val canComplete = allowEarly || System.currentTimeMillis() >= timeMillis
        if (canComplete) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryManaBlue)
                    .clickable { onComplete() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Done",
                    color = TextOnBlue,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
