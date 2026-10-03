package com.neurasamu.build.sl_tasker.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.sl_tasker.data.ai.AiApiClient
import com.neurasamu.build.sl_tasker.data.ai.AiConfig
import com.neurasamu.build.sl_tasker.data.ai.AiPrompt
import com.neurasamu.build.sl_tasker.data.ai.AiSettings
import com.neurasamu.build.sl_tasker.data.ai.ChatMsg
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.AlarmEntity
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.DismissMethod
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.data.repository.AlarmRepository
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import com.neurasamu.build.sl_tasker.domain.scheduler.ScheduleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

data class AiMessage(val role: String, val text: String, val raw: String? = null)

class AiViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = AiSettings(app)
    private val client = AiApiClient()
    private val db = AppDatabase.getInstance(app)
    private val taskRepo = TaskRepository(db)
    private val alarmRepo = AlarmRepository(db.alarmDao())
    private val scheduler = AlarmScheduler(app)

    private val _messages = MutableStateFlow<List<AiMessage>>(emptyList())
    val messages: StateFlow<List<AiMessage>> = _messages

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _lastRawRequest = MutableStateFlow<String?>(null)
    val lastRawRequest: StateFlow<String?> = _lastRawRequest

    private val _lastRawResponse = MutableStateFlow<String?>(null)
    val lastRawResponse: StateFlow<String?> = _lastRawResponse

    fun clearChat() {
        _messages.value = emptyList()
        _error.value = null
        _lastRawRequest.value = null
        _lastRawResponse.value = null
    }

    val config = settings.config

    fun dismissError() { _error.value = null }

    fun saveConfig(
        baseUrl: String,
        key: String,
        model: String,
        timeoutSec: Int,
        useStream: Boolean,
        useJsonFormat: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.save(baseUrl, key, model, timeoutSec, useStream, useJsonFormat)
        }
    }

    fun send(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _busy.value) return

        _messages.value = _messages.value + AiMessage("user", trimmed)
        _busy.value = true
        _error.value = null
        _lastRawRequest.value = null
        _lastRawResponse.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cfg = settings.config.first()
                if (!cfg.isConfigured) {
                    _messages.value = _messages.value + AiMessage("error",
                        "Configure API URL + Model in settings (top-right).")
                    return@launch
                }

                val sysMsg = ChatMsg("system", AiPrompt.buildSystemPrompt())
                // Only real user + assistant messages — never "error" entries
                val history = _messages.value
                    .filter { it.role == "user" || it.role == "assistant" }
                    .map { ChatMsg(it.role, it.text) }

                _lastRawRequest.value = buildString {
                    append("URL: ").append(cfg.baseUrl).append('\n')
                    append("Model: ").append(cfg.model).append('\n')
                    append("Stream: ").append(cfg.useStream).append('\n')
                    append("JSON mode: ").append(cfg.useJsonFormat).append('\n')
                    append("System prompt length: ").append(AiPrompt.buildSystemPrompt().length).append(" chars\n")
                    append("Messages sent: ").append(history.size)
                }

                val raw = try {
                    client.chat(cfg, listOf(sysMsg) + history)
                } catch (e: Exception) {
                    _lastRawResponse.value = "NETWORK ERROR: ${e.message ?: "unknown"}"
                    _messages.value = _messages.value + AiMessage("error",
                        "Network error: ${e.message ?: "unknown"}")
                    return@launch
                }

                _lastRawResponse.value = raw

                val cleaned = stripFences(raw)
                val json = try {
                    JSONObject(cleaned)
                } catch (_: Exception) {
                    _messages.value = _messages.value + AiMessage("error",
                        "Model did not return JSON.",
                        raw = raw.take(600)
                    )
                    return@launch
                }

                handleAction(json, cfg)
            } catch (t: Throwable) {
                _error.value = t.message ?: "Unknown error"
            } finally {
                _busy.value = false
            }
        }
    }

    private suspend fun handleAction(json: JSONObject, cfg: AiConfig) {
        val action = json.optString("action", "chat")
        when (action) {
            "create_task" -> {
                val title = json.optString("title").trim()
                val timeStr = json.optString("time").trim()
                val days = json.optString("days").trim()
                val typeStr = json.optString("type", "MEDIUM").uppercase()
                val durationMin = json.optInt("duration_minutes", 0)

                val time = parseHm(timeStr)
                if (title.isBlank() || time == null) {
                    _messages.value = _messages.value + AiMessage("assistant",
                        "Need task title and time (HH:MM).")
                    return
                }

                val difficulty = when (typeStr) {
                    "HARD" -> Difficulty.HARD
                    "CRITICAL" -> Difficulty.CRITICAL
                    else -> Difficulty.MEDIUM
                }
                val dur = if (difficulty == Difficulty.MEDIUM) 0
                          else durationMin.coerceAtLeast(1)

                val (hh, mm) = time
                val reminderMin = hh * 60 + mm
                val repeat = if (days.isBlank()) RepeatRule.ONCE else RepeatRule.CUSTOM
                val next = ScheduleHelper.nextTrigger(
                    hour = hh, minute = mm, daysCsv = days, repeat = repeat
                ) ?: (System.currentTimeMillis() + 60_000L)
                val deadline = next + dur * 60_000L

                val task = TaskEntity(
                    title = title,
                    description = "",
                    priority = com.neurasamu.build.sl_tasker.data.model.Priority.MEDIUM,
                    difficulty = difficulty,
                    repeatRule = repeat,
                    customRepeatDays = days,
                    durationMinutes = dur,
                    reminderMinutesOfDay = reminderMin
                )
                val (_, occId) = taskRepo.createTaskWithOccurrence(task, next, deadline)
                scheduler.scheduleReminder(occId, next, title)

                val durText = if (dur > 0) ", ${dur} min" else ""
                _messages.value = _messages.value + AiMessage("assistant",
                    "Task created: \"$title\" at $timeStr ($difficulty$durText).")
            }
            "create_alarm" -> {
                val label = json.optString("label", "Alarm").trim().ifBlank { "Alarm" }
                val timeStr = json.optString("time").trim()
                val days = json.optString("days").trim()
                val time = parseHm(timeStr)
                if (time == null) {
                    _messages.value = _messages.value + AiMessage("assistant",
                        "Need alarm time (HH:MM).")
                    return
                }
                val (hh, mm) = time
                val alarm = AlarmEntity(
                    hour = hh,
                    minute = mm,
                    label = label,
                    repeatRule = if (days.isBlank()) RepeatRule.ONCE else RepeatRule.CUSTOM,
                    customDays = days,
                    enabled = true
                )
                val id = alarmRepo.insertAlarm(alarm)
                scheduler.scheduleAlarm(alarm.copy(id = id))
                _messages.value = _messages.value + AiMessage("assistant",
                    "Alarm set: \"$label\" at $timeStr.")
            }
            else -> {
                val reply = json.optString("reply").trim().ifBlank { "…" }
                _messages.value = _messages.value + AiMessage("assistant", reply)
            }
        }
    }

    private fun parseHm(s: String): Pair<Int, Int>? {
        val parts = s.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private fun stripFences(raw: String): String {
        var s = raw.trim()
        // Remove markdown code fences
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").trim()
            if (s.endsWith("```")) s = s.dropLast(3).trim()
        }
        // Handle a model that wrapped the JSON in a quoted string
        if (s.startsWith("\"") && s.endsWith("\"") && s.length > 2) {
            s = s.substring(1, s.length - 1)
        }
        // Extract first {...} block
        val firstBrace = s.indexOf('{')
        val lastBrace = s.lastIndexOf('}')
        val candidate = if (firstBrace >= 0 && lastBrace > firstBrace) {
            s.substring(firstBrace, lastBrace + 1)
        } else s
        // If the model returned a JSON *string* containing JSON, try to unescape
        return candidate
    }
}
