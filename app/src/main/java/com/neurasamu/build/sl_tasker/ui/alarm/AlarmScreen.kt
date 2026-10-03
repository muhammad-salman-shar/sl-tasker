package com.neurasamu.build.sl_tasker.ui.alarm

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.AlarmEntity
import com.neurasamu.build.sl_tasker.data.model.DismissMethod
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.viewmodel.AlarmViewModel
import java.util.Locale

@Composable
fun AlarmScreen(
    alarmViewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val alarms by alarmViewModel.alarms.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
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

            Text(
                text = "SYSTEM AWAKENING PROTOCOL",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryManaBlue
            )
            Text(
                text = "Configure critical wake alarms and dismissal challenges",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NO ALARMS CONFIGURED",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to initialize a wake-up schedule.",
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
                        items = alarms,
                        key = { it.id }
                    ) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onToggle = { enabled ->
                                alarmViewModel.toggleAlarm(alarm, enabled)
                            },
                            onDelete = {
                                alarmViewModel.deleteAlarm(alarm)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { hour, minute, label, method, pin, isRepeat, csv, snoozeOn, snoozeMin, vib ->
                alarmViewModel.addAlarm(
                    hour = hour,
                    minute = minute,
                    label = label,
                    dismissMethod = method,
                    pinCode = pin,
                    daysCsv = csv,
                    isRepeat = isRepeat,
                    snoozeEnabled = snoozeOn,
                    snoozeMinutes = snoozeMin,
                    vibrate = vib
                )
            }
        )
    }
}

@Composable
private fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val displayHour = if (alarm.hour == 0) 12 else if (alarm.hour > 12) alarm.hour - 12 else alarm.hour
    val amPm = if (alarm.hour >= 12) "PM" else "AM"
    val timeString = String.format(Locale.getDefault(), "%02d:%02d", displayHour, alarm.minute)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, if (alarm.enabled) PrimaryManaBlue else DarkBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 32.sp,
                        color = if (alarm.enabled) TextPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPm,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alarm.enabled) PrimaryManaBlue else TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarm.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DarkSurface)
                        .border(
                            1.dp,
                            if (alarm.dismissMethod == DismissMethod.EASY) DarkBorder else DangerPenaltyRed,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "METHOD: ${alarm.dismissMethod.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (alarm.dismissMethod == DismissMethod.EASY) TextMuted else DangerPenaltyRed,
                        fontSize = 9.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryManaBlue,
                        checkedTrackColor = DarkSurface,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkCard
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDelete) {
                    Text(
                        text = "DEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerPenaltyRed
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        hour: Int,
        minute: Int,
        label: String,
        method: DismissMethod,
        pin: String,
        isRepeat: Boolean,
        daysCsv: String,
        snoozeEnabled: Boolean,
        snoozeMinutes: Int,
        vibrate: Boolean
    ) -> Unit
) {
    var hourText by remember { mutableStateOf("07") }
    var minuteText by remember { mutableStateOf("00") }
    var amPm by remember { mutableStateOf("AM") }
    var is24h by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("Wake Up Hunter") }
    var selectedMethod by remember { mutableStateOf(DismissMethod.EASY) }
    var pinCode by remember { mutableStateOf("") }
    var isRepeat by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var snoozeEnabled by remember { mutableStateOf(true) }
    var snoozeText by remember { mutableStateOf("10") }
    var vibrate by remember { mutableStateOf(true) }
    val dayShort = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "NEW ALARM",
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
                // Schedule mode (Single / Repeat)
                Text("SCHEDULE", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(false to "Single", true to "Repeat").forEach { (flag, text) ->
                        val sel = isRepeat == flag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (sel) PrimaryManaBlue else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { isRepeat = flag }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text, color = if (sel) PrimaryManaBlue else TextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Days selection
                Text(
                    if (isRepeat) "DAYS (select 1+)" else "PICK A DAY (optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    dayShort.forEachIndexed { idx, d ->
                        val sel = idx in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
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
                if (isRepeat && selectedDays.size == 7) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Daily alarm (all 7 days)", color = PrimaryManaBlue, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                    ) {
                        listOf(false to "12h", true to "24h").forEach { (flag, txt) ->
                            val sel = is24h == flag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                    .clickable { is24h = flag }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(txt, color = if (sel) PrimaryManaBlue else TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hourText = it },
                        label = { Text(if (is24h) "HH" else "HH (1-12)", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Text(":", color = TextPrimary, fontSize = 18.sp)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minuteText = it },
                        label = { Text("MM", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (!is24h) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        ) {
                            listOf("AM", "PM").forEach { t ->
                                val sel = amPm == t
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (sel) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                        .clickable { amPm = t }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(t, color = if (sel) PrimaryManaBlue else TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label", color = TextSecondary) },
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

                // Snooze
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SNOOZE", color = TextPrimary, fontSize = 12.sp)
                        Text("Allow delaying this alarm", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = snoozeEnabled,
                        onCheckedChange = { snoozeEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCard,
                            checkedTrackColor = PrimaryManaBlue
                        )
                    )
                }
                if (snoozeEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = snoozeText,
                        onValueChange = { snoozeText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Snooze minutes", color = TextSecondary) },
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Vibrate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VIBRATE", color = TextPrimary, fontSize = 12.sp)
                        Text("Vibrate when alarm rings", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCard,
                            checkedTrackColor = PrimaryManaBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("DISMISS METHOD", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DismissMethod.entries.forEach { method ->
                        val isSelected = selectedMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryManaBlue.copy(alpha = 0.2f) else DarkSurface)
                                .border(1.dp, if (isSelected) PrimaryManaBlue else DarkBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedMethod = method }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PrimaryManaBlue else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (selectedMethod == DismissMethod.PIN) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { if (it.all { c -> c.isDigit() }) pinCode = it },
                        label = { Text("Set 4-Digit PIN", color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DangerPenaltyRed,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawH = hourText.toIntOrNull() ?: 7
                    val minute = (minuteText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val hour24 = if (is24h) {
                        rawH.coerceIn(0, 23)
                    } else {
                        val h12 = if (rawH == 0) 12 else rawH.coerceIn(1, 12)
                        when {
                            amPm == "AM" && h12 == 12 -> 0
                            amPm == "PM" && h12 != 12 -> h12 + 12
                            else -> h12
                        }
                    }
                    val csv = if (isRepeat) selectedDays.sorted().joinToString(",") else selectedDays.sorted().joinToString(",")
                    onConfirm(
                        hour24,
                        minute,
                        label.trim(),
                        selectedMethod,
                        pinCode.trim(),
                        isRepeat || selectedDays.isNotEmpty(),
                        csv,
                        snoozeEnabled,
                        (snoozeText.toIntOrNull() ?: 10).coerceAtLeast(1),
                        vibrate
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = DarkCard
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("SAVE", style = MaterialTheme.typography.labelLarge, color = DarkCard)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelLarge, color = TextMuted)
            }
        }
    )
}
