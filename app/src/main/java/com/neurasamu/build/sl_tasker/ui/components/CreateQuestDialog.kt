package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
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
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState

private val DayShort = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    initialTask: TaskEntity? = null,
    existingTypes: List<String> = emptyList(),
    onDeleteType: (String) -> Unit = {},
    onConfirm: (
        title: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        daysOfWeekCsv: String,
        durationMinutes: Int,
        isRepeat: Boolean,
        allowEarlyComplete: Boolean,
        userType: String
    ) -> Unit
) {
    val isEdit = initialTask != null
    val isLight = ThemeState.isLight

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
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
        mutableStateOf(initialTask != null && initialTask.repeatRule != com.neurasamu.build.sl_tasker.data.model.RepeatRule.ONCE)
    }
    var allowEarly by remember { mutableStateOf(initialTask?.allowEarlyComplete ?: false) }

    var selectedType by remember { mutableStateOf(initialTask?.userType ?: "") }
    var showNewTypeInput by remember { mutableStateOf(false) }
    var newTypeText by remember { mutableStateOf("") }
    val knownTypes = remember(existingTypes) { existingTypes.filter { it.isNotBlank() }.distinct() }

    val difficultyColor = when (difficulty) {
        Difficulty.MEDIUM -> RankC
        Difficulty.HARD -> RankA
        Difficulty.CRITICAL -> DangerPenaltyRed
    }
    val requiresTimer = difficulty != Difficulty.MEDIUM

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isLight)
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f)
        else
            DarkCard.copy(alpha = 0.95f),
        modifier = Modifier
            .shadow(24.dp, RoundedCornerShape(24.dp))
            .border(1.dp, if (isLight) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (isEdit) "EDIT TASK" else "NEW TASK",
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryManaBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("Title (max 50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryManaBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                        unfocusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                        focusedLabelColor = PrimaryManaBlue,
                        unfocusedLabelColor = TextMuted
                    )
                )

                // Type category
                SectionLabel("TASK TYPE")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        TypeChip(
                            label = "None",
                            selected = selectedType.isBlank(),
                            onClick = { selectedType = "" }
                        )
                    }
                    items(knownTypes) { t ->
                        TypeChip(
                            label = t,
                            selected = selectedType.equals(t, ignoreCase = true),
                            onClick = { selectedType = t },
                            onDelete = {
                                if (selectedType == t) selectedType = ""
                                onDeleteType(t)
                            }
                        )
                    }
                    item {
                        TypeChip(
                            label = "+ New",
                            selected = false,
                            onClick = { showNewTypeInput = !showNewTypeInput }
                        )
                    }
                }

                if (showNewTypeInput || knownTypes.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTypeText,
                            onValueChange = { if (it.length <= 20) newTypeText = it },
                            label = { Text("New type (e.g. Gym)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                                unfocusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                                focusedLabelColor = AccentGold,
                                unfocusedLabelColor = TextMuted
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val nt = newTypeText.trim()
                                if (nt.isNotBlank()) {
                                    selectedType = nt
                                    newTypeText = ""
                                    showNewTypeInput = false
                                }
                            },
                            enabled = newTypeText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGold,
                                contentColor = androidx.compose.ui.graphics.Color(0xFF0B1220)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Difficulty
                SectionLabel("DIFFICULTY")
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) c.copy(alpha = 0.25f)
                                    else if (isLight) androidx.compose.ui.graphics.Color(0xFFF1F5F9)
                                    else DarkSurface
                                )
                                .border(1.dp, if (isSel) c else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { difficulty = d }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = d.name,
                                color = if (isSel) c else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Schedule
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) PrimaryManaBlue.copy(alpha = 0.2f)
                                    else if (isLight) androidx.compose.ui.graphics.Color(0xFFF1F5F9)
                                    else DarkSurface
                                )
                                .border(1.dp, if (isSel) PrimaryManaBlue else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { isRepeat = flag }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSel) PrimaryManaBlue else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isRepeat) {
                    SectionLabel("DAYS (select 1+)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DayShort.forEachIndexed { idx, d ->
                            val sel = idx in selectedDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (sel) PrimaryManaBlue.copy(alpha = 0.2f)
                                        else if (isLight) androidx.compose.ui.graphics.Color(0xFFF1F5F9)
                                        else DarkSurface
                                    )
                                    .border(1.dp, if (sel) PrimaryManaBlue else DarkBorder, CircleShape)
                                    .clickable {
                                        selectedDays = if (sel) selectedDays - idx else selectedDays + idx
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d, color = if (sel) PrimaryManaBlue else TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("TIME")
                    Spacer(Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(androidx.compose.ui.graphics.Color.Transparent)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    ) {
                        listOf(false to "12h", true to "24h").forEach { (flag, label) ->
                            val sel = is24h == flag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { is24h = flag }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (sel) PrimaryManaBlue else TextMuted, fontSize = 10.sp)
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
                        onValueChange = { hourText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text(if (is24h) "HH" else "HH (1-12)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            unfocusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            focusedLabelColor = PrimaryManaBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                    Text(":", color = TextPrimary, fontSize = 18.sp)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { minuteText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("MM") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            unfocusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            focusedLabelColor = PrimaryManaBlue,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                    if (!is24h) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLight) androidx.compose.ui.graphics.Color(0xFFF1F5F9) else DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        ) {
                            listOf("AM", "PM").forEach { t ->
                                val sel = amPm == t
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { amPm = t }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(t, color = if (sel) PrimaryManaBlue else TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Timer (for Hard/Critical)
                if (requiresTimer) {
                    SectionLabel("TIMER (MINUTES)")
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("e.g. 60") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = difficultyColor,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            unfocusedTextColor = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            focusedLabelColor = difficultyColor,
                            unfocusedLabelColor = TextMuted
                        )
                    )
                }

                // Completion
                SectionLabel("COMPLETION")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Allow complete anytime",
                            color = if (isLight) androidx.compose.ui.graphics.Color(0xFF0B1220) else TextPrimary,
                            fontSize = 11.sp
                        )
                        Text(
                            if (allowEarly) "You can complete before scheduled time"
                            else "Locked until scheduled time",
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = allowEarly,
                        onCheckedChange = { allowEarly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isLight) androidx.compose.ui.graphics.Color.White else DarkCard,
                            checkedTrackColor = PrimaryManaBlue
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
                    val hh = (hourText.toIntOrNull() ?: 0)
                    val mm = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val hour24 = if (is24h) {
                        hh.coerceIn(0, 23)
                    } else {
                        val h12 = if (hh == 0) 12 else hh.coerceIn(1, 12)
                        when {
                            amPm == "AM" && h12 == 12 -> 0
                            amPm == "PM" && h12 != 12 -> h12 + 12
                            else -> h12
                        }
                    }
                    val minutes = hour24 * 60 + mm
                    val csv = selectedDays.sorted().joinToString(",")
                    val dur = if (requiresTimer) durationText.toIntOrNull() ?: 0 else 0
                    onConfirm(
                        title,
                        difficulty,
                        minutes,
                        csv,
                        dur,
                        isRepeat,
                        allowEarly,
                        selectedType
                    )
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = difficultyColor,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF0B1220)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEdit) "SAVE" else "CREATE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val isLight = ThemeState.isLight
    val accent = AccentGold
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) accent.copy(alpha = 0.22f)
                else if (isLight) androidx.compose.ui.graphics.Color(0xFFF1F5F9)
                else DarkSurface
            )
            .border(
                1.dp,
                if (selected) accent else DarkBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(start = 12.dp, end = if (onDelete != null) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) accent else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (onDelete != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Delete type",
                tint = if (selected) accent else TextMuted,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onDelete() }
            )
        }
    }
}
