package com.neurasamu.build.sl_tasker.ui.player

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.block.PROTECTED_PACKAGES
import com.neurasamu.build.sl_tasker.data.datastore.UserPreferences
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.security.PaywallTrigger
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.GoldWarning
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary
import com.neurasamu.build.sl_tasker.ui.theme.ThemeState
import kotlinx.coroutines.launch

data class AppRow(val pkg: String, val label: String)

@Composable
fun PlayerSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { BlockPrefs(context) }
    val userPrefs = remember { UserPreferences(context) }
    val state by prefs.state.collectAsState(initial = null)
    val autoDeleteMissed by userPrefs.autoDeleteMissedTasksFlow.collectAsState(initial = false)
    val todaySteps by com.neurasamu.build.sl_tasker.util.StepTrackerManager.todaySteps.collectAsState()
    val sensorAvailable by com.neurasamu.build.sl_tasker.util.StepTrackerManager.sensorAvailable.collectAsState()
    var showRegisterScanner by remember { mutableStateOf(false) }
    var showGeneratedQr by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val allApps = remember { loadInstalledApps(context) }

    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter {
            it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true)
        }
    }

    BackHandler { onClose() }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryManaBlue
                    )
                }
                Text(
                    text = "SYSTEM & BLOCK SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryManaBlue,
                    fontSize = 16.sp
                )
            }
        }
    ) { pad ->
        val s = state ?: return@Scaffold

        val hasAccess = remember { isAccessibilityEnabled(context) }
        val hasOverlay = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
        }
        val hasBatteryOpt = remember {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        val hasExactAlarm = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else true
        }
        val hasNotif = remember {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val showAccessibilityDisclosure = remember { mutableStateOf(false) }

        val selectablePkgs = remember(allApps) {
            allApps
                .filter { it.pkg != "com.neurasamu.build.sl_tasker" }
                .filterNot { it.pkg in PROTECTED_PACKAGES }
                .map { it.pkg }
                .toSet()
        }
        val allSelected = selectablePkgs.isNotEmpty() && selectablePkgs.all { it in s.selectedPackages }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val activeTheme = ThemeState.activeTheme
                SettingsCard {
                    Text("APPEARANCE & THEMES", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "High-contrast themes calibrated for clear readability",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    val isProUser by com.neurasamu.build.sl_tasker.security.LicenseManager.isProFlow.collectAsState()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.neurasamu.build.sl_tasker.ui.theme.AppThemeId.entries.forEach { tId ->
                            val isSel = activeTheme == tId
                            val lockedPro = tId.isProOnly && !isProUser
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PrimaryManaBlue.copy(alpha = 0.16f) else DarkSurface)
                                    .border(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) PrimaryManaBlue else DarkBorder,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (lockedPro) {
                                            com.neurasamu.build.sl_tasker.security.PaywallTrigger.show(
                                                "${tId.displayName} is a PRO theme. Upgrade to unlock all 8 Hunter Themes!"
                                            )
                                        } else {
                                            ThemeState.selectTheme(tId)
                                            try {
                                                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                                    .edit().putString("active_theme_id", tId.name).apply()
                                            } catch (_: Throwable) {}
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tId.displayName,
                                            color = if (isSel) PrimaryManaBlue else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (tId.isProOnly) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "PRO",
                                                color = com.neurasamu.build.sl_tasker.ui.theme.AccentGold,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Text(
                                        text = tId.subtitle,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = when {
                                        isSel -> "ACTIVE"
                                        lockedPro -> "LOCKED"
                                        else -> "SELECT"
                                    },
                                    color = when {
                                        isSel -> PrimaryManaBlue
                                        lockedPro -> com.neurasamu.build.sl_tasker.ui.theme.AccentGold
                                        else -> TextMuted
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Text("PHYSICAL QR / BARCODE SEAL", color = com.neurasamu.build.sl_tasker.ui.theme.AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (s.hasRegisteredSeal)
                            "Registered: ${s.sealLabel} (${s.sealCode.take(18)})"
                        else
                            "Scan any household barcode/QR or generate an in-app QR to lock apps, alarms & tasks.",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showRegisterScanner = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.neurasamu.build.sl_tasker.ui.theme.AccentCyan,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SCAN BARCODE/QR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val payload = if (s.sealCode.startsWith("SLTASKER-SEAL-")) s.sealCode
                                    else com.neurasamu.build.sl_tasker.util.QrBarcodeHelper.generateSealPayload()
                                    prefs.registerPhysicalSeal(payload, "SL Hunter QR Seal")
                                    showGeneratedQr = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(com.neurasamu.build.sl_tasker.ui.theme.AccentGold)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("GENERATE / PRINT QR", color = com.neurasamu.build.sl_tasker.ui.theme.AccentGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (s.hasRegisteredSeal) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Require Seal for App Blocker", color = TextPrimary, fontSize = 11.sp)
                                Text("Keep blocked apps locked until seal scan or walk quota", color = TextSecondary, fontSize = 9.sp)
                            }
                            Switch(
                                checked = s.sealLockEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch { prefs.setSealLockEnabled(enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkCard,
                                    checkedTrackColor = com.neurasamu.build.sl_tasker.ui.theme.AccentCyan
                                )
                            )
                        }
                    }
                }
            }

            item {
                val stepsToNext = com.neurasamu.build.sl_tasker.util.StepTrackerManager.STEPS_PER_REWARD -
                    (todaySteps % com.neurasamu.build.sl_tasker.util.StepTrackerManager.STEPS_PER_REWARD)
                val earnedMins = s.earnedScrollSeconds / 60
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("WALK-TO-EARN (STEP COUNTER)", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (sensorAvailable)
                                    "Today: $todaySteps steps · $stepsToNext steps to next +25 HP & +10m scroll"
                                else
                                    "Hardware step sensor unavailable on this device.",
                                color = TextPrimary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Earned Scroll Quota Bank: $earnedMins mins",
                                color = com.neurasamu.build.sl_tasker.ui.theme.AccentGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (earnedMins >= 10) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = prefs.activateScrollUnlock(10, consumeEarnedQuota = true)
                                        if (ok) {
                                            android.widget.Toast.makeText(context, "10m scroll pass activated!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SuccessGreen,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("USE 10m", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Text("PERMISSIONS", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    PermissionRow("Notifications", hasNotif) {
                        try {
                            val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(i)
                        } catch (_: Throwable) {}
                    }
                    PermissionRow("Alarms & Reminders", hasExactAlarm) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(i)
                            }
                        } catch (_: Throwable) {}
                    }
                    PermissionRow("Battery Optimization", hasBatteryOpt) {
                        try {
                            val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(i)
                        } catch (_: Throwable) {}
                    }
                    PermissionRow("Display Over Other Apps", hasOverlay) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(i)
                            }
                        } catch (_: Throwable) {}
                    }
                    PermissionRow("Accessibility Service", hasAccess) {
                        if (hasAccess) {
                            try {
                                val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(i)
                            } catch (_: Throwable) {}
                        } else {
                            showAccessibilityDisclosure.value = true
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTO-DELETE MISSED TASKS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Warns before expiry and auto-deletes missed tasks once time runs out",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = autoDeleteMissed,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    userPrefs.setAutoDeleteMissedTasks(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCard,
                                checkedTrackColor = PrimaryManaBlue
                            )
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BATTERY SAVER (LITE GLASS)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Disable heavy animations", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = ThemeState.isLiteMode,
                            onCheckedChange = { isLite ->
                                ThemeState.isLiteMode = isLite
                                try {
                                    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                        .edit().putBoolean("lite_mode", isLite).apply()
                                } catch (_: Throwable) {}
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCard,
                                checkedTrackColor = PrimaryManaBlue
                            )
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TEST MODE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Blocks selected apps temporarily.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = s.mode == BlockMode.TEST,
                            onCheckedChange = { on ->
                                scope.launch {
                                    prefs.setMode(if (on) BlockMode.TEST else BlockMode.OFF)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCard,
                                checkedTrackColor = GoldWarning
                            )
                        )
                    }
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("STRICT MODE", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Blocks all apps except protected.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = s.mode == BlockMode.STRICT,
                            onCheckedChange = { on ->
                                scope.launch {
                                    prefs.setMode(if (on) BlockMode.STRICT else BlockMode.OFF)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCard,
                                checkedTrackColor = DangerPenaltyRed
                            )
                        )
                    }
                }
            }

            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "APP LIST",
                            color = PrimaryManaBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkCard)
                                .border(
                                    1.dp,
                                    if (allSelected) DangerPenaltyRed else DarkBorder,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        if (allSelected) {
                                            prefs.setSelected(emptySet())
                                        } else {
                                            if (!LicenseManager.isPro()) {
                                                PaywallTrigger.show("Blocking all apps requires Pro. Free tier allows 1 app.")
                                            } else {
                                                prefs.setSelected(selectablePkgs)
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (allSelected) "UNSELECT ALL" else "SELECT ALL",
                                color = if (allSelected) DangerPenaltyRed else PrimaryManaBlue,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        placeholder = { Text("Search app", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryManaBlue,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            }

            items(items = filtered, key = { it.pkg }) { row ->
                val isSelf = row.pkg == "com.neurasamu.build.sl_tasker"
                val isChecked = isSelf || row.pkg in s.selectedPackages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isSelf) "SL Tasker (LOCKED)" else row.label,
                            color = if (isSelf) PrimaryManaBlue else TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(row.pkg, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                    }
                    Switch(
                        checked = isChecked,
                        enabled = !isSelf,
                        onCheckedChange = { on ->
                            if (on && !isSelf && !LicenseManager.isPro() && s.selectedPackages.size >= 1) {
                                PaywallTrigger.show("Free users can only block 1 app. Upgrade to Pro for unlimited blocking.")
                            } else {
                                if (!isSelf) scope.launch { prefs.togglePackage(row.pkg, on) }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCard,
                            checkedTrackColor = PrimaryManaBlue
                        )
                    )
                }
            }
        }

        if (showRegisterScanner) {
            com.neurasamu.build.sl_tasker.ui.components.QrScannerDialog(
                title = "REGISTER PHYSICAL SEAL",
                subtitle = "Scan any household barcode (book, bottle, box) or QR code to set it as your lock key",
                expectedCode = null,
                onDismiss = { showRegisterScanner = false },
                onScanned = { scannedValue ->
                    showRegisterScanner = false
                    scope.launch {
                        prefs.registerPhysicalSeal(scannedValue, "Scanned Seal (${scannedValue.take(12)})")
                        android.widget.Toast.makeText(context, "Physical Seal registered!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showGeneratedQr && s.hasRegisteredSeal) {
            com.neurasamu.build.sl_tasker.ui.components.GeneratedQrSealDialog(
                payload = s.sealCode,
                label = s.sealLabel,
                onDismiss = { showGeneratedQr = false }
            )
        }

        if (showAccessibilityDisclosure.value) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDisclosure.value = false },
                containerColor = DarkCard,
                title = {
                    Text(
                        "Accessibility Service Disclosure",
                        color = PrimaryManaBlue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "SL Tasker uses Android's Accessibility Service API strictly for the optional App Blocker focus feature. When enabled, it checks only the active foreground app package name to block apps you selected during low-HP penalty mode or active Critical quests.\n\nSL Tasker does NOT read screen content, messages, passwords, or personal input, and never collects or transmits any accessibility data off your device. You can disable it anytime in Android Settings.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showAccessibilityDisclosure.value = false
                            try {
                                val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(i)
                            } catch (_: Throwable) {}
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryManaBlue
                        )
                    ) {
                        Text("CONTINUE TO SETTINGS", fontSize = 11.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAccessibilityDisclosure.value = false }
                    ) {
                        Text("NOT NOW", color = TextMuted, fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        content()
    }
}

private fun loadInstalledApps(context: Context): List<AppRow> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION")
    val list = pm.queryIntentActivities(intent, 0)
    return list
        .mapNotNull { info ->
            val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
            AppRow(pkg = pkg, label = info.loadLabel(pm).toString())
        }
        .distinctBy { it.pkg }
        .sortedBy {
            if (it.pkg == "com.neurasamu.build.sl_tasker") "" else it.label.lowercase()
        }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.neurasamu.build.sl_tasker.service.AppBlockerAccessibilityService"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(service, ignoreCase = true)
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (granted) SuccessGreen else DangerPenaltyRed)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PrimaryManaBlue.copy(alpha = 0.15f))
                .border(1.dp, PrimaryManaBlue.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .clickable { onOpen() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                if (granted) "MANAGE" else "GRANT",
                color = PrimaryManaBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
