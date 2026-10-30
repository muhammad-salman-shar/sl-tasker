package com.neurasamu.build.sl_tasker.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMsg(val role: String, val content: String)

class AiApiClient {

    suspend fun chat(config: AiConfig, messages: List<ChatMsg>): String =
        withContext(Dispatchers.IO) {
            val endpoint = buildEndpoint(config.baseUrl)

            val body = JSONObject().apply {
                put("model", config.model)
                put("temperature", 0.1)
                put("max_tokens", 512)
                if (config.useStream) put("stream", true)
                if (config.useJsonFormat) {
                    put("response_format", JSONObject().apply { put("type", "json_object") })
                }
                put("messages", JSONArray().apply {
                    messages.forEach { m ->
                        put(JSONObject().apply {
                            put("role", m.role)
                            put("content", m.content)
                        })
                    }
                })
            }

            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = config.timeoutSec * 1000
                readTimeout = config.timeoutSec * 1000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream, application/json")
                if (config.apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${config.apiKey}")
                }
            }

            try {
                OutputStreamWriter(conn.outputStream, "UTF-8").use { w ->
                    w.write(body.toString())
                    w.flush()
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream

                if (code !in 200..299) {
                    val errText = BufferedReader(InputStreamReader(stream, "UTF-8"))
                        .use { it.readText() }
                    throw RuntimeException("HTTP $code: ${errText.take(200)}")
                }

                val contentType = (conn.contentType ?: "").lowercase()
                val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))

                val result = if (config.useStream && contentType.contains("text/event-stream")) {
                    readSse(reader)
                } else {
                    // Even if stream was requested, some servers ignore it and send JSON.
                    val whole = reader.readText()
                    val trimmed = whole.trimStart()
                    if (trimmed.startsWith("data:")) {
                        // It's actually SSE in a single blob
                        parseSseBlob(whole)
                    } else {
                        parseJsonWhole(whole)
                    }
                }

                if (result.isBlank()) throw RuntimeException("Empty response")
                result
            } finally {
                conn.disconnect()
            }
        }

    private fun readSse(reader: BufferedReader): String {
        val sb = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            val l = line.trim()
            if (l.startsWith("data:")) {
                val data = l.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isNotEmpty()) {
                    try {
                        val obj = JSONObject(data)
                        val delta = obj.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content", "")
                        if (!delta.isNullOrEmpty()) sb.append(delta)
                    } catch (_: Exception) {}
                }
            }
            line = reader.readLine()
        }
        return sb.toString()
    }

    private fun parseJsonWhole(text: String): String {
        val root = JSONObject(text)
        // Some servers return {"error": {...}}
        root.optJSONObject("error")?.let { err ->
            throw RuntimeException("Server error: ${err.optString("message", "unknown")}")
        }
        val choices = root.optJSONArray("choices")
            ?: throw RuntimeException("No choices in response: ${text.take(200)}")
        val first = choices.optJSONObject(0)
            ?: throw RuntimeException("Empty choices")
        val msg = first.optJSONObject("message")
            ?: throw RuntimeException("No message")
        return msg.optString("content", "")
    }

    private fun parseSseBlob(blob: String): String {
        val sb = StringBuilder()
        blob.split("\n").forEach { rawLine ->
            val l = rawLine.trim()
            if (l.startsWith("data:")) {
                val data = l.removePrefix("data:").trim()
                if (data == "[DONE]") return@forEach
                if (data.isNotEmpty()) {
                    try {
                        val obj = JSONObject(data)
                        val delta = obj.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content", "")
                        if (!delta.isNullOrEmpty()) sb.append(delta)
                    } catch (_: Exception) {}
                }
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes user-supplied base URL into a full /chat/completions endpoint.
     * Handles common mistakes:
     *   - 0.0.0.0      -> 127.0.0.1   (0.0.0.0 is a bind address, not routable)
     *   - missing http:// -> prepend http://
     *   - trailing slashes removed
     *   - if URL already ends with /chat/completions, use as-is
     *   - if URL ends with /v1 (or /v1/), append /chat/completions
     *   - otherwise append /v1/chat/completions
     */
    private fun buildEndpoint(raw: String): String {
        var s = raw.trim()
        if (s.isBlank()) throw RuntimeException("Base URL is empty")

        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "http://$s"
        }

        // Replace 0.0.0.0 host with 127.0.0.1 (only the host part)
        s = s.replace("://0.0.0.0", "://127.0.0.1")

        while (s.endsWith("/")) s = s.dropLast(1)

        if (s.endsWith("/chat/completions")) return s

        if (s.endsWith("/v1")) return "$s/chat/completions"

        return "$s/v1/chat/completions"
    }
}

object AiPrompt {

    private fun nowString(): String {
        val now = java.util.Calendar.getInstance()
        val h = now.get(java.util.Calendar.HOUR_OF_DAY)
        val mi = now.get(java.util.Calendar.MINUTE)
        val d = now.get(java.util.Calendar.DAY_OF_MONTH)
        val mo = now.get(java.util.Calendar.MONTH) + 1
        val y = now.get(java.util.Calendar.YEAR)
        val nowTime = "%02d:%02d".format(h, mi)
        return "$d/$mo/$y $nowTime"
    }

    /** Chat mode: natural conversation, no JSON. */
    fun buildChatPrompt(): String = """
You are SL Tasker, a friendly assistant inside a productivity app.
You help the user with tasks, alarms, habits, focus, and stats.

- Reply in plain English. Never output JSON or code unless the user asks.
- Keep replies short (1-3 sentences).
- If the user asks you to create/delete/modify a task or alarm, tell them to switch to AGENT mode (button at the top).
- Be warm, direct, and helpful.
- Current time: ${nowString()}
""".trimIndent()

    /** Agent mode: structured JSON commands. */
    fun buildAgentPrompt(): String = """
You are SL Tasker Agent. Convert the user's English into ONE JSON object. Output only JSON. No prose.

Now: ${nowString()}. Days: 0=Sun,1=Mon,2=Tue,3=Wed,4=Thu,5=Fri,6=Sat.

ACTIONS:

create_task:
{"action":"create_task","title":"T","time":"HH:MM","days":"","type":"MEDIUM","duration_minutes":0}
- duration_minutes only for HARD/CRITICAL. 0 for MEDIUM.

create_alarm:
{"action":"create_alarm","label":"L","time":"HH:MM","days":"","dismiss_method":"EASY","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}
- pin_code only if dismiss_method is PIN (8 digits, not starting with 12345, not all same).

delete_task: {"action":"delete_task","title":"T"}
delete_alarm: {"action":"delete_alarm","label":"L"}
complete_task: {"action":"complete_task","title":"T"}
list_tasks: {"action":"list_tasks"}
list_alarms: {"action":"list_alarms"}
show_stats: {"action":"show_stats"}
set_block_mode: {"action":"set_block_mode","mode":"OFF|TEST|STRICT"}

chat (when info is missing or question is conversational):
{"action":"chat","reply":"short sentence"}

Rules:
- Reply with JSON only. No markdown.
- Use the defaults when a field is missing.
- If time is missing for create_task/create_alarm, use chat and ask for time.
- Never invent data.
""".trimIndent()
}
