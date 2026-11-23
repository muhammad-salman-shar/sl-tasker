package com.neurasamu.build.sl_tasker.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.SSLException

data class ChatMsg(val role: String, val content: String)

data class AiTestResult(
    val success: Boolean,
    val statusCode: Int? = null,
    val endpoint: String = "",
    val message: String,
    val latencyMs: Long = 0L
)

class AiApiClient {

    suspend fun chat(config: AiConfig, messages: List<ChatMsg>): String =
        withContext(Dispatchers.IO) {
            when (config.provider) {
                AiProvider.GEMINI -> chatGemini(config, messages)
                AiProvider.OPENAI_COMPATIBLE,
                AiProvider.LOCAL_CUSTOM -> chatOpenAiCompat(config, messages)
            }
        }

    suspend fun testConnection(config: AiConfig): AiTestResult =
        withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            val endpoint = try {
                resolveEndpoint(config)
            } catch (e: Exception) {
                return@withContext AiTestResult(
                    success = false,
                    message = e.message ?: "Invalid URL configuration"
                )
            }

            try {
                val pingMessages = listOf(
                    ChatMsg("system", "Reply with OK."),
                    ChatMsg("user", "Ping")
                )
                val testCfg = config.copy(
                    useStream = false,
                    useJsonFormat = false,
                    timeoutSec = config.timeoutSec.coerceIn(10, 90)
                )
                val reply = when (testCfg.provider) {
                    AiProvider.GEMINI -> chatGemini(testCfg, pingMessages, maxTokens = 16)
                    else -> chatOpenAiCompat(testCfg, pingMessages, maxTokens = 16)
                }
                val elapsed = System.currentTimeMillis() - start
                AiTestResult(
                    success = true,
                    statusCode = 200,
                    endpoint = endpoint,
                    message = "Connected (HTTP 200) in ${elapsed}ms — Reply: \"${reply.trim().take(40)}\"",
                    latencyMs = elapsed
                )
            } catch (e: HttpStatusException) {
                val elapsed = System.currentTimeMillis() - start
                AiTestResult(
                    success = false,
                    statusCode = e.code,
                    endpoint = endpoint,
                    message = formatHttpError(e.code, e.body),
                    latencyMs = elapsed
                )
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - start
                AiTestResult(
                    success = false,
                    endpoint = endpoint,
                    message = formatNetworkError(e, config.timeoutSec),
                    latencyMs = elapsed
                )
            }
        }

    fun resolveEndpoint(config: AiConfig): String {
        return when (config.provider) {
            AiProvider.GEMINI -> buildGeminiEndpoint(config.baseUrl, config.model)
            AiProvider.OPENAI_COMPATIBLE,
            AiProvider.LOCAL_CUSTOM -> buildOpenAiEndpoint(config.baseUrl, config.provider)
        }
    }

    private fun chatOpenAiCompat(
        config: AiConfig,
        messages: List<ChatMsg>,
        maxTokens: Int = 512
    ): String {
        val endpoint = buildOpenAiEndpoint(config.baseUrl, config.provider)
        val allowJsonFormat = config.useJsonFormat && !config.chatMode
        val effectiveTimeoutMs = config.timeoutSec.coerceIn(10, 180) * 1000

        return try {
            executeOpenAiRequest(endpoint, config, messages, maxTokens, allowJsonFormat, effectiveTimeoutMs)
        } catch (e: HttpStatusException) {
            // Some local servers (Ollama/llama.cpp/LM Studio) reject response_format: json_object with HTTP 400
            if (allowJsonFormat && e.code == 400 && e.body.contains("response_format", ignoreCase = true)) {
                executeOpenAiRequest(endpoint, config, messages, maxTokens, false, effectiveTimeoutMs)
            } else {
                throw e
            }
        }
    }

    private fun executeOpenAiRequest(
        endpoint: String,
        config: AiConfig,
        messages: List<ChatMsg>,
        maxTokens: Int,
        includeJsonFormat: Boolean,
        timeoutMs: Int
    ): String {
        val body = JSONObject().apply {
            put("model", config.model.trim())
            put("temperature", if (config.chatMode) 0.6 else 0.1)
            put("max_tokens", maxTokens)
            if (config.useStream) put("stream", true)
            if (includeJsonFormat) {
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
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", if (config.useStream) "text/event-stream, application/json" else "application/json")
            if (config.apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            }
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                w.write(body.toString())
                w.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream

            if (code !in 200..299) {
                val errText = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
                throw HttpStatusException(code, errText)
            }

            val contentType = (conn.contentType ?: "").lowercase()
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))

            val result = if (config.useStream && contentType.contains("text/event-stream")) {
                readSse(reader)
            } else {
                val whole = reader.readText()
                val trimmed = whole.trimStart()
                if (trimmed.startsWith("data:")) {
                    parseSseBlob(whole)
                } else {
                    parseOpenAiJsonWhole(whole)
                }
            }

            if (result.isBlank()) throw RuntimeException("Empty response from model")
            return result
        } finally {
            conn.disconnect()
        }
    }

    private fun chatGemini(
        config: AiConfig,
        messages: List<ChatMsg>,
        maxTokens: Int = 512
    ): String {
        if (config.apiKey.isBlank()) {
            throw RuntimeException("Gemini API Key is required")
        }
        val endpoint = buildGeminiEndpoint(config.baseUrl, config.model)
        val timeoutMs = config.timeoutSec.coerceIn(10, 180) * 1000

        val systemText = messages.filter { it.role == "system" }.joinToString("\n") { it.content }.trim()
        val contents = JSONArray()
        messages.filter { it.role != "system" }.forEach { m ->
            val geminiRole = if (m.role == "assistant") "model" else "user"
            contents.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", m.content) })
                })
            })
        }

        val body = JSONObject().apply {
            if (systemText.isNotBlank()) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemText) })
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", if (config.chatMode) 0.6 else 0.1)
                put("maxOutputTokens", maxTokens)
                if (config.useJsonFormat && !config.chatMode) {
                    put("responseMimeType", "application/json")
                }
            })
        }

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("x-goog-api-key", config.apiKey.trim())
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { w ->
                w.write(body.toString())
                w.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream

            if (code !in 200..299) {
                val errText = stream?.let { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() } } ?: ""
                throw HttpStatusException(code, errText)
            }

            val whole = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            val root = JSONObject(whole)
            root.optJSONObject("error")?.let { err ->
                throw RuntimeException("Gemini error: ${err.optString("message", "unknown")}")
            }
            val candidates = root.optJSONArray("candidates")
                ?: throw RuntimeException("No candidates in Gemini response")
            val first = candidates.optJSONObject(0)
                ?: throw RuntimeException("Empty candidates in Gemini response")
            val parts = first.optJSONObject("content")?.optJSONArray("parts")
                ?: throw RuntimeException("No content parts in Gemini response")

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val txt = parts.optJSONObject(i)?.optString("text", "") ?: ""
                if (txt.isNotEmpty()) sb.append(txt)
            }
            val result = sb.toString()
            if (result.isBlank()) throw RuntimeException("Empty response from Gemini")
            return result
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

    private fun parseOpenAiJsonWhole(text: String): String {
        val root = JSONObject(text)
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
     * Smart URL Normalizer for OpenAI-compatible & Local/LAN servers.
     * - Replaces 0.0.0.0 with 127.0.0.1
     * - Prepends http:// for local/LAN hosts and https:// for public domains if scheme is missing
     * - Enforces HTTPS for all public (non-private/LAN) domains
     * - Handles trailing slashes, /v1, /v1/chat/completions cleanly
     */
    private fun buildOpenAiEndpoint(raw: String, provider: AiProvider): String {
        var s = raw.trim()
        if (s.isBlank()) throw RuntimeException("Base URL is empty")

        val hasScheme = s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)
        if (!hasScheme) {
            val hostGuess = s.substringBefore("/").substringBefore(":").lowercase()
            val scheme = if (isPrivateOrLocalHost(hostGuess) || provider == AiProvider.LOCAL_CUSTOM) "http://" else "https://"
            s = scheme + s
        }

        s = s.replace("://0.0.0.0", "://127.0.0.1")

        val uri = try {
            URI(s)
        } catch (_: Exception) {
            throw RuntimeException("Invalid URL format: $s")
        }
        val host = uri.host?.lowercase() ?: throw RuntimeException("Missing host in URL: $s")

        // Strict HTTPS guard for public internet hosts
        if (uri.scheme.equals("http", ignoreCase = true) && !isPrivateOrLocalHost(host)) {
            s = "https://" + s.substringAfter("://")
        }

        while (s.endsWith("/")) s = s.dropLast(1)

        if (s.endsWith("/chat/completions", ignoreCase = true)) return s
        if (s.endsWith("/v1", ignoreCase = true)) return "$s/chat/completions"

        return "$s/v1/chat/completions"
    }

    private fun buildGeminiEndpoint(rawBase: String, rawModel: String): String {
        val model = rawModel.trim().removePrefix("models/")
        if (model.isBlank()) throw RuntimeException("Gemini model name is empty (e.g. gemini-2.5-flash)")

        var base = rawBase.trim().ifBlank { "https://generativelanguage.googleapis.com" }
        if (!base.startsWith("http://", ignoreCase = true) && !base.startsWith("https://", ignoreCase = true)) {
            base = "https://$base"
        }
        val uri = try { URI(base) } catch (_: Exception) { throw RuntimeException("Invalid Gemini URL: $base") }
        val host = uri.host?.lowercase() ?: throw RuntimeException("Invalid Gemini host")
        if (uri.scheme.equals("http", ignoreCase = true) && !isPrivateOrLocalHost(host)) {
            base = "https://" + base.substringAfter("://")
        }

        while (base.endsWith("/")) base = base.dropLast(1)

        if (base.contains(":generateContent", ignoreCase = true)) return base
        if (base.endsWith("/v1beta", ignoreCase = true) || base.endsWith("/v1", ignoreCase = true)) {
            return "$base/models/$model:generateContent"
        }
        return "$base/v1beta/models/$model:generateContent"
    }

    fun isPrivateOrLocalHost(host: String): Boolean {
        val h = host.trim().lowercase().removePrefix("[").removeSuffix("]")
        if (h == "localhost" || h == "127.0.0.1" || h == "0.0.0.0" || h == "::1" || h.endsWith(".local")) {
            return true
        }
        if (h.startsWith("127.") || h.startsWith("10.") || h.startsWith("192.168.")) {
            return true
        }
        if (h.startsWith("172.")) {
            val second = h.split(".").getOrNull(1)?.toIntOrNull() ?: return false
            if (second in 16..31) return true
        }
        return false
    }

    fun formatHttpError(code: Int, body: String): String {
        val detail = extractErrorMessage(body)
        val hint = when (code) {
            400 -> "Bad Request — check model name or disable Force JSON mode"
            401 -> "Unauthorized — check your API key"
            403 -> "Forbidden — API key lacks permission for this model"
            404 -> "Not Found — check Base URL or model name"
            429 -> "Rate Limit / Quota Exceeded — try again later"
            in 500..599 -> "Server Error ($code) — AI server crashed or overloaded"
            else -> "HTTP $code"
        }
        return if (detail.isNotBlank()) "HTTP $code: $hint ($detail)" else "HTTP $code: $hint"
    }

    fun formatNetworkError(e: Throwable, timeoutSec: Int): String {
        return when (e) {
            is HttpStatusException -> formatHttpError(e.code, e.body)
            is SocketTimeoutException -> "Timed out after ${timeoutSec}s — local model may still be loading. Try increasing timeout."
            is ConnectException -> "Connection refused — make sure your local server (SaMu Lab / Ollama / LM Studio) is running and reachable on this IP & port."
            is UnknownHostException -> "Host not found — check your internet connection or server address."
            is SSLException -> "SSL/HTTPS error — check if your local server uses http:// instead of https://."
            else -> e.message ?: "Unknown network error"
        }
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return ""
        return try {
            val root = JSONObject(body)
            val errObj = root.optJSONObject("error")
            val msg = errObj?.optString("message", "")?.ifBlank { root.optString("message", "") }
                ?: root.optString("message", "")
            msg.trim().take(140)
        } catch (_: Exception) {
            body.trim().take(140)
        }
    }
}

