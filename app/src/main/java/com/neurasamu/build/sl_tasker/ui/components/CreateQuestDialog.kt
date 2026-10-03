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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.RankA
import com.neurasamu.build.sl_tasker.ui.theme.RankC
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary

private val DayShort = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    initialTask: com.neurasamu.build.sl_tasker.data.model.TaskEntity? = null,
    onConfirm: (
        title: String,
        description: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        daysOfWeekCsv: String,
        durationMinutes: Int
    ) -> Unit
) {
    val isEdit = initialTask != null
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var difficulty by remember { mutableStateOf(initialTask?.difficulty ?: Difficulty.MEDIUM) }
    var durationText by remember {
        mutableStateOf(if ((initialTask?.durationMinutes ?: 0) > 0) initialTask!!.durationMinutes.toString() else "30")
    }
    val initHour = (initialTask?.reminderMinutesOfDay ?: 420) / 60
    val initMinute = (initialTask?.reminderMinutesOfDay ?: 420) % 60
    var hourText by remember { mutableStateOf("%02d".format(if (initHour % 12 == 0) 12 else initHour % 12)) }
    var minuteText by remember { mutableStateOf("%02d".format(initMinute)) }
    var is24h by remember { mutableStateOf(false) }
    var amPm by remember { mutableStateOf(if (initHour < 12) "AM" else "PM") }
    var selectedDays by remember {
        mutableStateOf(
            initialTask?.customRepeatDays
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet() ?: emptySet()
        )
    }
    var isRepeat by remember {
        mutableStateOf(initialTask?.repeatRule != com.neurasamu.build.sl_tasker.data.model.RepeatRule.ONCE)
    }

    val difficultyColor = when (difficulty) {
        Difficulty.MEDIUM -> RankC
        Difficulty.HARD -> RankA
        Difficulty.CRITICAL -> DangerPenaltyRed
    }
    val requiresTimer = difficulty != Difficulty.MEDIUM

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = if (isEdit) "EDIT TASK" else "NEW TASK",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryManaBlue,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("Title (max 50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryManaBlue,
                        unfocusedLabelColor = TextMuted
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 100) description = it },
                    label = { Text("Description (max 100)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = PrimaryManaBlue,
                        unfocusedLabelColor = TextMuted
                    )
                )

                SectionLabel("TASK TYPE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Difficulty.entries.forEach { d ->
                        val isSel = difficulty == d
                        val c = when (d) {
                            Difficulty.MEDIUM -> RankC
                            Difficulty.HARD -> RankA
                            Difficulty.CRITICAL -> DangerPenaltyRed
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) c.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSel) c else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { difficulty = d }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d.name,
                                color = if (isSel) c else TextMuted,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                SectionLabel("SCHEDULE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(false to "Single", true to "Repeat").forEach { (flag, label) ->
                        val isSel = isRepeat == flag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSel) PrimaryManaBlue else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { isRepeat = flag }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) PrimaryManaBlue else TextMuted,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (isRepeat) {
                SectionLabel("DAYS (pick one or more)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DayShort.forEachIndexed { index, label ->
                        val isSel = index in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSel) PrimaryManaBlue else DarkBorder, CircleShape)
                                .clickable {
                                    selectedDays = if (isSel) selectedDays - index else selectedDays + index
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) PrimaryManaBlue else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("TIME")
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    ) {
                        listOf(false to "12h", true to "24h").forEach { (flag, label) ->
                            val sel = is24h == flag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                    .clickable { is24h = flag }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (sel) PrimaryManaBlue else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { v ->
                            hourText = v.filter { it.isDigit() }.take(2)
                        },
                        label = { Text(if (is24h) "HH" else "HH (1-12)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryManaBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                    Text(":", color = TextPrimary, fontSize = 18.sp)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { v ->
                            minuteText = v.filter { it.isDigit() }.take(2)
                        },
                        label = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryManaBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                    if (!is24h) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        ) {
                            listOf("AM", "PM").forEach { label ->
                                val sel = amPm == label
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                        .clickable { amPm = label }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        label,
                                        color = if (sel) PrimaryManaBlue else TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (requiresTimer) {
                    SectionLabel("TIMER (MINUTES)")
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("e.g. 60") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = difficultyColor,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = difficultyColor,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                }
            }
        },
        confirmButton = {
            val valid = title.isNotBlank() &&
                (!isRepeat || selectedDays.isNotEmpty()) &&
                (!requiresTimer || (durationText.toIntOrNull() ?: 0) > 0)
            Button(
                onClick = {
                    val rawH = (hourText.toIntOrNull() ?: 0)
                    val mm = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val hh = if (is24h) {
                        rawH.coerceIn(0, 23)
                    } else {
                        val h12 = if (rawH == 0) 12 else rawH.coerceIn(1, 12)
                        when {
                            amPm == "AM" && h12 == 12 -> 0
                            amPm == "PM" && h12 != 12 -> h12 + 12
                            else -> h12
                        }
                    }
                    val minutes = hh * 60 + mm
                    val csv = if (isRepeat) selectedDays.sorted().joinToString(",") else ""
                    val dur = if (requiresTimer) durationText.toIntOrNull() ?: 0 else 0
                    onConfirm(title, description, difficulty, minutes, csv, dur)
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = difficultyColor,
                    contentColor = DarkCard
                )
            ) {
                Text(if (isEdit) "SAVE" else "CREATE", fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted, fontSize = 12.sp)
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontSize = 10.sp
    )
}
