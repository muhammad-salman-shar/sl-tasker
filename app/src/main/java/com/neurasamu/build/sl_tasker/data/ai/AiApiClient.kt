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

    fun buildSystemPrompt(chatMode: Boolean = false): String {
        val now = java.util.Calendar.getInstance()
        val h = now.get(java.util.Calendar.HOUR_OF_DAY)
        val mi = now.get(java.util.Calendar.MINUTE)
        val d = now.get(java.util.Calendar.DAY_OF_MONTH)
        val mo = now.get(java.util.Calendar.MONTH) + 1
        val y = now.get(java.util.Calendar.YEAR)
        val nowTime = "%02d:%02d".format(h, mi)

        return """
You are an API that outputs ONE JSON object. No text outside JSON.

Now: $d/$mo/$y $nowTime. Days: 0=Sun,1=Mon,2=Tue,3=Wed,4=Thu,5=Fri,6=Sat. Always output 24-hour HH:MM.

TASK schema:
{"action":"create_task","title":"<name>","time":"<HH:MM>","days":"","type":"MEDIUM|HARD|CRITICAL","duration_minutes":0}
- duration_minutes only needed for HARD or CRITICAL. Use 0 for MEDIUM.

ALARM schema:
{"action":"create_alarm","label":"<name>","time":"<HH:MM>","days":"","dismiss_method":"EASY|PIN|MATH","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}
- dismiss_method default "EASY". If user asks for a PIN, use "PIN" and fill pin_code with 8 digits. If user asks math challenge, use "MATH".
- pin_code: ONLY when dismiss_method is PIN. Must be 8 digits, must not start with "12345", must not be all same digit.
- snooze_enabled default true. snooze_minutes default 10. vibrate default true.

CHAT schema (only when info is missing or user is just chatting):
{"action":"chat","reply":"<short sentence>"}

Rules:
- Extract everything from the user message. Do not invent values.
- If user did not specify a field, use the default.
- If time is missing for a task/alarm, use CHAT and ask for time in one short sentence.
- Output JSON only. No markdown. No explanation.

${if (chatMode) """
CONVERSATION MODE:
- You have context of previous turns. Remember what the user already told you.
- If they give a missing field later ("make it 6 AM"), combine it with previous intent.
- Stay conversational but still return JSON only.
""" else ""}

Examples:

IN: Study math tomorrow 7pm for 1 hour, hard
OUT: {"action":"create_task","title":"Study math","time":"19:00","days":"","type":"HARD","duration_minutes":60}

IN: Wake me at 6:30 AM on weekdays
OUT: {"action":"create_alarm","label":"Wake up","time":"06:30","days":"1,2,3,4,5","dismiss_method":"EASY","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}

IN: Set an alarm at 5 AM tomorrow with a PIN 58294017, snooze 15
OUT: {"action":"create_alarm","label":"Alarm","time":"05:00","days":"","dismiss_method":"PIN","pin_code":"58294017","snooze_enabled":true,"snooze_minutes":15,"vibrate":true}

IN: Hi
OUT: {"action":"chat","reply":"Hi. Tell me a task or alarm."}
""".trimIndent()
    }

    @Deprecated("Use buildSystemPrompt()")
    val SYSTEM: String = ""
}
