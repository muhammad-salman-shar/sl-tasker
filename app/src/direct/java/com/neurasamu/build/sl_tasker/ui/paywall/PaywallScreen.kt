package com.neurasamu.build.sl_tasker.ui.paywall

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PaymentVault
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
    val clipboard = LocalClipboardManager.current

    var isLifetime by remember { mutableStateOf(true) }
    var selectedProofUri by remember { mutableStateOf<Uri?>(null) }
    var showActivation by remember { mutableStateOf(false) }
    var activationInput by remember { mutableStateOf("") }
    var activationError by remember { mutableStateOf<String?>(null) }

    val deviceToken = remember { LicenseManager.getDeviceId(ctx) }
    val qrBytes = remember(isLifetime) {
        PaymentVault.verifyAndLoadQrBytes(ctx, isLifetime)
    }
    val qrBitmap = remember(qrBytes) {
        qrBytes?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Throwable) {
                null
            }
        }
    }

    val planLabel = if (isLifetime) "LIFETIME (Rs. 1,499)" else "PRO MONTHLY (Rs. 199)"
    val priceText = if (isLifetime) "Rs. 1,499" else "Rs. 199"
    val waNumber = remember { PaymentVault.whatsappNumber() }
    val localAccount = remember { PaymentVault.localAccount() }
    val accountTitle = remember { PaymentVault.accountTitle() }

    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedProofUri = uri
            clipboard.setText(AnnotatedString(deviceToken))
            val msg = "Hi! I have paid for SL Tasker $planLabel. My Device ID: $deviceToken"
            sendProofToWhatsApp(ctx, waNumber, uri, msg)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF5070B14))
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top close row
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

        Spacer(Modifier.height(6.dp))

        Text(
            reason,
            color = TextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Plan selector tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlanOptionCard(
                title = "PRO MONTHLY",
                price = "Rs. 199",
                subtitle = "per month",
                selected = !isLifetime,
                badge = null,
                onClick = { isLifetime = false },
                modifier = Modifier.weight(1f)
            )
            PlanOptionCard(
                title = "LIFETIME",
                price = "Rs. 1,499",
                subtitle = "one-time",
                selected = isLifetime,
                badge = "BEST VALUE",
                onClick = { isLifetime = true },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Pro benefits value stack
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard.copy(alpha = 0.78f))
                .border(1.dp, AccentGold.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                "EVERYTHING YOU UNLOCK WITH PRO",
                color = AccentGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(2.dp))
            BenefitRow("Unlimited active quests (removes the 2-task free limit)")
            BenefitRow("Daily Quests unlocked for bonus EP and HP recovery")
            BenefitRow("Unlimited Level progression (breaks the Level 2 cap)")
            BenefitRow("Unlimited Hard and Critical difficulty quests")
            BenefitRow("Unlimited App Blocker slots during focus sessions")
            BenefitRow("AI Hub: Interactive Chat Mode for instant guidance")
            BenefitRow("AI Hub: Autonomous Agent Mode for smart task planning")
            BenefitRow("AI Hub: Connect your own API key anytime")
            BenefitRow("AI Hub: Run offline local AI models via SaMu Lab (neurasamu.com/store)")
            BenefitRow("Live HP-Drop alert notifications on every drop until 0 HP")
            BenefitRow("Orbiting light-beam Pro animation on HP and EP circles")
            BenefitRow("Exclusive Gold and Black PRO Hunter Nameplate")
            BenefitRow("Full 10-Tier Hunter Rank journey from E-Rank to Eternal Legend")
        }

        Spacer(Modifier.height(14.dp))

        // Tamper-checked QR Payment Section
        if (qrBitmap == null || qrBytes == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF220A0E))
                    .border(1.2.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(30.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "SECURITY INTEGRITY ALERT",
                    color = Color(0xFFEF4444),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Payment assets failed integrity verification. Please install the official SL Tasker build.",
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarkCard.copy(alpha = 0.88f))
                    .border(
                        1.2.dp,
                        Brush.linearGradient(listOf(AccentGold, PrimaryManaBlue)),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "STEP 1: SCAN OR SAVE RAAST QR ($priceText)",
                    color = AccentGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Works with EasyPaisa, JazzCash, SadaPay, NayaPay & All Banks",
                    color = TextMuted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .size(205.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Raast Payment QR",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        val fileName = if (isLifetime) "SL_Tasker_QR_1499.jpg" else "SL_Tasker_QR_199.jpg"
                        val ok = saveQrToGallery(ctx, qrBytes, fileName)
                        Toast.makeText(
                            ctx,
                            if (ok) "QR saved to Gallery (Pictures/SL_Tasker)" else "Could not save QR",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color(0xFF070A12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("SAVE QR TO GALLERY", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                // Quick Wallet Redirect + Account Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A1224))
                        .border(1.dp, Color(0xFF1E2A45), RoundedCornerShape(10.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(localAccount))
                            Toast.makeText(ctx, "Account $localAccount copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = PrimaryManaBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$localAccount  ($accountTitle)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap to copy number for manual transfer",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        tint = AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WalletChip("EasyPaisa", Modifier.weight(1f)) {
                        openWalletWithCopy(ctx, clipboard, localAccount, "pk.com.telenor.phoenix", "EasyPaisa")
                    }
                    WalletChip("JazzCash", Modifier.weight(1f)) {
                        openWalletWithCopy(ctx, clipboard, localAccount, "com.techlogix.mobilinkcustomer", "JazzCash")
                    }
                    WalletChip("SadaPay", Modifier.weight(1f)) {
                        openWalletWithCopy(ctx, clipboard, localAccount, "com.sadapay.app", "SadaPay")
                    }
                    WalletChip("Meezan", Modifier.weight(1f)) {
                        openWalletWithCopy(ctx, clipboard, localAccount, "com.meezanbank.mb", "Meezan")
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Step 2: Upload screenshot & auto-attach to WhatsApp
                Text(
                    "STEP 2: SEND SCREENSHOT TO GET ACTIVATION CODE",
                    color = PrimaryManaBlue,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { screenshotPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (selectedProofUri != null) "RESEND SCREENSHOT ON WHATSAPP" else "UPLOAD SCREENSHOT TO WHATSAPP",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(deviceToken))
                        val msg = "Hi! I want to buy SL Tasker $planLabel. My Device ID: $deviceToken"
                        openWhatsAppChat(ctx, waNumber, msg)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, tint = PrimaryManaBlue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "MESSAGE ON WHATSAPP (WITHOUT SCREENSHOT)",
                        color = PrimaryManaBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Step 3: Enter Activation Code
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard.copy(alpha = 0.8f))
                .border(1.dp, PrimaryManaBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showActivation = !showActivation },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "HAVE AN ACTIVATION CODE?",
                    color = AccentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                Text(
                    if (showActivation) "HIDE" else "ENTER CODE",
                    color = PrimaryManaBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showActivation) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A1224))
                        .clickable {
                            clipboard.setText(AnnotatedString(deviceToken))
                            Toast.makeText(ctx, "Device ID copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Device ID: $deviceToken",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = PrimaryManaBlue,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = activationInput,
                    onValueChange = {
                        activationInput = it.uppercase().take(12)
                        activationError = null
                    },
                    label = { Text("8-Character Activation Code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = PrimaryManaBlue.copy(alpha = 0.4f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = AccentGold,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (activationError != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(activationError!!, color = Color(0xFFEF4444), fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (LicenseManager.verifyActivationCode(ctx, activationInput)) {
                            Toast.makeText(ctx, "SL Tasker PRO Unlocked!", Toast.LENGTH_LONG).show()
                            onUnlocked()
                        } else {
                            activationError = "Invalid code for this device."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color(0xFF070A12)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("UNLOCK PRO NOW", fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    subtitle: String,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderBrush = if (selected) {
        Brush.linearGradient(listOf(Color(0xFFFFF5B8), AccentGold, Color(0xFFFF9800)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF1E2A45), Color(0xFF1E2A45)))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF17140A) else DarkCard.copy(alpha = 0.7f))
            .border(if (selected) 1.6.dp else 1.dp, borderBrush, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGold)
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(badge, color = Color(0xFF070A12), fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(
            title,
            color = if (selected) AccentGold else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(3.dp))
        Text(
            price,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Text(subtitle, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = TextPrimary, fontSize = 11.sp)
    }
}

@Composable
private fun WalletChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF101A33))
            .border(1.dp, PrimaryManaBlue.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = Color(0xFF93C5FD),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

private fun saveQrToGallery(context: Context, bytes: ByteArray, fileName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SL_Tasker")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (_: Throwable) {
        false
    }
}

private fun openWalletWithCopy(
    context: Context,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    account: String,
    pkg: String,
    label: String
) {
    clipboard.setText(AnnotatedString(account))
    Toast.makeText(context, "Account $account copied. Opening $label...", Toast.LENGTH_SHORT).show()
    try {
        val launch = context.packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        } else {
            Toast.makeText(context, "$label not installed. Account number copied.", Toast.LENGTH_SHORT).show()
        }
    } catch (_: Throwable) {}
}

private fun sendProofToWhatsApp(context: Context, waNumber: String, imageUri: Uri, message: String) {
    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
    for (pkg in packages) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                setPackage(pkg)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", "$waNumber@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Throwable) {}
    }
    try {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(fallback, "Send Payment Proof").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (_: Throwable) {
        openWhatsAppChat(context, waNumber, message)
    }
}

private fun openWhatsAppChat(context: Context, waNumber: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$waNumber?text=" + Uri.encode(message))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show()
    }
}
