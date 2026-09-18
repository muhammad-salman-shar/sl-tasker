package com.neurasamu.build.solo_leveling_tasker.ui.components

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.Priority
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskEntity
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBorder
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkCard
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkSurface
import com.neurasamu.build.solo_leveling_tasker.ui.theme.GoldWarning
import com.neurasamu.build.solo_leveling_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankA
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankC
import com.neurasamu.build.solo_leveling_tasker.ui.theme.RankD
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextMuted
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextPrimary
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuestCard(
    task: TaskEntity,
    occurrence: OccurrenceEntity,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val deadlineStr = dateFormat.format(Date(occurrence.deadlineAt))

    val priorityColor = when (task.priority) {
        Priority.CRITICAL -> DangerPenaltyRed
        Priority.HIGH -> GoldWarning
        Priority.MEDIUM -> PrimaryManaBlue
        Priority.LOW -> TextMuted
    }

    val difficultyColor = when (task.difficulty) {
        Difficulty.EXTREME -> DangerPenaltyRed
        Difficulty.HARD -> RankA
        Difficulty.NORMAL -> RankC
        Difficulty.EASY -> RankD
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
            .padding(14.dp)
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
                            text = "${task.difficulty.name} (+${task.difficulty.baseEp} EP)",
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

                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryManaBlue,
                        contentColor = DarkCard
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "COMPLETE QUEST",
                        style = MaterialTheme.typography.labelLarge,
                        color = DarkCard
                    )
                }
            }
        }
    }
}
