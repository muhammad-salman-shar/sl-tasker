package com.neurasamu.build.solo_leveling_tasker.ui.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.solo_leveling_tasker.service.AlarmService
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBackground
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBorder
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkCard
import com.neurasamu.build.solo_leveling_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.solo_leveling_tasker.ui.theme.SoloLevelingTheme
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextMuted
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextPrimary
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextSecondary

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val requiredPin = intent.getStringExtra("PIN_CODE") ?: ""

        setContent {
            SoloLevelingTheme {
                AlarmRingContent(
                    label = alarmLabel,
                    dismissMethod = dismissMethod,
                    requiredPin = requiredPin,
                    onDismissConfirmed = {
                        dismissAlarm()
                    }
                )
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
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
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
        finish()
    }
}

@Composable
fun AlarmRingContent(
    label: String,
    dismissMethod: String,
    requiredPin: String,
    onDismissConfirmed: () -> Unit
) {
    var inputVal by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val num1 by remember { mutableIntStateOf((12..49).random()) }
    val num2 by remember { mutableIntStateOf((11..49).random()) }
    val expectedAnswer = num1 + num2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(2.dp, DangerPenaltyRed, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "SYSTEM EMERGENCY ALERT",
                style = MaterialTheme.typography.labelLarge,
                color = DangerPenaltyRed,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Awaken immediately to prevent system penalties.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            when {
                dismissMethod.contains("PIN", ignoreCase = true) -> {
                    Text(
                        text = "ENTER SECURITY PIN TO DISMISS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DangerPenaltyRed,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                dismissMethod.contains("MATH", ignoreCase = true) -> {
                    Text(
                        text = "SOLVE TO DISMISS: $num1 + $num2 = ?",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryManaBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { inputVal = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerPenaltyRed
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        dismissMethod.contains("PIN", ignoreCase = true) -> {
                            if (inputVal == requiredPin || requiredPin.isBlank()) {
                                onDismissConfirmed()
                            } else {
                                errorMessage = "Incorrect PIN. Focus Hunter."
                            }
                        }
                        dismissMethod.contains("MATH", ignoreCase = true) -> {
                            if (inputVal.toIntOrNull() == expectedAnswer) {
                                onDismissConfirmed()
                            } else {
                                errorMessage = "Incorrect calculation. Try again."
                            }
                        }
                        else -> {
                            onDismissConfirmed()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerPenaltyRed,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "DISMISS ALARM",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
        }
    }
}
