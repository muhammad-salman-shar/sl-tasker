package com.neurasamu.build.sl_tasker.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.sl_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.sl_tasker.data.model.Priority
import com.neurasamu.build.sl_tasker.data.model.ScheduleMode
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.PurpleMagic
import com.neurasamu.build.sl_tasker.ui.theme.RankA
import com.neurasamu.build.sl_tasker.ui.theme.RankC
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import com.neurasamu.build.sl_tasker.util.SoundFx
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
    val ctx = LocalContext.current
    val isLight = ThemeState.isLight
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val scheduledStr = dateFormat.format(Date(occurrence.scheduledAt))

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

    val advanceLineColor = PurpleMagic
    val cardBorderColor = when {
        task.priority == Priority.CRITICAL -> DangerPenaltyRed
        task.isAdvanced -> advanceLineColor
        else -> DarkBorder
    }

    val shape = NotchedShape(notch = 16.dp, corner = 12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (isLight)
                        listOf(Color.White.copy(alpha = 0.88f), Color.White.copy(alpha = 0.68f))
                    else
                        listOf(DarkCard.copy(alpha = 0.58f), DarkCard.copy(alpha = 0.34f))
                )
            )
            .border(
                width = if (task.isAdvanced || task.priority == Priority.CRITICAL) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = shape
            )
            .drawWithContent {
                drawContent()
                if (task.isAdvanced) {
                    val strokePx = 4.dp.toPx()
                    drawLine(
                        brush = Brush.verticalGradient(listOf(advanceLineColor, AccentCyan)),
                        start = Offset(strokePx / 2f, 0f),
                        end = Offset(strokePx / 2f, size.height),
                        strokeWidth = strokePx
                    )
                }
            }
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
                    if (task.isAdvanced) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(advanceLineColor.copy(alpha = 0.22f))
                                .border(1.dp, advanceLineColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ADV",
                                style = MaterialTheme.typography.labelSmall,
                                color = advanceLineColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
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
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    val epGain = GamificationEngine.epRewardFor(task.difficulty)
                    val hpGain = GamificationEngine.hpRewardFor(task.difficulty)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurface)
                            .border(1.dp, difficultyColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${task.difficulty.name} (+${epGain} EP · +${hpGain} HP)",
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor,
                            fontSize = 9.sp
                        )
                    }
                    if (task.userType.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGold.copy(alpha = 0.15f))
                                .border(1.dp, AccentGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.userType,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                val timeLabel = if (task.scheduleMode == ScheduleMode.NO_TIME) {
                    if (task.validityDays > 0) "ANYTIME · ${task.validityDays}d" else "ANYTIME · LIFETIME"
                } else {
                    "AT: $scheduledStr"
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.scheduleMode == ScheduleMode.NO_TIME) AccentGold else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isLight) Color(0xFF0B1220) else TextPrimary
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLight) Color(0xFF475569) else TextSecondary
                )
            }

            if (task.isAdvanced && (!task.hpDropEnabled || task.requireQrProof || task.stepGoal > 0)) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!task.hpDropEnabled) {
                        AdvanceMetaPill("SAFE · NO HP DROP", SuccessGreen)
                    }
                    if (task.requireQrProof) {
                        AdvanceMetaPill("QR SEAL", AccentCyan)
                    }
                    if (task.stepGoal > 0) {
                        AdvanceMetaPill("GOAL: ${task.stepGoal} STEPS", AccentGold)
                    }
                }
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
                val now = System.currentTimeMillis()
                val isLocked = !isCompleted && !task.allowEarlyComplete && now < occurrence.scheduledAt
                val remainingMs = (occurrence.scheduledAt - now).coerceAtLeast(0L)
                val lockedLabel = when {
                    remainingMs < 60_000L -> "LOCKED"
                    remainingMs < 60L * 60_000L -> "LOCKED ${remainingMs / 60_000L}m"
                    remainingMs < 24L * 60L * 60_000L -> "LOCKED ${remainingMs / (60L * 60_000L)}h"
                    else -> "LOCKED"
                }

                when {
                    isCompleted -> {
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
                    }
                    isLocked -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = lockedLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                    else -> {
                        val needsTimer = task.difficulty != Difficulty.MEDIUM
                        Button(
                            onClick = {
                                SoundFx.complete()
                                try {
                                    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        v.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        v.vibrate(80)
                                    }
                                } catch (_: Throwable) {}
                                if (needsTimer) onStart() else onComplete()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (needsTimer) difficultyColor else PrimaryManaBlue,
                                contentColor = DarkCard
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (needsTimer) "START" else "COMPLETE",
                                style = MaterialTheme.typography.labelLarge,
                                color = DarkCard
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvanceMetaPill(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(0.8.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
