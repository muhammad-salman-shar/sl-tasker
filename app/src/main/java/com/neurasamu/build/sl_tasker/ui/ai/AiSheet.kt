package com.neurasamu.build.sl_tasker.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurasamu.build.sl_tasker.ui.components.AiHubLogo
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary

@Composable
fun AiSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    vm: AiViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val busy by vm.busy.collectAsState()
    val config by vm.config.collectAsState(initial = com.neurasamu.build.sl_tasker.data.ai.AiConfig())
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp, max = 460.dp)
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
                Text("AI HUB", color = PrimaryManaBlue, fontSize = 13.sp, style = MaterialTheme.typography.labelLarge)
                Text(
                    if (config.isConfigured) config.model else "Not configured",
                    color = TextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = PrimaryManaBlue)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextMuted)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBorder)
        )

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
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Ask me to create a task or alarm.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "e.g. \"Study math tomorrow 7 PM for 60 min, hard\"",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            items(messages) { msg ->
                MessageBubble(msg)
            }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("thinking…", color = TextMuted, fontSize = 11.sp)
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (busy || input.isBlank()) DarkSurface else PrimaryManaBlue)
                    .clickable(enabled = !busy && input.isNotBlank()) {
                        vm.send(input)
                        input = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (busy || input.isBlank()) TextMuted else DarkBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showSettings) {
        AiSettingsDialog(
            initialUrl = config.baseUrl,
            initialKey = config.apiKey,
            initialModel = config.model,
            initialTimeout = config.timeoutSec,
            onDismiss = { showSettings = false },
            onSave = { url, key, model, timeout ->
                vm.saveConfig(url, key, model, timeout)
                showSettings = false
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: AiMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isUser) PrimaryManaBlue.copy(alpha = 0.18f) else DarkSurface)
                .border(
                    1.dp,
                    if (isUser) PrimaryManaBlue.copy(alpha = 0.4f) else DarkBorder,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.text,
                color = if (isUser) TextPrimary else TextPrimary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AiSettingsDialog(
    initialUrl: String,
    initialKey: String,
    initialModel: String,
    initialTimeout: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int) -> Unit
) {
    var url by remember { mutableStateOf(initialUrl) }
    var key by remember { mutableStateOf(initialKey) }
    var model by remember { mutableStateOf(initialModel) }
    var timeout by remember { mutableStateOf(initialTimeout.toString()) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("AI SETTINGS", color = PrimaryManaBlue, fontSize = 14.sp)
        },
        text = {
            Column {
                Field("Base URL", url, { url = it }, "https://api.openai.com/v1")
                Spacer(Modifier.height(8.dp))
                Field("API Key", key, { key = it }, "sk-…", password = true)
                Spacer(Modifier.height(8.dp))
                Field("Model", model, { model = it }, "gpt-4o-mini")
                Spacer(Modifier.height(8.dp))
                Field("Timeout (sec)", timeout, { v -> timeout = v.filter { it.isDigit() }.take(3) }, "30")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Anything OpenAI-compatible works (OpenRouter, Groq, LM Studio, Ollama).",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    onSave(
                        url,
                        key,
                        model,
                        (timeout.toIntOrNull() ?: 30)
                    )
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = PrimaryManaBlue,
                    contentColor = DarkBackground
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
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
        Text(label, color = TextMuted, fontSize = 10.sp)
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