class HttpStatusException(val code: Int, val body: String) : RuntimeException("HTTP $code: ${body.take(200)}")

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
You are SL Tasker Agent. Convert the user's English into ONE JSON object. Output only JSON. No prose, no markdown.

Now: ${nowString()}. Days: 0=Sun,1=Mon,2=Tue,3=Wed,4=Thu,5=Fri,6=Sat.

ACTIONS:

1) CREATE TASK
{"action":"create_task","title":"Study math","time":"19:00","days":"","type":"HARD","duration_minutes":60}
- time: 24-hour HH:MM
- days: "" for one-time, OR comma list "1,3,5" for weekly repeat
- type: MEDIUM (no timer), HARD/CRITICAL (need duration)
- duration_minutes: 0 for MEDIUM, >0 for HARD/CRITICAL

2) CREATE ALARM (3 dismiss methods)
EASY:
{"action":"create_alarm","label":"Wake up","time":"06:30","days":"","dismiss_method":"EASY","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}

PIN (requires 8-digit code, not starting 12345, not all-same):
{"action":"create_alarm","label":"Gym","time":"05:00","days":"","dismiss_method":"PIN","pin_code":"58294017","snooze_enabled":true,"snooze_minutes":15,"vibrate":true}
- If user says "with PIN" but no number given, ASK: "What 8-digit PIN? (not starting with 12345, not all same)"

MATH (solve a sum to dismiss):
{"action":"create_alarm","label":"Wake up","time":"06:00","days":"","dismiss_method":"MATH","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}

3) DELETE
{"action":"delete_task","title":"Study math"}
{"action":"delete_alarm","label":"Gym"}

4) COMPLETE
{"action":"complete_task","title":"Study math"}

