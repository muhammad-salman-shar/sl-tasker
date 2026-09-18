package com.neurasamu.build.solo_leveling_tasker.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.Priority
import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
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

@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        priority: Priority,
        difficulty: Difficulty,
        repeatRule: RepeatRule,
        durationMinutes: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("30") }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var selectedDifficulty by remember { mutableStateOf(Difficulty.NORMAL) }
    var selectedRepeatRule by remember { mutableStateOf<RepeatRule>(RepeatRule.ONCE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "NEW SYSTEM QUEST",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryManaBlue
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationText = it },
                    label = { Text("Duration (Minutes)", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PRIORITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Priority.entries.forEach { priority ->
                        val isSelected = selectedPriority == priority
                        val pColor = when (priority) {
                            Priority.CRITICAL -> DangerPenaltyRed
                            Priority.HIGH -> GoldWarning
                            Priority.MEDIUM -> PrimaryManaBlue
                            Priority.LOW -> TextMuted
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) pColor.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSelected) pColor else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedPriority = priority }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priority.name.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) pColor else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "DIFFICULTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Difficulty.entries.forEach { difficulty ->
                        val isSelected = selectedDifficulty == difficulty
                        val dColor = when (difficulty) {
                            Difficulty.EXTREME -> DangerPenaltyRed
                            Difficulty.HARD -> RankA
                            Difficulty.NORMAL -> RankC
                            Difficulty.EASY -> RankD
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) dColor.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSelected) dColor else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedDifficulty = difficulty }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = difficulty.name.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) dColor else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "REPEAT SCHEDULE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RepeatRule.entries.forEach { rule ->
                        val isSelected = selectedRepeatRule == rule
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSelected) PrimaryManaBlue else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedRepeatRule = rule }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rule.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PrimaryManaBlue else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val duration = durationText.toIntOrNull() ?: 30
                        onConfirm(
                            title.trim(),
                            description.trim(),
                            selectedPriority,
                            selectedDifficulty,
                            selectedRepeatRule,
                            duration
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = DarkCard
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "INITIALIZE",
                    style = MaterialTheme.typography.labelLarge,
                    color = DarkCard
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }
        }
    )
}
