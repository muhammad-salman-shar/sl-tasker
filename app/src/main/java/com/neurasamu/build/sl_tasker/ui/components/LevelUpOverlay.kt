package com.neurasamu.build.sl_tasker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.AccentGoldLight
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextOnGold
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun LevelUpOverlay(level: Int, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    var showInner by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "alpha"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (visible) 0.88f else 0f,
        animationSpec = tween(350),
        label = "bg"
    )

    val transition = rememberInfiniteTransition(label = "ring")
    val ringScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha"
    )

    LaunchedEffect(level) {
        visible = true
        delay(150)
        showInner = true
        delay(2400)
        visible = false
        delay(380)
        onDismiss()
    }

    if (bgAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0A0F1A).copy(alpha = bgAlpha),
                            Color.Black.copy(alpha = bgAlpha)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.scale(scale).alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing outer ring
                Canvas(modifier = Modifier.size(340.dp)) {
                    drawCircle(
                        color = AccentGold.copy(alpha = ringAlpha),
                        radius = (size.minDimension / 2f) * ringScale,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = AccentGold.copy(alpha = ringAlpha * 0.5f),
                        radius = (size.minDimension / 2.6f) * ringScale,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    // LEVEL UP text
                    Text(
                        text = "LEVEL UP",
                        color = AccentGoldLight,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    // Big level number in circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AccentGold.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(2.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LV",
                                color = AccentGold,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$level",
                                color = TextPrimary,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (showInner) {
                        Text(
                            text = "HUNTER ASCENDED",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 3.sp
                        )
                    }
                }
            }
        }
    }
}
