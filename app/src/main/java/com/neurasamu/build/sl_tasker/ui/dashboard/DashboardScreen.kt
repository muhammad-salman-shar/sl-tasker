package com.neurasamu.build.sl_tasker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.ui.components.CreateQuestDialog
import com.neurasamu.build.sl_tasker.ui.components.HunterHud
import com.neurasamu.build.sl_tasker.ui.components.QuestCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel

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
    var timerItem by remember { mutableStateOf<com.neurasamu.build.sl_tasker.ui.viewmodel.TaskOccurrenceItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryManaBlue,
                contentColor = DarkCard,
                shape = CircleShape
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    color = DarkCard
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ACTIVE QUEST LOG",
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryManaBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (activeQuests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ACTIVE QUESTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All daily system objectives cleared. Stand by for awakening.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
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
                        items = activeQuests,
                        key = { it.occurrence.id }
                    ) { item ->
                        QuestCard(
                            task = item.task,
                            occurrence = item.occurrence,
                            onComplete = {
                                taskViewModel.completeQuest(item.occurrence.id)
                            },
                            onStart = { timerItem = item },
                            onEdit = { editingTask = item.task },
                            onDelete = { taskViewModel.deleteTask(item.task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateQuestDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, description, difficulty, reminderMinutesOfDay, daysCsv, durationMinutes ->
                taskViewModel.createQuest(
                    title = title,
                    description = description,
                    difficulty = difficulty,
                    reminderMinutesOfDay = reminderMinutesOfDay,
                    customRepeatDays = daysCsv,
                    durationMinutes = durationMinutes
                )
                showCreateDialog = false
            }
        )
    }

    timerItem?.let { item ->
        val accent = when (item.task.difficulty) {
            com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL -> com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
            com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD -> com.neurasamu.build.sl_tasker.ui.theme.RankA
            else -> com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
        }
        com.neurasamu.build.sl_tasker.ui.timer.TaskTimerScreen(
            title = item.task.title,
            durationMinutes = item.task.durationMinutes.coerceAtLeast(1),
            accentColor = accent,
            onFinish = {
                taskViewModel.completeQuest(item.occurrence.id)
                timerItem = null
            },
            onCancel = { timerItem = null }
        )
    }

    editingTask?.let { task ->
        CreateQuestDialog(
            onDismiss = { editingTask = null },
            initialTask = task,
            onConfirm = { title, description, difficulty, reminderMinutesOfDay, daysCsv, durationMinutes ->
                taskViewModel.updateTaskFields(
                    taskId = task.id,
                    title = title,
                    description = description,
                    difficulty = difficulty,
                    reminderMinutesOfDay = reminderMinutesOfDay,
                    customRepeatDays = daysCsv,
                    durationMinutes = durationMinutes
                )
                editingTask = null
            }
        )
    }
}
