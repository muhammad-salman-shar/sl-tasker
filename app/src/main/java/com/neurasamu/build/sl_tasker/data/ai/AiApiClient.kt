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
                put("temperature", 0.2)
                put("max_tokens", 512)
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
                val text = BufferedReader(InputStreamReader(stream, "UTF-8")).use { it.readText() }

                if (code !in 200..299) {
                    throw RuntimeException("HTTP $code: ${text.take(200)}")
                }

                val root = JSONObject(text)
                val choices = root.optJSONArray("choices")
                    ?: throw RuntimeException("No choices in response")
                val first = choices.optJSONObject(0)
                    ?: throw RuntimeException("Empty choices")
                val msg = first.optJSONObject("message")
                    ?: throw RuntimeException("No message")
                msg.optString("content", "").ifBlank {
                    throw RuntimeException("Empty content")
                }
            } finally {
                conn.disconnect()
            }
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
    val SYSTEM: String = """
You are SL Tasker AI. Reply ONLY with a single JSON object. No prose, no markdown, no explanation.

Three actions are allowed:

1) Create a task:
{"action":"create_task","title":"...","time":"HH:MM","days":"","type":"MEDIUM","duration_minutes":0}
- time: 24-hour HH:MM
- days: empty for one-time, else comma list of weekdays (0=Sun,1=Mon,...,6=Sat)
- type: MEDIUM or HARD or CRITICAL
- duration_minutes: only for HARD/CRITICAL (minutes). Set 0 for MEDIUM.

2) Create an alarm:
{"action":"create_alarm","label":"...","time":"HH:MM","days":""}
- days: empty for one-time, else comma list of weekdays (0=Sun,...,6=Sat)

3) Normal chat (question, greeting, unclear request):
{"action":"chat","reply":"..."}

Rules:
- If a required field is missing, return {"action":"chat","reply":"<one short question>"}.
- Ask only ONE missing field per turn.
- Never invent values. Never complete or delete tasks.
- Only create tasks and alarms.
- Keep reply text short.
""".trimIndent()
}