5) LIST
{"action":"list_tasks"}
{"action":"list_alarms"}
{"action":"list_pending"}
{"action":"list_missed"}

6) STATS
{"action":"show_stats"}
- Returns level, HP, EP, streak, completed, missed counts

7) BLOCK MODE
{"action":"set_block_mode","mode":"OFF|TEST|STRICT"}

8) BLOCK BY CATEGORY (auto-picks installed apps from a curated list)
{"action":"block_category","category":"social"}
{"action":"block_category","category":"social,games"}
- categories: social, ai, games, entertainment

9) CLARIFY (when request is vague — show options)
{"action":"show_options","options":["Create a task","Create an alarm","Show my stats","Block social apps"]}
- 2-4 short options

10) CHAT (info missing or casual)
{"action":"chat","reply":"What time should I set?"}

RULES:
- Reply with JSON only. No prose. No markdown.
- For PIN method: if user says "with PIN" without giving one, ASK for an 8-digit PIN.
- For "block socials" → block_category "social".
- For "pending tasks" → list_pending.
- For "what did I miss" → list_missed.
- For "my stats" → show_stats.
- For hourly reminders → NOT supported, use chat and offer alternatives.
- If a REQUIRED field is missing, use chat to ask ONE short question.
- Never invent data.

EXAMPLES:
IN: Wake me at 6:30 on weekdays with a PIN
OUT: {"action":"chat","reply":"What 8-digit PIN? (not starting with 12345, not all same)"}

IN: Alarm at 5am labeled gym with PIN 58294017
OUT: {"action":"create_alarm","label":"gym","time":"05:00","days":"","dismiss_method":"PIN","pin_code":"58294017","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}

IN: Alarm at 6am with math challenge
OUT: {"action":"create_alarm","label":"Wake up","time":"06:00","days":"","dismiss_method":"MATH","pin_code":"","snooze_enabled":true,"snooze_minutes":10,"vibrate":true}

IN: Block socials for me
OUT: {"action":"block_category","category":"social"}

IN: Block social media and games
OUT: {"action":"block_category","category":"social,games"}

IN: Show my stats
OUT: {"action":"show_stats"}

IN: What tasks are pending?
OUT: {"action":"list_pending"}

IN: Which tasks did I miss?
OUT: {"action":"list_missed"}

IN: I want to be productive
OUT: {"action":"show_options","options":["Create a task","Create an alarm","Show my stats","Block distractions"]}
""".trimIndent()
}
