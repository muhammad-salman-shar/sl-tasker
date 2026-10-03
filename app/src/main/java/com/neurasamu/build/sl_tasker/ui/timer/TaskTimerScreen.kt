package com.neurasamu.build.sl_tasker.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun TaskTimerScreen(
    title: String,
    durationMinutes: Int,
    accentColor: Color,
    onFinish: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSec = remember { (durationMinutes * 60).coerceAtLeast(1) }
    var remaining by remember { mutableStateOf(totalSec) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000L)
            remaining -= 1
        }
        onFinish()
    }

    val progress = remaining.toFloat() / totalSec.toFloat()
    val mm = remaining / 60
    val ss = remaining % 60
    val timeStr = "%02d:%02d".format(mm, ss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "QUEST IN PROGRESS",
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(1.dp, DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(260.dp)) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = accentColor.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = arcSize
                    )
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        topLeft = Offset(inset, inset),
                        size = arcSize
                    )
                }
                Text(
                    text = timeStr,
                    color = TextPrimary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Stay focused. Complete the task.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )

            Spacer(Modifier.height(24.dp))

            // Smooth animated progress bar (sweeps across based on elapsed time)
            val smoothProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 950, easing = LinearEasing),
                label = "smoothProgress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DarkBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(smoothProgress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = DangerPenaltyRed
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("GIVE UP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
