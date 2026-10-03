package com.neurasamu.build.sl_tasker.ui.player

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
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
import kotlinx.coroutines.launch

private val PROTECTED_PKGS = setOf(
    "com.neurasamu.build.sl_tasker",
    "com.android.settings",
    "com.android.systemui",
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.mms",
    "com.google.android.apps.messaging",
    "com.android.phone"
)

data class AppRow(val pkg: String, val label: String)

@Composable
fun PlayerSettingsScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { BlockPrefs(context) }
    val state by prefs.state.collectAsState(initial = null)
    var query by remember { mutableStateOf("") }
    val allApps = remember { loadInstalledApps(context) }

    val filtered = remember(query, allApps) {
        if (query.isBlank()) allApps
        else allApps.filter {
            it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true)
        }
    }

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
                    text = "BLOCK SYSTEM",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryManaBlue,
                    fontSize = 16.sp
                )
            }
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            val s = state ?: return@Column

            Spacer(Modifier.height(8.dp))

            val hasAccess = remember { isAccessibilityEnabled(context) }
            SettingsCard {
                Text("PERMISSIONS", color = PrimaryManaBlue, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (hasAccess) "Accessibility: GRANTED" else "Accessibility: NOT GRANTED",
                    color = if (hasAccess) SuccessGreen else DangerPenaltyRed,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryManaBlue.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryManaBlue, RoundedCornerShape(8.dp))
                        .clickable {
                            val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(i)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasAccess) "OPEN ACCESSIBILITY SETTINGS" else "GRANT ACCESSIBILITY",
                        color = PrimaryManaBlue,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TEST MODE", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            "Blocks selected apps temporarily.",
                            color = TextMuted,
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

            Spacer(Modifier.height(10.dp))

            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("STRICT MODE", color = TextPrimary, fontSize = 12.sp)
                        Text(
                            "Blocks all apps except protected.",
                            color = TextMuted,
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

            Spacer(Modifier.height(10.dp))

            Text("APP LIST", color = PrimaryManaBlue, fontSize = 11.sp)
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
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items = filtered, key = { it.pkg }) { row ->
                    val isChecked = row.pkg in s.selectedPackages
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
                            Text(row.label, color = TextPrimary, fontSize = 12.sp)
                            Text(row.pkg, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                        }
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { on ->
                                scope.launch { prefs.togglePackage(row.pkg, on) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkCard,
                                checkedTrackColor = PrimaryManaBlue
                            )
                        )
                    }
                }
            }
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
            if (pkg in PROTECTED_PKGS) return@mapNotNull null
            AppRow(pkg = pkg, label = info.loadLabel(pm).toString())
        }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val service = "${context.packageName}/com.neurasamu.build.sl_tasker.service.AppBlockerAccessibilityService"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(service, ignoreCase = true)
}
