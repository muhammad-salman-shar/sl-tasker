package com.neurasamu.build.sl_tasker.ui.block

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.block.BlockState
import com.neurasamu.build.sl_tasker.ui.components.QrScannerDialog
import com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary

@Composable
fun BlockOverlayScreen(
    blockedPackage: String?,
    blockState: BlockState = BlockState(),
    onUseEarnedScroll: () -> Unit = {},
    onSealVerified: () -> Unit = {},
    onOpenTasker: () -> Unit
) {
    var showSealScanner by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "blockPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val availableScrollMins = blockState.earnedScrollSeconds / 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF1A0505),
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DangerPenaltyRed.copy(alpha = 0.25f * glowAlpha),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(DarkBackground)
                        .border(2.dp, DangerPenaltyRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = DangerPenaltyRed,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = "ACCESS DENIED",
                style = MaterialTheme.typography.headlineMedium,
                color = DangerPenaltyRed,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "The System requires physical effort or focus.",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (blockState.hasRegisteredSeal)
                    "Scan your registered Physical Seal (${blockState.sealLabel}), use Walk-to-Earn scroll quota, or open SL Tasker."
                else
                    "Walk 500 real steps to earn scroll quota, or complete your pending tasks in SL Tasker.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )

            if (!blockedPackage.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "BLOCKED: $blockedPackage",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (blockState.hasRegisteredSeal) {
                OutlinedButton(
                    onClick = { showSealScanner = true },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(AccentCyan)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "SCAN PHYSICAL SEAL (UNLOCK 10m)",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            if (availableScrollMins >= 10) {
                OutlinedButton(
                    onClick = onUseEarnedScroll,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(SuccessGreen)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "USE 10m SCROLL QUOTA (${availableScrollMins}m EARNED)",
                        color = SuccessGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
            } else {
                Text(
                    text = "Walk-to-Earn Quota: ${availableScrollMins}m / 10m needed (500 steps = 10m)",
                    color = AccentGold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onOpenTasker,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = TextOnBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "OPEN SL TASKER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Scroll time cannot be farmed from trivial tasks.",
                style = MaterialTheme.typography.labelSmall,
                color = DangerPenaltyRed.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showSealScanner && blockState.hasRegisteredSeal) {
        QrScannerDialog(
            title = "VERIFY PHYSICAL SEAL",
            subtitle = "Scan \"${blockState.sealLabel}\" to unlock 10 minutes of scroll time",
            expectedCode = blockState.sealCode,
            onDismiss = { showSealScanner = false },
            onScanned = {
                showSealScanner = false
                onSealVerified()
            }
        )
    }
}
