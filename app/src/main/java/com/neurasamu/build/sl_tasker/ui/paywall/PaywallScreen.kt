package com.neurasamu.build.sl_tasker.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.liquidGlass

@Composable
fun PaywallScreen(
    reason: String = "Unlock Pro features",
    onClose: () -> Unit,
    onUnlocked: () -> Unit
) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showActivation by remember { mutableStateOf(false) }
    var activationInput by remember { mutableStateOf("") }
    var activationError by remember { mutableStateOf<String?>(null) }

    val deviceToken = remember { LicenseManager.generateActivationToken(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00A0E18))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "UPGRADE TO",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp
        )
        Text(
            "HUNTER PRO",
            color = AccentGold,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            reason,
            color = PrimaryManaBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(28.dp))

        // Monthly plan
        PlanCard(
            title = "PRO MONTHLY",
            price = "Rs. 199",
            period = "/ month",
            highlight = false,
            features = listOf(
                "Unlimited Tasks + AI Hub",
                "Hard/Critical unlimited",
                "Block unlimited apps",
                "Golden Player Name",
                "Level 2 cap removed"
            ),
            onChoose = {
                clipboard.setText(AnnotatedString(deviceToken))
                val msg = "Hi! I want to buy SL Tasker PRO (Monthly Rs. 199). My Device ID: $deviceToken"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/923000000000?text=" + Uri.encode(msg))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { ctx.startActivity(intent) } catch (_: Throwable) {}
                showActivation = true
            }
        )

        Spacer(Modifier.height(14.dp))

        // Lifetime plan
        PlanCard(
            title = "LIFETIME",
            price = "Rs. 1,499",
            period = "one-time",
            highlight = true,
            features = listOf(
                "Everything in Monthly",
                "Pay once, use forever",
                "Priority support",
                "Founder's badge"
            ),
            onChoose = {
                clipboard.setText(AnnotatedString(deviceToken))
                val msg = "Hi! I want to buy SL Tasker LIFETIME (Rs. 1,499). My Device ID: $deviceToken"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/923000000000?text=" + Uri.encode(msg))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { ctx.startActivity(intent) } catch (_: Throwable) {}
                showActivation = true
            }
        )

        Spacer(Modifier.height(24.dp))

        // Activation code
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(shape = RoundedCornerShape(14.dp), shadowElevation = 10.dp)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "ALREADY PAID?",
                    color = AccentGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Enter the 8-character activation code you received from support.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = activationInput,
                    onValueChange = {
                        activationInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8)
                        activationError = null
                    },
                    label = { Text("Activation Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Ascii
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = AccentGold,
                        unfocusedLabelColor = TextMuted
                    )
                )
                if (activationError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(activationError!!, color = Color(0xFFFF6B6B), fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (LicenseManager.verifyActivationCode(ctx, activationInput)) {
                            onUnlocked()
                        } else {
                            activationError = "Invalid code for this device."
                        }
                    },
                    enabled = activationInput.length == 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ACTIVATE", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Payment via EasyPaisa / JazzCash / Raast. Activation code is bound to this device.",
            color = TextMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    highlight: Boolean,
    features: List<String>,
    onChoose: () -> Unit
) {
    val accent = if (highlight) AccentGold else PrimaryManaBlue
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (highlight)
                    Brush.verticalGradient(listOf(AccentGold.copy(alpha = 0.18f), DarkCard.copy(alpha = 0.9f)))
                else
                    Brush.verticalGradient(listOf(DarkCard.copy(alpha = 0.9f), DarkCard.copy(alpha = 0.7f)))
            )
            .border(if (highlight) 2.dp else 1.dp, accent, shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.weight(1f)
            )
            if (highlight) {
                Text(
                    "BEST VALUE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(AccentGold, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(price, color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(" $period", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        features.forEach { f ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text("✓", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(f, color = TextPrimary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onChoose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("BUY NOW", fontWeight = FontWeight.Bold)
        }
    }
}
