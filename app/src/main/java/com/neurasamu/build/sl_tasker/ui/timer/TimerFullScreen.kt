package com.neurasamu.build.sl_tasker.ui.timer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.timer.TimerRuntime
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.util.StepTrackerManager
import kotlinx.coroutines.delay

@Composable
fun TimerFullScreen(
    onPause: () -> Unit,
    onResume: () -> Unit,
    onGiveUp: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val active by TimerRuntime.active.collectAsState()
    val todaySteps by StepTrackerManager.todaySteps.collectAsState()
    val timer = active

    DisposableEffect(Unit) {
        StepTrackerManager.startListening(ctx)
        onDispose { }
    }

    if (timer == null) {
        LaunchedEffect(Unit) { onClose() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text("Quest Completed!", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
        return
    }

    var remaining by remember(timer) { mutableIntStateOf(timer.remainingSeconds()) }
    LaunchedEffect(timer) {
        while (true) {
            val t = TimerRuntime.active.value ?: break
            remaining = t.remainingSeconds()
            if (!t.isStepMode && remaining <= 0) break
            delay(400L)
        }
    }

    val isStepQuest = timer.isStepMode
    val stepsWalked = if (isStepQuest) timer.sessionSteps(todaySteps) else 0
    val stepTarget = timer.stepGoal.coerceAtLeast(1)

    val total = timer.totalSeconds.coerceAtLeast(1)
    val progress = if (isStepQuest) {
        (stepsWalked.toFloat() / stepTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    val mm = remaining / 60
    val ss = remaining % 60
    val timeStr = "%02d:%02d".format(mm, ss)
    val accent = if (isStepQuest) AccentCyan else Color(timer.accentColorArgb)

    val transition = rememberInfiniteTransition(label = "richTimerPulse")
    val glow by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "g"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        Color(0xFF040710)
                    ),
                    radius = 950f
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rich Luxury Glass Focus Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(28.dp, RoundedCornerShape(28.dp), ambientColor = accent, spotColor = accent)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DarkCard,
                            DarkSurface,
                            DarkCard
                        )
                    )
                )
                .border(1.5.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Mode Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isStepQuest) Icons.Rounded.DirectionsWalk else Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when {
                        timer.isPaused -> "QUEST PAUSED"
                        isStepQuest -> "LIVE STEP COUNTER QUEST"
                        else -> "SYSTEM FOCUS LOCK ACTIVE"
                    },
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = timer.taskTitle,
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(Modifier.height(24.dp))

            // Holographic Ring Gauge
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                accent.copy(alpha = 0.14f * glow),
                                Color(0xFF060B18)
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(256.dp)) {
                    val outerStroke = 13.dp.toPx()
                    val inset = outerStroke / 2f + 4.dp.toPx()
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)

                    // Outer ambient track
                    drawArc(
                        color = accent.copy(alpha = 0.14f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = arcSize
                    )
                    // Active glowing sweep
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = outerStroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = arcSize
                    )
                    // Inner subtle ring
                    drawCircle(
                        color = accent.copy(alpha = 0.22f * glow),
                        radius = size.minDimension / 2f - 24.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isStepQuest) {
                        Text(
                            text = "$stepsWalked",
                            color = TextPrimary,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "/ $stepTarget STEPS",
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    } else {
                        Text(
                            text = timeStr,
                            color = TextPrimary,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% REMAINING",
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Stat Highlights Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isStepQuest) "Walk to Auto-Finish" else "Full HP Reward",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isStepQuest) "${(stepTarget - stepsWalked).coerceAtLeast(0)} steps left" else "${timer.durationMinutes}m Session",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isStepQuest) {
                    Button(
                        onClick = if (timer.isPaused) onResume else onPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent.copy(alpha = 0.18f),
                            contentColor = accent
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (timer.isPaused) "▶ RESUME" else "❚❚ PAUSE",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
                Button(
                    onClick = onGiveUp,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerPenaltyRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "GIVE UP (-40 HP)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onClose,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = SolidColor(DarkBorder)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text(
                    text = "MINIMIZE TO FLOATING BUBBLE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
