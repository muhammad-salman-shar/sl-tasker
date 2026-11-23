package com.neurasamu.build.sl_tasker.ui.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurasamu.build.sl_tasker.data.ai.AiConfig
import com.neurasamu.build.sl_tasker.data.ai.AiProvider
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.ui.components.AiHubLogo
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextOnBlue
import com.neurasamu.build.sl_tasker.ui.theme.TextOnGold
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.theme.TextSecondary

@Composable
fun AiSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AiViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    val config by vm.config.collectAsState(initial = AiConfig())
    val testState by vm.testState.collectAsState()
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showRaw by remember { mutableStateOf(false) }
    val rawReq by vm.lastRawRequest.collectAsState()
    val rawRes by vm.lastRawResponse.collectAsState()
    val ctx = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp, max = 480.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .imePadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiHubLogo(size = 30.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "AI HUB",
                        color = PrimaryManaBlue,
                        fontSize = 13.sp,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(6.dp))
                    val isPro = remember { LicenseManager.isPro(ctx) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPro) AccentGold else DangerPenaltyRed)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            if (isPro) "PRO" else "FREE",
                            color = if (isPro) TextOnGold else TextOnBlue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    if (config.isConfigured) "${config.provider.displayName} · ${config.model}" else "Not configured",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            ) {
                listOf(false to "AGENT", true to "CHAT").forEach { (flag, label) ->
                    val sel = config.chatMode == flag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (sel) PrimaryManaBlue.copy(alpha = 0.22f) else DarkSurface)
                            .clickable { vm.setChatMode(flag) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            label,
                            color = if (sel) PrimaryManaBlue else TextSecondary,
                            fontSize = 9.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = { showRaw = !showRaw }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.BugReport, contentDescription = "Raw debug", tint = if (showRaw) PrimaryManaBlue else TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { vm.clearChat() }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Clear chat", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = {
                vm.resetTestState()
                showSettings = true
            }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = PrimaryManaBlue, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBorder)
        )

        if (showRaw) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(10.dp)
            ) {
                Text("REQUEST", color = PrimaryManaBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(rawReq ?: "—", color = TextSecondary, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Text("RESPONSE", color = PrimaryManaBlue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(rawRes ?: "—", color = TextSecondary, fontSize = 10.sp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DarkBorder)
            )
        }

        // Messages
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Ask me to create a task, alarm, or focus block.",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "e.g. \"Study math tomorrow 7 PM for 60 min, hard\"",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        SamuLabCard(ctx)
                    }
                }
            }
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("thinking…", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message…", color = TextMuted, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryManaBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                )
            )
            Spacer(Modifier.width(8.dp))
            if (busy) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DangerPenaltyRed)
                        .clickable { vm.cancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop",
                        tint = TextOnBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (input.isBlank()) DarkSurface else PrimaryManaBlue)
                        .clickable(enabled = input.isNotBlank()) {
                            vm.send(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Send",
                        tint = if (input.isBlank()) TextMuted else TextOnBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (showSettings) {
        AiSettingsDialog(
            initialProvider = config.provider,
            initialUrl = config.baseUrl,
            initialKey = config.apiKey,
            initialModel = config.model,
            initialTimeout = config.timeoutSec,
            initialStream = config.useStream,
            initialJson = config.useJsonFormat,
            testState = testState,
            onTest = { provider, url, key, model, timeout ->
                vm.testConnection(provider, url, key, model, timeout)
            },
            onDismiss = {
                vm.resetTestState()
                showSettings = false
            },
            onSave = { provider, url, key, model, timeout, streamOn, jsonOn ->
                vm.saveConfig(provider, url, key, model, timeout, streamOn, jsonOn)
                vm.resetTestState()
                showSettings = false
            }
        )
    }
}

@Composable
private fun SamuLabCard(ctx: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, PrimaryManaBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://neurasamu.com"))
                    ctx.startActivity(intent)
                } catch (_: Exception) {}
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Run Local AI with SaMu Lab",
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Host private offline models on your PC/phone & connect to SL Tasker. Download at neurasamu.com",
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = "Open neurasamu.com",
            tint = PrimaryManaBlue,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(msg: AiMessage) {
    val ctx = LocalContext.current
    val isUser = msg.role == "user"
    val isError = msg.role == "error"
    val isInfo = msg.role == "info"

    if (isInfo) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = msg.text,
                color = SuccessGreen,
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelMedium
            )
        }
        return
    }

    val bgColor = when {
        isError -> DangerPenaltyRed.copy(alpha = 0.16f)
        isUser -> PrimaryManaBlue.copy(alpha = 0.18f)
        else -> DarkSurface
    }
    val borderColor = when {
        isError -> DangerPenaltyRed.copy(alpha = 0.6f)
        isUser -> PrimaryManaBlue.copy(alpha = 0.45f)
        else -> DarkBorder
    }
    val textColor = if (isError) TextPrimary else TextPrimary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("ai_msg", msg.text))
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (isError) {
                Text(
                    text = "ERROR",
                    color = DangerPenaltyRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = msg.text,
                color = textColor,
                fontSize = 12.sp
            )
            if (msg.raw != null && isError) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "raw: " + msg.raw,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AiSettingsDialog(
    initialProvider: AiProvider,
    initialUrl: String,
    initialKey: String,
    initialModel: String,
    initialTimeout: Int,
    initialStream: Boolean,
    initialJson: Boolean,
    testState: ConnectionTestState,
    onTest: (AiProvider, String, String, String, Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (AiProvider, String, String, String, Int, Boolean, Boolean) -> Unit
) {
    val ctx = LocalContext.current
    var provider by remember { mutableStateOf(initialProvider) }
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }
    var model by remember { mutableStateOf(initialModel) }
    var timeout by remember { mutableStateOf(initialTimeout.toString()) }
    var useStream by remember { mutableStateOf(initialStream) }
    var useJson by remember { mutableStateOf(initialJson) }

    val urlPlaceholder = when (provider) {
        AiProvider.LOCAL_CUSTOM -> "http://192.168.1.10:11434 or http://127.0.0.1:8080"
        AiProvider.OPENAI_COMPATIBLE -> "https://api.openai.com/v1"
        AiProvider.GEMINI -> "https://generativelanguage.googleapis.com (optional)"
    }
    val modelPlaceholder = when (provider) {
        AiProvider.LOCAL_CUSTOM -> "qwen2.5:3b / llama3.2"
        AiProvider.OPENAI_COMPATIBLE -> "gpt-4o-mini"
        AiProvider.GEMINI -> "gemini-2.5-flash"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("AI SETTINGS", color = PrimaryManaBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Provider", color = TextSecondary, fontSize = 10.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiProvider.entries.forEach { p ->
                        val selected = provider == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PrimaryManaBlue else DarkSurface)
                                .border(1.dp, if (selected) PrimaryManaBlue else DarkBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    provider = p
                                    if (p == AiProvider.GEMINI && url.isBlank()) {
                                        url = "https://generativelanguage.googleapis.com"
                                    }
                                    if (p == AiProvider.LOCAL_CUSTOM && (timeout.toIntOrNull() ?: 0) < 60) {
                                        timeout = "90"
                                    }
                                }
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.displayName,
                                color = if (selected) TextOnBlue else TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Field("Base URL", url, { url = it }, urlPlaceholder)
                Spacer(Modifier.height(8.dp))
                Field(
                    if (provider == AiProvider.LOCAL_CUSTOM) "API Key (optional for local)" else "API Key",
                    key,
                    { key = it },
                    if (provider == AiProvider.GEMINI) "AIza…" else "sk-…",
                    password = true
                )
                Spacer(Modifier.height(8.dp))
                Field("Model", model, { model = it }, modelPlaceholder)
                Spacer(Modifier.height(8.dp))
                Field("Timeout (10–180 sec)", timeout, { v -> timeout = v.filter { it.isDigit() }.take(3) }, "90")
                Spacer(Modifier.height(10.dp))

                if (provider != AiProvider.GEMINI) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Streaming (SSE)", color = TextPrimary, fontSize = 11.sp)
                            Text("Disable if local server errors", color = TextSecondary, fontSize = 9.sp)
                        }
                        Switch(
                            checked = useStream,
                            onCheckedChange = { useStream = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnBlue,
                                checkedTrackColor = PrimaryManaBlue
                            )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Force JSON mode", color = TextPrimary, fontSize = 11.sp)
                        Text("Recommended for Agent commands", color = TextSecondary, fontSize = 9.sp)
                    }
                    Switch(
                        checked = useJson,
                        onCheckedChange = { useJson = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextOnBlue,
                            checkedTrackColor = PrimaryManaBlue
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        onTest(provider, url, key, model, (timeout.toIntOrNull() ?: 90).coerceIn(10, 180))
                    },
                    enabled = testState !is ConnectionTestState.Testing,
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(PrimaryManaBlue)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (testState is ConnectionTestState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = PrimaryManaBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Testing connection…", color = PrimaryManaBlue, fontSize = 11.sp)
                    } else {
                        Text("Test Connection", color = PrimaryManaBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (testState is ConnectionTestState.Done) {
                    val res = testState.result
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (res.success) SuccessGreen.copy(alpha = 0.14f)
                                else DangerPenaltyRed.copy(alpha = 0.14f)
                            )
                            .border(
                                1.dp,
                                if (res.success) SuccessGreen.copy(alpha = 0.5f)
                                else DangerPenaltyRed.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = res.message,
                            color = TextPrimary,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                        if (res.endpoint.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = res.endpoint,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                SamuLabCard(ctx)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        provider,
                        url,
                        key,
                        model,
                        (timeout.toIntOrNull() ?: 90).coerceIn(10, 180),
                        useStream,
                        useJson
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = TextOnBlue
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    password: Boolean = false
) {
    Column {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text(placeholder, color = TextMuted, fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text
            ),
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
