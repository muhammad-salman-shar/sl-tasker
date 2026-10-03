package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.IconDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.sl_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.sl_tasker.data.model.Priority
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.RankA
import com.neurasamu.build.sl_tasker.ui.theme.RankC
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestCard(
    task: TaskEntity,
    occurrence: OccurrenceEntity,
    onComplete: () -> Unit,
    onStart: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val deadlineStr = dateFormat.format(Date(occurrence.deadlineAt))

    val priorityColor = when (task.priority) {
        Priority.CRITICAL -> DangerPenaltyRed
        Priority.HIGH -> GoldWarning
        Priority.MEDIUM -> PrimaryManaBlue
        Priority.LOW -> TextMuted
    }

    val difficultyColor = when (task.difficulty) {
        Difficulty.CRITICAL -> DangerPenaltyRed
        Difficulty.HARD -> RankA
        Difficulty.MEDIUM -> RankC
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(
                1.dp,
                if (task.priority == Priority.CRITICAL) DangerPenaltyRed else DarkBorder,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onClick = {},
                onLongClick = { menuOpen = true }
            )
            .padding(14.dp)
    ) {
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit", color = TextPrimary) },
                onClick = { menuOpen = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = DangerPenaltyRed) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .border(1.dp, priorityColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurface)
                            .border(1.dp, difficultyColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${task.difficulty.name} (+${when(task.difficulty){ com.neurasamu.build.sl_tasker.data.model.Difficulty.MEDIUM -> 10; com.neurasamu.build.sl_tasker.data.model.Difficulty.HARD -> 30; com.neurasamu.build.sl_tasker.data.model.Difficulty.CRITICAL -> 50 }} EP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = "DUE: $deadlineStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DURATION: ${task.durationMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                val isCompleted = occurrence.status == OccurrenceStatus.COMPLETED
                if (isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen.copy(alpha = 0.75f),
                            fontSize = 11.sp
                        )
                    }
                } else {
                    val needsTimer = task.difficulty != Difficulty.MEDIUM
                    Button(
                        onClick = if (needsTimer) onStart else onComplete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (needsTimer) difficultyColor else PrimaryManaBlue,
                            contentColor = DarkCard
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (needsTimer) "START" else "COMPLETE QUEST",
                            style = MaterialTheme.typography.labelLarge,
                            color = DarkCard
                        )
                    }
                }
            }
        }
    }
}
