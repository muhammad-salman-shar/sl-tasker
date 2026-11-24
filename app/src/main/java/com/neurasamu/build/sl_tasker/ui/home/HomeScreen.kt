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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskOccurrenceItem
import com.neurasamu.build.sl_tasker.data.timer.ActiveTimer
import com.neurasamu.build.sl_tasker.data.timer.TimerRuntime
import com.neurasamu.build.sl_tasker.service.TimerService
import com.neurasamu.build.sl_tasker.ui.components.DailyQuestSection
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
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
import com.neurasamu.build.sl_tasker.ui.timer.TimerActivity
import com.neurasamu.build.sl_tasker.ui.viewmodel.DailyQuestViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import com.neurasamu.build.sl_tasker.util.SoundFx
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class EssentialDailyHabit(
    val id: String,
    val badge: String,
    val title: String,
    val subtitle: String,
    val bonusLabel: String
)

private val ESSENTIAL_HABITS = listOf(
    EssentialDailyHabit(
        id = "essential_hydrate",
        badge = "ESSENTIAL",
        title = "Hydration & Clean Morning",
        subtitle = "Drink 2L water & wash up fresh",
        bonusLabel = "+10 HP · +15 EP"
    ),
    EssentialDailyHabit(
        id = "essential_focus",
        badge = "ESSENTIAL",
        title = "Deep Focus / Reading",
        subtitle = "20 mins distraction-free learning",
        bonusLabel = "+10 HP · +15 EP"
    ),
    EssentialDailyHabit(
        id = "essential_move",
        badge = "OPTIONAL +PTS",
        title = "Walk & Posture Stretch",
        subtitle = "1,000 steps or 10m mobility",
        bonusLabel = "+15 HP · +20 EP"
    )
)

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
    val dailyQuests by dailyQuestViewModel.quests.collectAsStateWithLifecycle()
    val incompleteDailyQuests = remember(dailyQuests) { dailyQuests.filter { !it.completed } }

    var showDailyCongrats by remember { mutableStateOf(false) }
    var viewingItem by remember { mutableStateOf<TaskOccurrenceItem?>(null) }

    val cal = remember { Calendar.getInstance() }
    val dayEpochKey = remember { "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}" }
    val essentialPrefs = remember { ctx.getSharedPreferences("sl_essential_habits", Context.MODE_PRIVATE) }
    var completedEssentialIds by remember {
        mutableStateOf(
            ESSENTIAL_HABITS.filter { essentialPrefs.getBoolean("${dayEpochKey}_${it.id}", false) }
                .map { it.id }
                .toSet()
        )
    }

    LaunchedEffect(activeQuests.size, completedQuests.size) {
        if (activeQuests.isEmpty() && completedQuests.isNotEmpty()) {
            val prefs = ctx.getSharedPreferences("sl_daily_congrats", Context.MODE_PRIVATE)
            if (!prefs.getBoolean(dayEpochKey, false)) {
                prefs.edit().putBoolean(dayEpochKey, true).apply()
                showDailyCongrats = true
                try { SoundFx.levelUp() } catch (_: Throwable) {}
            }
        }
    }

    fun startTimerFor(item: TaskOccurrenceItem) {
        val mins = item.task.durationMinutes.coerceAtLeast(1)
        val accent = when (item.task.difficulty) {
            Difficulty.CRITICAL -> 0xFFFF3B5C.toInt()
            Difficulty.HARD -> 0xFFFFB800.toInt()
            else -> 0xFF00E5FF.toInt()
        }
        val activeTimer = ActiveTimer(
            occurrenceId = item.occurrence.id,
            taskTitle = item.task.title,
            totalSeconds = mins * 60,
            startedAtMillis = System.currentTimeMillis(),
            elapsedBeforePauseSeconds = 0,
            isPaused = false,
            accentColorArgb = accent,
            durationMinutes = mins,
            stepGoal = if (item.task.isAdvanced) item.task.stepGoal else 0
        )
        TimerRuntime.set(activeTimer)
        try {
            val svc = TimerService.startIntent(ctx, activeTimer).apply {
                putExtra(TimerService.EXTRA_STEP_GOAL, activeTimer.stepGoal)
            }
            ContextCompat.startForegroundService(ctx, svc)
        } catch (_: Throwable) {}
        try {
            ctx.startActivity(
                android.content.Intent(ctx, TimerActivity::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Throwable) {}
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
        if (incompleteDailyQuests.isNotEmpty()) {
            item {
                DailyQuestSection(
                    quests = incompleteDailyQuests,
                    onComplete = { id -> dailyQuestViewModel.complete(id) }
                )
            }
        }

        // 3. 3 Essential Daily Life Habits (Swipeable cards with bottom Complete button)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY ESSENTIALS",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryManaBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${completedEssentialIds.size} / 3 cleared",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items = ESSENTIAL_HABITS, key = { it.id }) { habit ->
                        val isDone = habit.id in completedEssentialIds
                        EssentialHabitCard(
                            habit = habit,
                            isCompleted = isDone,
                            onComplete = {
                                if (!isDone) {
                                    essentialPrefs.edit().putBoolean("${dayEpochKey}_${habit.id}", true).apply()
                                    completedEssentialIds = completedEssentialIds + habit.id
                                    val hpGain = if (habit.id == "essential_move") 15 else 10
                                    val epGain = if (habit.id == "essential_move") 20 else 15
                                    statsViewModel.awardBonus(hp = hpGain, ep = epGain)
                                    try { SoundFx.complete() } catch (_: Throwable) {}
                                    Toast.makeText(
                                        ctx,
                                        "Completed ${habit.title}! (${habit.bonusLabel})",
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
                                "Tap + on Tasks to schedule your day.",
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
                    item = item,
                    onView = { viewingItem = item }
                )
            }
        }
    }

    // Glowing Task Detail Modal when user clicks "View" on Home Screen
    viewingItem?.let { item ->
        val diffColor = when (item.task.difficulty) {
            Difficulty.CRITICAL -> DangerPenaltyRed
            Difficulty.HARD -> AccentGold
            Difficulty.MEDIUM -> PrimaryManaBlue
        }
        val needsTimer = item.task.difficulty != Difficulty.MEDIUM ||
            (item.task.isAdvanced && item.task.stepGoal > 0)
        val canAct = item.task.allowEarlyComplete || System.currentTimeMillis() >= item.occurrence.scheduledAt
        val timeFmt = remember { SimpleDateFormat("hh:mm a · dd MMM", Locale.getDefault()) }

        Dialog(onDismissRequest = { viewingItem = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(22.dp), ambientColor = diffColor, spotColor = diffColor)
                    .clip(RoundedCornerShape(22.dp))
                    .background(DarkCard)
                    .border(1.5.dp, diffColor.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(diffColor.copy(alpha = 0.16f))
                            .border(1.dp, diffColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.task.difficulty.name,
                            color = diffColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        text = timeFmt.format(Date(item.occurrence.scheduledAt)),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = item.task.title,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                if (item.task.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.task.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(10.dp))

                val metaLabel = buildString {
                    if (item.task.isAdvanced && item.task.stepGoal > 0) {
                        append("Step Target: ${item.task.stepGoal} steps")
                    } else if (item.task.durationMinutes > 0) {
                        append("Focus Duration: ${item.task.durationMinutes} min")
                    } else {
                        append("Instant Check-Off Task")
                    }
                    if (item.task.difficulty == Difficulty.HARD) append(" · +40 HP")
                    if (item.task.difficulty == Difficulty.CRITICAL) append(" · +50 HP")
                }
                Text(
                    text = metaLabel,
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        val target = item
                        viewingItem = null
                        if (needsTimer) {
                            startTimerFor(target)
                        } else {
                            taskViewModel.completeQuest(target.occurrence.id)
                        }
                    },
                    enabled = canAct,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (needsTimer) diffColor else SuccessGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (needsTimer) Icons.Rounded.PlayArrow else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (!canAct) "LOCKED UNTIL SCHEDULED TIME" else if (needsTimer) "START TASK" else "COMPLETE TASK",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { viewingItem = null }) {
                    Text("CLOSE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
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
private fun EssentialHabitCard(
    habit: EssentialDailyHabit,
    isCompleted: Boolean,
    onComplete: () -> Unit
) {
    val glowColor = if (isCompleted) SuccessGreen else PrimaryManaBlue
    Column(
        modifier = Modifier
            .width(168.dp)
            .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = glowColor.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .border(1.dp, glowColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(BadgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = habit.badge,
                        color = BadgeText,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = habit.bonusLabel,
                    color = SuccessGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = habit.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = habit.subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCompleted) SuccessGreen.copy(alpha = 0.18f) else PrimaryManaBlue)
                .border(1.dp, if (isCompleted) SuccessGreen else PrimaryManaBlue, RoundedCornerShape(8.dp))
                .clickable(enabled = !isCompleted) { onComplete() }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isCompleted) "COMPLETED ✓" else "COMPLETE",
                color = if (isCompleted) SuccessGreen else TextOnBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
        }
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
    item: TaskOccurrenceItem,
    onView: () -> Unit
) {
    val timeMillis = item.occurrence.scheduledAt
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

    val (diffColor, diffLabel) = when (item.task.difficulty) {
        Difficulty.MEDIUM -> com.neurasamu.build.sl_tasker.ui.theme.RankC to "MEDIUM"
        Difficulty.HARD -> com.neurasamu.build.sl_tasker.ui.theme.RankA to "HARD"
        Difficulty.CRITICAL -> DangerPenaltyRed to "CRITICAL"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onView() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.task.title,
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
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryManaBlue.copy(alpha = 0.16f))
                .border(1.dp, PrimaryManaBlue, RoundedCornerShape(8.dp))
                .clickable { onView() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "View",
                color = PrimaryManaBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
