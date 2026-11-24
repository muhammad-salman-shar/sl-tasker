package com.neurasamu.build.sl_tasker.ui.dashboard

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PaywallTrigger
import com.neurasamu.build.sl_tasker.ui.components.CreateQuestDialog
import com.neurasamu.build.sl_tasker.ui.components.HunterHud
import com.neurasamu.build.sl_tasker.ui.components.QuestCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    val activeQuests by taskViewModel.activeQuests.collectAsStateWithLifecycle()
    val playerStats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<com.neurasamu.build.sl_tasker.data.model.TaskEntity?>(null) }
    var subTab by remember { mutableStateOf(0) }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    val taskTypes by taskViewModel.taskTypes.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val blockPrefs = remember { BlockPrefs(ctx) }
    val blockState by blockPrefs.state.collectAsStateWithLifecycle(initialValue = com.neurasamu.build.sl_tasker.data.block.BlockState())
    val todaySteps by com.neurasamu.build.sl_tasker.util.StepTrackerManager.todaySteps.collectAsStateWithLifecycle()
    var pendingQrTaskItem by remember { mutableStateOf<com.neurasamu.build.sl_tasker.ui.viewmodel.TaskOccurrenceItem?>(null) }
    val scope = rememberCoroutineScope()
    val completedQuests by taskViewModel.completedQuests.collectAsStateWithLifecycle()

    fun launchTaskTimer(item: com.neurasamu.build.sl_tasker.ui.viewmodel.TaskOccurrenceItem) {
        try {
            val accent = when (item.task.difficulty) {
                com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL -> 0xFFFF1744.toInt()
                com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD -> 0xFFFB923C.toInt()
                else -> 0xFF00E5FF.toInt()
            }
            val timer = com.neurasamu.build.sl_tasker.data.timer.ActiveTimer(
                occurrenceId = item.occurrence.id,
                taskTitle = item.task.title,
                totalSeconds = (item.task.durationMinutes.coerceAtLeast(1)) * 60,
                startedAtMillis = System.currentTimeMillis(),
                elapsedBeforePauseSeconds = 0,
                isPaused = false,
                accentColorArgb = accent,
                durationMinutes = item.task.durationMinutes.coerceAtLeast(1),
                stepGoal = item.task.stepGoal
            )
            val svc = Intent(ctx, com.neurasamu.build.sl_tasker.service.TimerService::class.java)
            svc.action = com.neurasamu.build.sl_tasker.service.TimerService.ACTION_START
            svc.putExtra(com.neurasamu.build.sl_tasker.service.TimerService.EXTRA_OCC_ID, timer.occurrenceId)
            svc.putExtra(com.neurasamu.build.sl_tasker.service.TimerService.EXTRA_TITLE, timer.taskTitle)
            svc.putExtra(com.neurasamu.build.sl_tasker.service.TimerService.EXTRA_MINUTES, timer.durationMinutes)
            svc.putExtra(com.neurasamu.build.sl_tasker.service.TimerService.EXTRA_ACCENT, timer.accentColorArgb)
            svc.putExtra(com.neurasamu.build.sl_tasker.service.TimerService.EXTRA_STEP_GOAL, timer.stepGoal)
            androidx.core.content.ContextCompat.startForegroundService(ctx, svc)
            ctx.startActivity(
                Intent(ctx, com.neurasamu.build.sl_tasker.ui.timer.TimerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {}
    }

    // Automatic 12:00 AM midnight clean-slate trigger while app is open
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val nextMidnight = TaskRepository.nextMidnightMillis(now)
            val waitMs = (nextMidnight - now + 1_500L).coerceAtLeast(5_000L)
            delay(waitMs)
            taskViewModel.runCleanup()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            if (subTab != 2) Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(PrimaryManaBlue.copy(alpha = 0.9f), PrimaryManaBlue.copy(alpha = 0.6f))
                        )
                    )
                    .border(1.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable {
                        if (!LicenseManager.isPro() && activeQuests.size >= 2) {
                            PaywallTrigger.show("Free tier allows up to 2 active tasks. Upgrade to Pro for unlimited tasks.")
                        } else {
                            showCreateDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 26.sp,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            HunterHud(stats = playerStats)

            if (playerStats.health in 1..49) {
                val needed = com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.TARGET_EP
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(com.neurasamu.build.sl_tasker.ui.theme.WarningOrange.copy(alpha = 0.15f))
                        .border(1.dp, com.neurasamu.build.sl_tasker.ui.theme.WarningOrange, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "⚠ PENALTY DUNGEON",
                            color = com.neurasamu.build.sl_tasker.ui.theme.WarningOrange,
                            fontSize = 11.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Health critical. Complete tasks to earn $needed EP and restore ${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE} HP.",
                            color = if (ThemeState.isLight) androidx.compose.ui.graphics.Color(0xFF64748B) else TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCard)
                    .border(1.dp, com.neurasamu.build.sl_tasker.ui.theme.DarkBorder, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("INCOMPLETE", "COMPLETED", "CALENDAR").forEachIndexed { idx, label ->
                    val sel = subTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) PrimaryManaBlue.copy(alpha = 0.15f) else DarkCard)
                            .border(1.dp, if (sel) PrimaryManaBlue else com.neurasamu.build.sl_tasker.ui.theme.DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { subTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (sel) PrimaryManaBlue else TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LaunchedEffect(taskTypes) {
                val cur = selectedTypeFilter
                if (cur != null && taskTypes.none { it.equals(cur, ignoreCase = true) }) {
                    selectedTypeFilter = null
                }
            }

            if (subTab == 2) {
                com.neurasamu.build.sl_tasker.ui.calendar.CalendarScreen(
                    taskViewModel = taskViewModel,
                    modifier = Modifier.fillMaxSize()
                )
                return@Column
            }

            val rawList = if (subTab == 0) activeQuests else completedQuests

            if (taskTypes.isNotEmpty()) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "__all__") {
                        TypeChipSmall(
                            label = "All",
                            count = rawList.size,
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null }
                        )
                    }
                    items(
                        items = taskTypes,
                        key = { it.lowercase() }
                    ) { ty ->
                        val catCount = rawList.count { it.task.userType.equals(ty, ignoreCase = true) }
                        TypeChipSmall(
                            label = ty,
                            count = catCount,
                            selected = selectedTypeFilter.equals(ty, ignoreCase = true),
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter.equals(ty, ignoreCase = true)) null else ty
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val currentList = if (selectedTypeFilter == null) rawList
            else rawList.filter {
                it.task.userType.equals(selectedTypeFilter, ignoreCase = true)
            }
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                selectedTypeFilter != null -> "NO ${selectedTypeFilter!!.uppercase()} QUESTS"
                                subTab == 1 -> "NO COMPLETED QUESTS TODAY"
                                else -> "NO ACTIVE QUESTS"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                selectedTypeFilter != null -> "Tap + to add a task in $selectedTypeFilter or switch category."
                                subTab == 1 -> "Completed tasks clear automatically at 12:00 AM midnight."
                                else -> "All daily system objectives cleared. Stand by for awakening."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            } else if (selectedTypeFilter != null) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        items = currentList,
                        key = { it.occurrence.id }
                    ) { item ->
                        CategoryGlowSwipeCard(
                            item = item,
                            onStart = {
                                if (item.task.isAdvanced && item.task.requireQrProof) {
                                    pendingQrTaskItem = item
                                } else {
                                    launchTaskTimer(item)
                                }
                            },
                            onComplete = {
                                if (item.task.isAdvanced && item.task.requireQrProof) {
                                    pendingQrTaskItem = item
                                } else {
                                    taskViewModel.completeQuest(item.occurrence.id)
                                }
                            },
                            onEdit = { editingTask = item.task },
                            onDelete = { taskViewModel.deleteTask(item.task.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = currentList,
                        key = { it.occurrence.id }
                    ) { item ->
                        QuestCard(
                            task = item.task,
                            occurrence = item.occurrence,
                            todaySteps = todaySteps,
                            onComplete = {
                                if (item.task.isAdvanced && item.task.stepGoal > 0 && todaySteps < item.task.stepGoal) {
                                    val rem = item.task.stepGoal - todaySteps
                                    android.widget.Toast.makeText(
                                        ctx,
                                        "Walk $rem more steps to unlock completion!",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else if (item.task.isAdvanced && item.task.requireQrProof) {
                                    pendingQrTaskItem = item
                                } else {
                                    taskViewModel.completeQuest(item.occurrence.id)
                                }
                            },
                            onStart = {
                                if (item.task.isAdvanced && item.task.requireQrProof) {
                                    pendingQrTaskItem = item
                                } else {
                                    launchTaskTimer(item)
                                }
                            },
                            onEdit = { editingTask = item.task },
                            onDelete = { taskViewModel.deleteTask(item.task.id) }
                        )
                    }
                }
            }
        }
    }

    pendingQrTaskItem?.let { qrItem ->
        com.neurasamu.build.sl_tasker.ui.components.QrScannerDialog(
            title = "PROOF-OF-WORK SCAN",
            subtitle = if (blockState.hasRegisteredSeal)
                "Scan your registered seal (${blockState.sealLabel}) to verify \"${qrItem.task.title}\""
            else
                "Scan any physical QR or barcode to verify \"${qrItem.task.title}\"",
            expectedCode = if (blockState.hasRegisteredSeal) blockState.sealCode else null,
            onDismiss = { pendingQrTaskItem = null },
            onScanned = {
                pendingQrTaskItem = null
                if (qrItem.task.difficulty != com.neurasamu.build.sl_tasker.data.model.Difficulty.MEDIUM) {
                    launchTaskTimer(qrItem)
                } else {
                    taskViewModel.completeQuest(qrItem.occurrence.id)
                }
            }
        )
    }

    if (showCreateDialog) {
        CreateQuestDialog(
            onDismiss = { showCreateDialog = false },
            initialType = selectedTypeFilter ?: "",
            existingTypes = taskTypes,
            onAddType = { taskViewModel.addTaskType(it) },
            onDeleteType = { taskViewModel.removeTaskType(it) },
            onConfirm = { title, difficulty, reminderMinutesOfDay, daysCsv, durationMinutes, isRepeat, allowEarly, userType, scheduleMode, validityDays, isAdvanced, hpDropEnabled, requireQrProof, stepGoal ->
                var blocked = false
                if (!LicenseManager.isPro()) {
                    when (difficulty) {
                        com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD -> {
                            if (LicenseManager.hasUsedHardTrial(ctx)) {
                                PaywallTrigger.show("Your free Hard task trial is used. Upgrade to Pro for unlimited.")
                                blocked = true
                            } else {
                                LicenseManager.markHardTrialUsed(ctx)
                            }
                        }
                        com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL -> {
                            if (LicenseManager.hasUsedCriticalTrial(ctx)) {
                                PaywallTrigger.show("Your free Critical task trial is used. Upgrade to Pro for unlimited.")
                                blocked = true
                            } else {
                                LicenseManager.markCriticalTrialUsed(ctx)
                            }
                        }
                        else -> {}
                    }
                }
                if (blocked) return@CreateQuestDialog
                taskViewModel.createQuest(
                    title = title,
                    description = "",
                    difficulty = difficulty,
                    reminderMinutesOfDay = reminderMinutesOfDay,
                    customRepeatDays = daysCsv,
                    durationMinutes = durationMinutes,
                    isRepeat = isRepeat,
                    allowEarlyComplete = allowEarly,
                    userType = userType,
                    scheduleMode = scheduleMode,
                    validityDays = validityDays,
                    isAdvanced = isAdvanced,
                    hpDropEnabled = hpDropEnabled,
                    requireQrProof = requireQrProof,
                    stepGoal = stepGoal
                )
                showCreateDialog = false
            }
        )
    }

    editingTask?.let { task ->
        CreateQuestDialog(
            onDismiss = { editingTask = null },
            initialTask = task,
            existingTypes = taskTypes,
            onAddType = { taskViewModel.addTaskType(it) },
            onDeleteType = { taskViewModel.removeTaskType(it) },
            onConfirm = { title, difficulty, reminderMinutesOfDay, daysCsv, durationMinutes, isRepeat, allowEarly, userType, scheduleMode, validityDays, isAdvanced, hpDropEnabled, requireQrProof, stepGoal ->
                taskViewModel.updateTaskFields(
                    taskId = task.id,
                    title = title,
                    description = "",
                    difficulty = difficulty,
                    reminderMinutesOfDay = reminderMinutesOfDay,
                    customRepeatDays = daysCsv,
                    durationMinutes = durationMinutes,
                    isRepeat = isRepeat,
                    allowEarlyComplete = allowEarly,
                    userType = userType,
                    scheduleMode = scheduleMode,
                    validityDays = validityDays,
                    isAdvanced = isAdvanced,
                    hpDropEnabled = hpDropEnabled,
                    requireQrProof = requireQrProof,
                    stepGoal = stepGoal
                )
                editingTask = null
            }
        )
    }
}

@Composable
private fun TypeChipSmall(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val isLight = ThemeState.isLight
    val accent = com.neurasamu.build.sl_tasker.ui.theme.AccentGold
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) accent.copy(alpha = 0.22f)
                else if (isLight) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                else com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
            )
            .border(
                1.dp,
                if (selected) accent else com.neurasamu.build.sl_tasker.ui.theme.DarkBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            label,
            color = if (selected) accent else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
        )
        if (count > 0) {
            Text(
                "$count",
                color = if (selected) accent else TextMuted.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
    }
}


@Composable
private fun CategoryGlowSwipeCard(
    item: com.neurasamu.build.sl_tasker.ui.viewmodel.TaskOccurrenceItem,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val diffColor = when (item.task.difficulty) {
        com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL -> DangerPenaltyRed
        com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD -> com.neurasamu.build.sl_tasker.ui.theme.AccentGold
        com.neurasamu.build.sl_tasker.data.model.Difficulty.MEDIUM -> PrimaryManaBlue
    }
    val diffMs = item.occurrence.scheduledAt - System.currentTimeMillis()
    val timeLeftLabel = when {
        diffMs <= 0L -> "DUE NOW"
        diffMs < 60_000L -> "<1m LEFT"
        diffMs < 60L * 60_000L -> "${diffMs / 60_000L}m LEFT"
        diffMs < 24L * 60L * 60_000L -> "${diffMs / (60L * 60_000L)}h LEFT"
        else -> "${diffMs / (24L * 60L * 60_000L)}d LEFT"
    }
    val needsTimer = item.task.difficulty != com.neurasamu.build.sl_tasker.data.model.Difficulty.MEDIUM ||
        item.task.stepGoal > 0
    val durationInfo = when {
        item.task.stepGoal > 0 -> "Target: ${item.task.stepGoal} Steps"
        item.task.durationMinutes > 0 -> "Duration: ${item.task.durationMinutes} min"
        else -> "Instant Quest"
    }

    Column(
        modifier = Modifier
            .width(264.dp)
            .height(294.dp)
            .androidx.compose.ui.draw.shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = diffColor.copy(alpha = 0.50f),
                spotColor = diffColor.copy(alpha = 0.50f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(DarkCard, DarkSurface)
                )
            )
            .border(1.5.dp, diffColor.copy(alpha = 0.70f), RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top-Left: Time Left + Top-Right: Difficulty
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(diffColor.copy(alpha = 0.16f))
                    .border(1.dp, diffColor.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeLeftLabel,
                    color = diffColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = item.task.difficulty.name,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Middle: Bold Task Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.task.title,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 3
            )
            if (item.task.userType.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.task.userType.uppercase(),
                    color = PrimaryManaBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Bottom: Duration/Steps above Start/Complete button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = durationInfo,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = if (needsTimer) onStart else onComplete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (needsTimer) diffColor else com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (needsTimer) "START QUEST" else "COMPLETE",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "Edit",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onEdit() }
                )
                Text(
                    text = "Delete",
                    color = DangerPenaltyRed.copy(alpha = 0.85f),
                    fontSize = 10.sp,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}
