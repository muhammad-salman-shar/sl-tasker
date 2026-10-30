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
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs

data class AiMessage(val role: String, val text: String, val raw: String? = null)

class AiViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = AiSettings(app)
    private val client = AiApiClient()
    private val db = AppDatabase.getInstance(app)
    private val taskRepo = TaskRepository(db)
    private val alarmRepo = AlarmRepository(db.alarmDao())
    private val scheduler = AlarmScheduler(app)
    private val blockPrefs = BlockPrefs(app)

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

    fun setChatMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setChatMode(enabled)
        }
    }

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

                val sysMsg = ChatMsg(
                    "system",
                    if (cfg.chatMode) AiPrompt.buildChatPrompt()
                    else AiPrompt.buildAgentPrompt()
                )
                val history: List<ChatMsg> = if (cfg.chatMode) {
                    // Chat mode: full user/assistant history, plain text
                    _messages.value.mapNotNull { m ->
                        when (m.role) {
                            "user" -> ChatMsg("user", m.text)
                            "assistant" -> ChatMsg("assistant", m.text)
                            else -> null
                        }
                    }
                } else {
                    // Agent mode: single message, no history
                    listOf(ChatMsg("user", trimmed))
                }

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

                if (cfg.chatMode) {
                    // Chat mode: display raw text directly
                    _messages.value = _messages.value + AiMessage("assistant", raw.trim())
                    return@launch
                }

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
                _messages.value = _messages.value + AiMessage("info",
                    "Task created: \"$title\" at $timeStr ($difficulty$durText).")
            }
            "create_alarm" -> {
                val label = json.optString("label", "Alarm").trim().ifBlank { "Alarm" }
                val timeStr = json.optString("time").trim()
                val days = json.optString("days").trim()
                val methodStr = json.optString("dismiss_method", "EASY").uppercase()
                val pinIn = json.optString("pin_code", "").trim()
                val snoozeOn = json.optBoolean("snooze_enabled", true)
                val snoozeMin = json.optInt("snooze_minutes", 10).coerceAtLeast(1)
                val vib = json.optBoolean("vibrate", true)

                val time = parseHm(timeStr)
                if (time == null) {
                    _messages.value = _messages.value + AiMessage("assistant",
                        "Need alarm time (HH:MM).")
                    return
                }
                val method = when (methodStr) {
                    "PIN" -> DismissMethod.PIN
                    "MATH" -> DismissMethod.MATH
                    else -> DismissMethod.EASY
                }
                var pin = ""
                if (method == DismissMethod.PIN) {
                    if (pinIn.length != 8 || !pinIn.all { it.isDigit() }) {
                        _messages.value = _messages.value + AiMessage("assistant",
                            "PIN must be exactly 8 digits.")
                        return
                    }
                    if (pinIn.startsWith("12345")) {
                        _messages.value = _messages.value + AiMessage("assistant",
                            "PIN cannot start with 12345.")
                        return
                    }
                    if (pinIn.toSet().size == 1) {
                        _messages.value = _messages.value + AiMessage("assistant",
                            "PIN cannot be all same digit.")
                        return
                    }
                    pin = pinIn
                }
                val (hh, mm) = time
                val alarm = AlarmEntity(
                    hour = hh,
                    minute = mm,
                    label = label,
                    repeatRule = if (days.isBlank()) RepeatRule.ONCE else RepeatRule.CUSTOM,
                    customDays = days,
                    dismissMethod = method,
                    pinCode = pin,
                    snoozeEnabled = snoozeOn,
                    snoozeMinutes = snoozeMin,
                    vibrate = vib,
                    enabled = true
                )
                val id = alarmRepo.insertAlarm(alarm)
                scheduler.scheduleAlarm(alarm.copy(id = id))
                val extra = buildString {
                    if (method != DismissMethod.EASY) append(" • ${method.name}")
                    if (snoozeOn) append(" • snooze ${snoozeMin}m")
                }
                _messages.value = _messages.value + AiMessage("info",
                    "Alarm set: \"$label\" at $timeStr$extra.")
            }
            "delete_task" -> {
                val title = json.optString("title").trim()
                if (title.isBlank()) {
                    _messages.value = _messages.value + AiMessage("assistant", "Which task title?")
                    return
                }
                try {
                    val all = taskRepo.getAllActiveTasks().first()
                    val match = all.lastOrNull { it.title.equals(title, ignoreCase = true) }
                    if (match == null) {
                        _messages.value = _messages.value + AiMessage("assistant", "No task named \"$title\".")
                    } else {
                        taskRepo.deleteTask(match.id)
                        _messages.value = _messages.value + AiMessage("info", "Task deleted: \"${match.title}\".")
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "Delete failed: ${e.message}")
                }
            }
            "delete_alarm" -> {
                val label = json.optString("label").trim()
                if (label.isBlank()) {
                    _messages.value = _messages.value + AiMessage("assistant", "Which alarm label?")
                    return
                }
                try {
                    val all = alarmRepo.allAlarms.first()
                    val match = all.lastOrNull { it.label.equals(label, ignoreCase = true) }
                    if (match == null) {
                        _messages.value = _messages.value + AiMessage("assistant", "No alarm \"$label\".")
                    } else {
                        alarmRepo.deleteAlarm(match)
                        _messages.value = _messages.value + AiMessage("info", "Alarm deleted: \"${match.label}\".")
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "Delete failed: ${e.message}")
                }
            }
            "complete_task" -> {
                val title = json.optString("title").trim()
                if (title.isBlank()) {
                    _messages.value = _messages.value + AiMessage("assistant", "Which task title?")
                    return
                }
                try {
                    val all = taskRepo.getAllActiveTasks().first()
                    val match = all.lastOrNull { it.title.equals(title, ignoreCase = true) }
                    if (match == null) {
                        _messages.value = _messages.value + AiMessage("assistant", "No task \"$title\".")
                    } else {
                        val occs = taskRepo.getPendingOccurrencesForTask(match.id)
                        val occ = occs.firstOrNull()
                        if (occ == null) {
                            _messages.value = _messages.value + AiMessage("assistant", "No pending occurrence for \"$title\".")
                        } else {
                            val ok = taskRepo.completeOccurrence(occ.id)
                            if (ok) {
                                _messages.value = _messages.value + AiMessage("info", "Task completed: \"${match.title}\".")
                            } else {
                                _messages.value = _messages.value + AiMessage("assistant", "Could not complete \"$title\" right now.")
                            }
                        }
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "Complete failed: ${e.message}")
                }
            }
            "list_tasks" -> {
                try {
                    val all = taskRepo.getAllActiveTasks().first()
                    if (all.isEmpty()) {
                        _messages.value = _messages.value + AiMessage("assistant", "You have no active tasks.")
                    } else {
                        val text = all.take(15).joinToString("\n") { "• ${it.title} (${it.difficulty.name})" }
                        _messages.value = _messages.value + AiMessage("assistant", text)
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "List failed: ${e.message}")
                }
            }
            "list_alarms" -> {
                try {
                    val all = alarmRepo.allAlarms.first()
                    if (all.isEmpty()) {
                        _messages.value = _messages.value + AiMessage("assistant", "You have no alarms.")
                    } else {
                        val text = all.take(15).joinToString("\n") {
                            val hh = it.hour.toString().padStart(2, '0')
                            val mm = it.minute.toString().padStart(2, '0')
                            "• $hh:$mm ${it.label}"
                        }
                        _messages.value = _messages.value + AiMessage("assistant", text)
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "List failed: ${e.message}")
                }
            }
            "show_stats" -> {
                try {
                    val stats = db.playerStatsDao().getPlayerStats()
                    if (stats == null) {
                        _messages.value = _messages.value + AiMessage("assistant", "No stats yet.")
                    } else {
                        val text = "Level ${stats.level} · HP ${stats.health}/100 · EP ${stats.ep} · Streak ${stats.streak} · Completed ${stats.totalCompleted} · Missed ${stats.totalMissed}"
                        _messages.value = _messages.value + AiMessage("assistant", text)
                    }
                } catch (e: Throwable) {
                    _messages.value = _messages.value + AiMessage("assistant", "Stats failed: ${e.message}")
                }
            }
            "set_block_mode" -> {
                val mode = json.optString("mode", "OFF").uppercase()
                val bm = when (mode) {
                    "TEST" -> BlockMode.TEST
                    "STRICT" -> BlockMode.STRICT
                    else -> BlockMode.OFF
                }
                blockPrefs.setMode(bm)
                _messages.value = _messages.value + AiMessage("info", "Block mode: ${bm.name}.")
            }
            else -> {
                val reply = json.optString("reply").trim().ifBlank { "…" }
                _messages.value = _messages.value + AiMessage("assistant", reply, raw = json.toString())
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
