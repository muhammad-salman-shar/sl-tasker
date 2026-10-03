package com.neurasamu.build.sl_tasker.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import com.neurasamu.build.sl_tasker.service.AlarmService
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SoloLevelingTheme
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import kotlin.math.abs

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val requiredPin = intent.getStringExtra("PIN_CODE") ?: ""
        val snoozeEnabled = intent.getBooleanExtra("SNOOZE_ENABLED", true)
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 10)

        setContent {
            SoloLevelingTheme {
                AlarmRingContent(
                    label = alarmLabel,
                    dismissMethod = dismissMethod,
                    requiredPin = requiredPin,
                    snoozeEnabled = snoozeEnabled,
                    snoozeMinutes = snoozeMinutes,
                    onDismissConfirmed = { dismissAlarm() },
                    onSnoozeConfirmed = {
                        if (alarmId > 0) {
                            AlarmScheduler(this).snooze(
                                alarmId = alarmId,
                                minutes = snoozeMinutes,
                                label = alarmLabel,
                                dismissMethod = dismissMethod,
                                pinCode = requiredPin
                            )
                        }
                        stopAlarmService()
                        finish()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun dismissAlarm() {
        stopAlarmService()
        finish()
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
    }
}

@Composable
fun AlarmRingContent(
    label: String,
    dismissMethod: String,
    requiredPin: String,
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    onDismissConfirmed: () -> Unit,
    onSnoozeConfirmed: () -> Unit
) {
    var inputVal by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showChallenge by remember { mutableStateOf(false) }

    val num1 by remember { mutableIntStateOf((12..49).random()) }
    val num2 by remember { mutableIntStateOf((11..49).random()) }
    val expectedAnswer = num1 + num2
    val needsChallenge = dismissMethod.contains("PIN", ignoreCase = true) ||
            dismissMethod.contains("MATH", ignoreCase = true)

    // Drag offsets
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }

    val dragThreshold = 260f

    fun attemptDismiss() {
        when {
            dismissMethod.contains("PIN", ignoreCase = true) -> {
                if (inputVal == requiredPin || requiredPin.isBlank()) onDismissConfirmed()
                else errorMessage = "Incorrect PIN. Focus Hunter."
            }
            dismissMethod.contains("MATH", ignoreCase = true) -> {
                if (inputVal.toIntOrNull() == expectedAnswer) onDismissConfirmed()
                else errorMessage = "Incorrect calculation. Try again."
            }
            else -> onDismissConfirmed()
        }
    }

    fun attemptSnooze() {
        if (snoozeEnabled) onSnoozeConfirmed()
        else errorMessage = "Snooze disabled for this alarm."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragY < -dragThreshold) {
                            // swipe up → dismiss (challenge if needed)
                            if (needsChallenge) {
                                showChallenge = true
                            } else {
                                attemptDismiss()
                            }
                        } else if (abs(dragX) > dragThreshold) {
                            attemptSnooze()
                        }
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { _, delta ->
                        dragX += delta.x
                        dragY += delta.y
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Alarm,
                contentDescription = null,
                tint = DangerPenaltyRed,
                modifier = Modifier
                    .height(72.dp)
                    .graphicsLayer(
                        translationY = dragY.coerceAtMost(0f),
                        scaleX = 1f + (abs(dragY) / 1500f).coerceAtMost(0.2f),
                        scaleY = 1f + (abs(dragY) / 1500f).coerceAtMost(0.2f)
                    )
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "SYSTEM WAKE UP",
                style = MaterialTheme.typography.labelLarge,
                color = DangerPenaltyRed,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            if (showChallenge) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (dismissMethod.contains("MATH", ignoreCase = true)) {
                            Text(
                                text = "SOLVE: $num1 + $num2 = ?",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryManaBlue
                            )
                        } else {
                            Text(
                                text = "ENTER PIN",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryManaBlue
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputVal,
                            onValueChange = { inputVal = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (dismissMethod.contains("MATH"))
                                    KeyboardType.Number else KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryManaBlue,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { attemptDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SuccessGreen,
                                contentColor = DarkBackground
                            )
                        ) {
                            Text("CONFIRM & DISMISS")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Swipe hints
            RowHints(snoozeEnabled = snoozeEnabled, snoozeMinutes = snoozeMinutes)

            Spacer(Modifier.height(24.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerPenaltyRed,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }

            // Buttons as fallback
            RowButtons(
                snoozeEnabled = snoozeEnabled,
                snoozeMinutes = snoozeMinutes,
                onSnooze = { attemptSnooze() },
                onDismiss = {
                    if (needsChallenge) showChallenge = true else attemptDismiss()
                }
            )
        }
    }
}

@Composable
private fun RowHints(snoozeEnabled: Boolean, snoozeMinutes: Int) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.ArrowDownward,
                contentDescription = null,
                tint = if (snoozeEnabled) PrimaryManaBlue else TextMuted,
                modifier = Modifier.height(28.dp).alpha(if (snoozeEnabled) 1f else 0.4f)
            )
            Text(
                text = if (snoozeEnabled) "Snooze $snoozeMinutes m" else "Snooze off",
                color = if (snoozeEnabled) PrimaryManaBlue else TextMuted,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.ArrowUpward,
                contentDescription = null,
                tint = DangerPenaltyRed,
                modifier = Modifier.height(28.dp)
            )
            Text("Dismiss", color = DangerPenaltyRed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun RowButtons(
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (snoozeEnabled) {
            Button(
                onClick = onSnooze,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = PrimaryManaBlue
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SNOOZE $snoozeMinutes m")
            }
        }
        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = DangerPenaltyRed,
                contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("DISMISS")
        }
    }
}
