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
                put("stream", true)
                put("response_format", JSONObject().apply { put("type", "json_object") })
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

                val result = if (contentType.contains("text/event-stream")) {
                    readSse(reader)
                } else {
                    readJsonResponse(reader)
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

    private fun readJsonResponse(reader: BufferedReader): String {
        val text = reader.readText()
        val root = JSONObject(text)
        val choices = root.optJSONArray("choices")
            ?: throw RuntimeException("No choices in response")
        val first = choices.optJSONObject(0)
            ?: throw RuntimeException("Empty choices")
        val msg = first.optJSONObject("message")
            ?: throw RuntimeException("No message")
        return msg.optString("content", "")
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

    fun buildSystemPrompt(): String {
        val now = java.util.Calendar.getInstance()
        val y = now.get(java.util.Calendar.YEAR)
        val mo = now.get(java.util.Calendar.MONTH) + 1
        val d = now.get(java.util.Calendar.DAY_OF_MONTH)
        val h = now.get(java.util.Calendar.HOUR_OF_DAY)
        val mi = now.get(java.util.Calendar.MINUTE)
        val dow = when (now.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.SUNDAY -> "Sunday"
            java.util.Calendar.MONDAY -> "Monday"
            java.util.Calendar.TUESDAY -> "Tuesday"
            java.util.Calendar.WEDNESDAY -> "Wednesday"
            java.util.Calendar.THURSDAY -> "Thursday"
            java.util.Calendar.FRIDAY -> "Friday"
            else -> "Saturday"
        }
        val nowTime = "%02d:%02d".format(h, mi)

        return """
You are a JSON API. You receive one English sentence and return one JSON object. Nothing else.

Current: $d/$mo/$y ($dow) at $nowTime. Days: 0=Sun 1=Mon 2=Tue 3=Wed 4=Thu 5=Fri 6=Sat.

Return one of these exactly:

Task:  {"action":"create_task","title":"TITLE","time":"HH:MM","days":"","type":"MEDIUM","duration_minutes":0}
Alarm: {"action":"create_alarm","label":"LABEL","time":"HH:MM","days":""}
Chat:  {"action":"chat","reply":"YOUR REPLY TEXT"}

Field rules:
- time: 24-hour HH:MM
- days: "" for one-time. For repeat use weekday numbers like "1,2,3,4,5"
- type: MEDIUM | HARD | CRITICAL
- duration_minutes: 0 for MEDIUM, otherwise minutes

Follow these examples exactly:

Input: Study math tomorrow at 7 PM for 1 hour, hard
Output: {"action":"create_task","title":"Study math","time":"19:00","days":"","type":"HARD","duration_minutes":60}

Input: Wake me at 6:30 AM on weekdays
Output: {"action":"create_alarm","label":"Wake up","time":"06:30","days":"1,2,3,4,5"}

Input: Code every Monday and Friday at 5 PM for 90 min, hard
Output: {"action":"create_task","title":"Code","time":"17:00","days":"1,5","type":"HARD","duration_minutes":90}

Input: Add a task called read a book
Output: {"action":"chat","reply":"What time should I set for reading?"}

Input: Hi
Output: {"action":"chat","reply":"Hi. Tell me a task or alarm to create."}

Input: Create an alarm for 10 min
Output: {"action":"chat","reply":"What time should the alarm ring?"}

Hard rules:
- Reply with ONLY the JSON. No text before or after.
- Do not write the words "a short natural question" or anything in angle brackets.
- In the "reply" field, write a real natural sentence of your own.
- Never invent user data. If something is missing, ask for it in "reply".
- Ask one short question at most.
""".trimIndent()
    }

    @Deprecated("Use buildSystemPrompt()")
    val SYSTEM: String = ""
}
