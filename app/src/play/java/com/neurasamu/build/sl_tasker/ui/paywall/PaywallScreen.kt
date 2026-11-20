package com.neurasamu.build.sl_tasker.ui.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PlayBillingManager
import com.neurasamu.build.sl_tasker.security.PlayPlan
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary

@Composable
fun PaywallScreen(
    reason: String = "Unlock Pro features",
    onClose: () -> Unit,
    onUnlocked: () -> Unit
) {
    val ctx = LocalContext.current
    val billingState by PlayBillingManager.uiState.collectAsState()
    val isPro by LicenseManager.isProFlow.collectAsState()
    var selectedPlan by remember { mutableStateOf(PlayPlan.YEARLY) }

    LaunchedEffect(Unit) {
        PlayBillingManager.clearStatus()
        PlayBillingManager.refresh()
    }

    LaunchedEffect(isPro) {
        if (isPro) {
            onUnlocked()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5070B14))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "SL TASKER PRO",
                    color = AccentGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.6.sp
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextPrimary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = reason,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(14.dp))

        // Plan Selector Cards
        PlanOptionCard(
            title = "PRO MONTHLY",
            price = "${billingState.monthlyPrice} / mo",
            subtitle = "Cancel anytime via Google Play",
            badge = null,
            selected = selectedPlan == PlayPlan.MONTHLY,
            onClick = { selectedPlan = PlayPlan.MONTHLY }
        )
        Spacer(Modifier.height(8.dp))
        PlanOptionCard(
            title = "PRO YEARLY",
            price = "${billingState.yearlyPrice} / yr",
            subtitle = "Billed annually • 10% cheaper than monthly",
            badge = "SAVE 10%",
            selected = selectedPlan == PlayPlan.YEARLY,
            onClick = { selectedPlan = PlayPlan.YEARLY }
        )
        Spacer(Modifier.height(8.dp))
        PlanOptionCard(
            title = "PRO LIFETIME",
            price = billingState.lifetimePrice,
            subtitle = "One-time purchase • Permanent access",
            badge = "BEST VALUE",
            selected = selectedPlan == PlayPlan.LIFETIME,
            onClick = { selectedPlan = PlayPlan.LIFETIME }
        )

        Spacer(Modifier.height(14.dp))

        // Benefits
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard.copy(alpha = 0.85f))
                .border(1.dp, PrimaryManaBlue.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                "WHAT YOU UNLOCK",
                color = PrimaryManaBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            PlayBenefitRow("Unlimited active tasks & custom categories")
            PlayBenefitRow("Unlimited Level progression (breaks the Level 2 cap)")
            PlayBenefitRow("Unlimited Hard and Critical difficulty quests")
            PlayBenefitRow("Unlimited App Blocker slots during focus sessions")
            PlayBenefitRow("AI Hub: Interactive Chat & Autonomous Agent Mode")
            PlayBenefitRow("AI Hub: Connect your own API key or local AI models")
            PlayBenefitRow("Live HP-Drop alert notifications & Pro orbiting rings")
            PlayBenefitRow("Gold & Black PRO Nameplate + Full 10-Tier Hunter Ranks")
        }

        Spacer(Modifier.height(14.dp))

        billingState.statusMessage?.let { msg ->
            Text(
                text = msg,
                color = AccentGold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        Button(
            onClick = {
                val activity = ctx.findActivity()
                if (activity != null) {
                    PlayBillingManager.launchPurchase(activity, selectedPlan)
                }
            },
            enabled = !billingState.isBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Color(0xFF070A12)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (billingState.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF070A12),
                    strokeWidth = 2.dp
                )
            } else {
                val ctaPrice = when (selectedPlan) {
                    PlayPlan.MONTHLY -> "${billingState.monthlyPrice} / mo"
                    PlayPlan.YEARLY -> "${billingState.yearlyPrice} / yr"
                    PlayPlan.LIFETIME -> "${billingState.lifetimePrice} once"
                }
                Text(
                    text = "CONTINUE WITH GOOGLE PLAY ($ctaPrice)",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { PlayBillingManager.restorePurchases() },
            enabled = !billingState.isBusy,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = PrimaryManaBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("RESTORE PURCHASES", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Subscriptions auto-renew via Google Play unless canceled at least 24 hours before renewal in Google Play Store settings. Lifetime is a one-time non-recurring purchase.",
            color = TextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(
                        listOf(AccentGold.copy(alpha = 0.22f), PrimaryManaBlue.copy(alpha = 0.14f))
                    )
                } else {
                    Brush.horizontalGradient(listOf(DarkCard, DarkCard))
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AccentGold else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (selected) AccentGold else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PrimaryManaBlue.copy(alpha = 0.22f))
                            .border(0.8.dp, PrimaryManaBlue, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = PrimaryManaBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
        }
        Text(
            text = price,
            color = if (selected) AccentGold else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun PlayBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, color = TextPrimary, fontSize = 11.sp)
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
