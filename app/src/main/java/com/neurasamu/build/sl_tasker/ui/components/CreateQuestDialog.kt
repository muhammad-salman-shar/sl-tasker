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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.ScheduleMode
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.PurpleMagic
import com.neurasamu.build.sl_tasker.ui.theme.RankA
import com.neurasamu.build.sl_tasker.ui.theme.RankC
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import kotlinx.coroutines.launch

private val DayShort = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

@Composable
fun CreateQuestDialog(
    onDismiss: () -> Unit,
    initialTask: TaskEntity? = null,
    initialType: String = "",
    existingTypes: List<String> = emptyList(),
    onAddType: (String) -> Unit = {},
    onDeleteType: (String) -> Unit = {},
    onConfirm: (
        title: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        daysOfWeekCsv: String,
        durationMinutes: Int,
        isRepeat: Boolean,
        allowEarlyComplete: Boolean,
        userType: String,
        scheduleMode: ScheduleMode,
        validityDays: Int,
        isAdvanced: Boolean,
        hpDropEnabled: Boolean,
        requireQrProof: Boolean,
        stepGoal: Int
    ) -> Unit
) {
    val isEdit = initialTask != null
    val isLight = ThemeState.isLight
    val primaryTextColor = if (isLight) Color(0xFF0B1220) else TextPrimary
    val mutedTextColor = if (isLight) Color(0xFF475569) else TextSecondary
    val inactiveSurfaceColor = if (isLight) Color(0xFFF1F5F9) else DarkSurface

    // Top-Middle Dual Tab State: false = Simple, true = Advance
    var isAdvanceTab by remember { mutableStateOf(initialTask?.isAdvanced ?: false) }

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
    var scheduleMode by remember { mutableStateOf(initialTask?.scheduleMode ?: ScheduleMode.TIME) }
    var validityDaysText by remember {
        mutableStateOf(if ((initialTask?.validityDays ?: 0) > 0) initialTask!!.validityDays.toString() else "")
    }

    // Advance-only controls
    var hpDropEnabled by remember { mutableStateOf(initialTask?.hpDropEnabled ?: true) }
    var requireQrProof by remember { mutableStateOf(initialTask?.requireQrProof ?: false) }
    var stepGoalText by remember {
        mutableStateOf(if ((initialTask?.stepGoal ?: 0) > 0) initialTask!!.stepGoal.toString() else "")
    }

    var selectedType by remember { mutableStateOf((initialTask?.userType ?: initialType).trim()) }
    var showNewTypeInput by remember { mutableStateOf(false) }
    var newTypeText by remember { mutableStateOf("") }
    var sessionAdded by remember { mutableStateOf<List<String>>(emptyList()) }
    var sessionRemoved by remember { mutableStateOf<Set<String>>(emptySet()) }
    val typeRowState = rememberLazyListState()
    val dialogScope = rememberCoroutineScope()
    val knownTypes = remember(existingTypes, sessionAdded, sessionRemoved, initialTask?.userType, initialType) {
        buildList<String> {
            existingTypes.forEach { t ->
                val tr = t.trim()
                if (tr.isNotBlank() && tr.lowercase() !in sessionRemoved && none { it.equals(tr, ignoreCase = true) }) {
                    add(tr)
                }
            }
            val seedType = (initialTask?.userType ?: initialType).trim()
            if (seedType.isNotBlank() && seedType.lowercase() !in sessionRemoved && none { it.equals(seedType, ignoreCase = true) }) {
                add(seedType)
            }
            sessionAdded.forEach { t ->
                val tr = t.trim()
                if (tr.isNotBlank() && tr.lowercase() !in sessionRemoved && none { it.equals(tr, ignoreCase = true) }) {
                    add(tr)
                }
            }
        }
    }

    val difficultyColor = when (difficulty) {
        Difficulty.MEDIUM -> RankC
        Difficulty.HARD -> RankA
        Difficulty.CRITICAL -> DangerPenaltyRed
    }
    val tabAccent = if (isAdvanceTab) PurpleMagic else PrimaryManaBlue
    val requiresTimer = difficulty != Difficulty.MEDIUM

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isLight) Color.White.copy(alpha = 0.97f) else DarkCard.copy(alpha = 0.97f),
        modifier = Modifier
            .shadow(24.dp, RoundedCornerShape(24.dp))
            .border(
                width = if (isAdvanceTab) 1.5.dp else 1.dp,
                color = if (isAdvanceTab) PurpleMagic else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEdit) "EDIT TASK" else "CREATE TASK",
                    style = MaterialTheme.typography.titleMedium,
                    color = tabAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                // Top-Middle Side-by-Side Simple | Advance Dual Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(inactiveSurfaceColor)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(false to "Simple", true to "Advance").forEach { (advFlag, tabLabel) ->
                        val selected = isAdvanceTab == advFlag
                        val pillBg = when {
                            selected && advFlag -> PurpleMagic
                            selected && !advFlag -> PrimaryManaBlue
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(pillBg)
                                .clickable { isAdvanceTab = advFlag }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                color = if (selected) TextOnBlue else mutedTextColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("Title (max 50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tabAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = primaryTextColor,
                        unfocusedTextColor = primaryTextColor,
                        focusedLabelColor = tabAccent,
                        unfocusedLabelColor = mutedTextColor
                    )
                )

                // 2. Task Type
                SectionLabel("TASK TYPE")
                LazyRow(
                    state = typeRowState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "__none__") {
                        TypeChip(
                            label = "None",
                            selected = selectedType.isBlank(),
                            onClick = { selectedType = "" }
                        )
                    }
                    items(items = knownTypes, key = { it.lowercase() }) { t ->
                        TypeChip(
                            label = t,
                            selected = selectedType.equals(t, ignoreCase = true),
                            onClick = { selectedType = t },
                            onDelete = {
                                val lc = t.lowercase()
                                sessionRemoved = sessionRemoved + lc
                                sessionAdded = sessionAdded.filterNot { it.equals(t, ignoreCase = true) }
                                if (selectedType.equals(t, ignoreCase = true)) selectedType = ""
                                onDeleteType(t)
                            }
                        )
                    }
                    item(key = "__add_new__") {
                        TypeChip(
                            label = "+ New",
                            selected = showNewTypeInput,
                            accentColor = tabAccent,
                            alwaysTinted = true,
                            onClick = { showNewTypeInput = !showNewTypeInput }
                        )
                    }
                }

                if (showNewTypeInput) {
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
                                focusedBorderColor = tabAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = tabAccent,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val nt = newTypeText.trim()
                                if (nt.isNotBlank()) {
                                    val lc = nt.lowercase()
                                    val existingMatch = knownTypes.firstOrNull { it.equals(nt, ignoreCase = true) }
                                    val finalName = existingMatch ?: nt
                                    sessionRemoved = sessionRemoved - lc
                                    if (existingMatch == null) {
                                        sessionAdded = sessionAdded + finalName
                                    }
                                    selectedType = finalName
                                    newTypeText = ""
                                    onAddType(finalName)
                                    dialogScope.launch {
                                        val targetIdx = (knownTypes.size + 1).coerceAtLeast(0)
                                        typeRowState.animateScrollToItem(targetIdx)
                                    }
                                }
                            },
                            enabled = newTypeText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tabAccent,
                                contentColor = TextOnBlue
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. Schedule
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
                                .background(if (isSel) tabAccent.copy(alpha = 0.2f) else inactiveSurfaceColor)
                                .border(1.dp, if (isSel) tabAccent else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { isRepeat = flag }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSel) tabAccent else mutedTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (scheduleMode == ScheduleMode.TIME) {
                    SectionLabel(if (isRepeat) "DAYS (every week)" else "DAY (one-time trigger)")
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
                                    .background(if (sel) tabAccent.copy(alpha = 0.2f) else inactiveSurfaceColor)
                                    .border(1.dp, if (sel) tabAccent else DarkBorder, CircleShape)
                                    .clickable {
                                        selectedDays = if (sel) selectedDays - idx else selectedDays + idx
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(d, color = if (sel) tabAccent else mutedTextColor, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 4. Schedule Mode
                SectionLabel("SCHEDULE MODE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ScheduleMode.TIME to "Time", ScheduleMode.NO_TIME to "No Time").forEach { (m, label) ->
                        val sel = scheduleMode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) tabAccent.copy(alpha = 0.2f) else inactiveSurfaceColor)
                                .border(1.dp, if (sel) tabAccent else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { scheduleMode = m }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (sel) tabAccent else mutedTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (scheduleMode == ScheduleMode.NO_TIME) {
                    SectionLabel("VALIDITY (optional, blank = lifetime)")
                    OutlinedTextField(
                        value = validityDaysText,
                        onValueChange = { validityDaysText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Days (e.g. 3, 7, 30)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor,
                            focusedLabelColor = AccentGold,
                            unfocusedLabelColor = mutedTextColor
                        )
                    )
                }

                // 5. Time
                if (scheduleMode == ScheduleMode.TIME) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("TIME")
                        Spacer(Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        ) {
                            listOf(false to "12h", true to "24h").forEach { (flag, label) ->
                                val sel = is24h == flag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (sel) tabAccent.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { is24h = flag }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(label, color = if (sel) tabAccent else mutedTextColor, fontSize = 10.sp)
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
                                focusedBorderColor = tabAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = tabAccent,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                        Text(":", color = primaryTextColor, fontSize = 18.sp)
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { minuteText = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("MM") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = tabAccent,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = tabAccent,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                        if (!is24h) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(inactiveSurfaceColor)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                            ) {
                                listOf("AM", "PM").forEach { t ->
                                    val sel = amPm == t
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(if (sel) tabAccent.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable { amPm = t }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(t, color = if (sel) tabAccent else mutedTextColor, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Difficulty
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
                        val hpReward = GamificationEngine.hpRewardFor(d)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) c.copy(alpha = 0.25f) else inactiveSurfaceColor)
                                .border(1.dp, if (isSel) c else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable { difficulty = d }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = d.name,
                                    color = if (isSel) c else mutedTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "+${hpReward} HP",
                                    color = if (isSel) c else mutedTextColor.copy(alpha = 0.85f),
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                if (requiresTimer) {
                    val useStepTarget = (stepGoalText.toIntOrNull() ?: 0) > 0
                    SectionLabel("TRACKING TARGET (TIMER OR STEPS)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(false to "Duration (Min)", true to "Step Counter").forEach { (isStepChoice, lbl) ->
                            val sel = useStepTarget == isStepChoice
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) difficultyColor.copy(alpha = 0.22f) else inactiveSurfaceColor)
                                    .border(1.dp, if (sel) difficultyColor else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isStepChoice) {
                                            if ((stepGoalText.toIntOrNull() ?: 0) <= 0) stepGoalText = "1000"
                                        } else {
                                            stepGoalText = ""
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (sel) difficultyColor else mutedTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (useStepTarget) {
                        OutlinedTextField(
                            value = stepGoalText,
                            onValueChange = { stepGoalText = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Step target (e.g. 1000 or 3000)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = difficultyColor,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = difficultyColor,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                    } else {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Duration in minutes (e.g. 30 or 60)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = difficultyColor,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = difficultyColor,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                    }
                }

                // 7. Allow Complete
                SectionLabel("ALLOW COMPLETE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Allow complete anytime",
                            color = primaryTextColor,
                            fontSize = 11.sp
                        )
                        Text(
                            if (allowEarly) "You can complete before scheduled time"
                            else "Locked until scheduled time",
                            color = mutedTextColor,
                            fontSize = 9.sp
                        )
                    }
                    Switch(
                        checked = allowEarly,
                        onCheckedChange = { allowEarly = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextOnBlue,
                            checkedTrackColor = tabAccent
                        )
                    )
                }

                // ==== ADVANCE-ONLY CONTROLS ====
                if (isAdvanceTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(PurpleMagic.copy(alpha = 0.45f))
                    )
                    Text(
                        text = "ADVANCE CONTROLS",
                        color = PurpleMagic,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // HP Drop On/Off Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(inactiveSurfaceColor)
                            .border(1.dp, PurpleMagic.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "HP Drop Penalty",
                                color = primaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (hpDropEnabled) "ON — Missing this task deducts HP"
                                else "OFF — Safe mode (no HP lost if missed)",
                                color = mutedTextColor,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = hpDropEnabled,
                            onCheckedChange = { hpDropEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnBlue,
                                checkedTrackColor = PurpleMagic
                            )
                        )
                    }

                    // QR / Barcode Proof-of-Work
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(inactiveSurfaceColor)
                            .border(1.dp, PurpleMagic.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "QR / Barcode Proof-of-Work",
                                color = primaryTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Require physical QR/barcode scan to complete",
                                color = mutedTextColor,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = requireQrProof,
                            onCheckedChange = { requireQrProof = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnBlue,
                                checkedTrackColor = PurpleMagic
                            )
                        )
                    }

                    // Step Counter Goal
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(inactiveSurfaceColor)
                            .border(1.dp, PurpleMagic.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Step Counter Goal (optional)",
                            color = primaryTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = stepGoalText,
                            onValueChange = { stepGoalText = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Target steps (e.g. 500, 2000)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleMagic,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor,
                                focusedLabelColor = PurpleMagic,
                                unfocusedLabelColor = mutedTextColor
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            val valid = title.isNotBlank() &&
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
                    val pendingTyped = newTypeText.trim()
                    val finalSelectedType = if (pendingTyped.isNotBlank()) {
                        onAddType(pendingTyped)
                        pendingTyped
                    } else {
                        selectedType.trim()
                    }
                    val parsedSteps = if (isAdvanceTab) (stepGoalText.toIntOrNull()?.coerceAtLeast(0) ?: 0) else 0
                    onConfirm(
                        title,
                        difficulty,
                        minutes,
                        csv,
                        dur,
                        isRepeat,
                        allowEarly,
                        finalSelectedType,
                        scheduleMode,
                        validityDaysText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                        isAdvanceTab,
                        if (isAdvanceTab) hpDropEnabled else true,
                        if (isAdvanceTab) requireQrProof else false,
                        parsedSteps
                    )
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tabAccent,
                    contentColor = TextOnBlue
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEdit) "SAVE" else "CREATE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = mutedTextColor, fontSize = 12.sp)
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
private fun TypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    accentColor: Color = AccentGold,
    alwaysTinted: Boolean = false
) {
    val isLight = ThemeState.isLight
    val accent = accentColor
    val active = selected || alwaysTinted
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    selected -> accent.copy(alpha = 0.28f)
                    alwaysTinted -> accent.copy(alpha = 0.14f)
                    isLight -> Color(0xFFF1F5F9)
                    else -> DarkSurface
                }
            )
            .border(
                1.dp,
                if (active) accent else DarkBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(start = 12.dp, end = if (onDelete != null) 6.dp else 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (active) accent else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
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
