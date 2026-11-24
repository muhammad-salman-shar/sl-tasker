package com.neurasamu.build.sl_tasker.data.timer

import org.json.JSONObject

data class ActiveTimer(
    val occurrenceId: Long,
    val taskTitle: String,
    val totalSeconds: Int,
    val startedAtMillis: Long,
    val elapsedBeforePauseSeconds: Int,
    val isPaused: Boolean,
    val accentColorArgb: Int,
    val durationMinutes: Int,
    val elapsedRealtimeAtStart: Long = android.os.SystemClock.elapsedRealtime(),
    val stepGoal: Int = 0,
    val initialStepsAtStart: Int = 0
) {
    val isStepMode: Boolean
        get() = stepGoal > 0

    fun sessionSteps(currentTodaySteps: Int): Int =
        (currentTodaySteps - initialStepsAtStart).coerceAtLeast(0)

    fun remainingSeconds(): Int {
        return if (isPaused) {
            (totalSeconds - elapsedBeforePauseSeconds).coerceAtLeast(0)
        } else {
            val elapsedRealNow = android.os.SystemClock.elapsedRealtime()
            val elapsedRealSinceStart = (elapsedRealNow - elapsedRealtimeAtStart) / 1000
            val elapsed = elapsedBeforePauseSeconds + elapsedRealSinceStart.toInt()
            (totalSeconds - elapsed).coerceAtLeast(0)
        }
    }

    fun elapsedNow(): Int = (totalSeconds - remainingSeconds()).coerceAtLeast(0)

    fun toJson(): String = JSONObject().apply {
        put("occurrenceId", occurrenceId)
        put("taskTitle", taskTitle)
        put("totalSeconds", totalSeconds)
        put("startedAtMillis", startedAtMillis)
        put("elapsedBeforePauseSeconds", elapsedBeforePauseSeconds)
        put("isPaused", isPaused)
        put("accentColorArgb", accentColorArgb)
        put("durationMinutes", durationMinutes)
        put("elapsedRealtimeAtStart", elapsedRealtimeAtStart)
        put("stepGoal", stepGoal)
        put("initialStepsAtStart", initialStepsAtStart)
    }.toString()

    companion object {
        fun fromJson(s: String): ActiveTimer? = try {
            val o = JSONObject(s)
            ActiveTimer(
                occurrenceId = o.getLong("occurrenceId"),
                taskTitle = o.optString("taskTitle", "Task"),
                totalSeconds = o.getInt("totalSeconds"),
                startedAtMillis = o.getLong("startedAtMillis"),
                elapsedBeforePauseSeconds = o.optInt("elapsedBeforePauseSeconds", 0),
                isPaused = o.optBoolean("isPaused", false),
                accentColorArgb = o.optInt("accentColorArgb", 0xFF00E5FF.toInt()),
                durationMinutes = o.optInt("durationMinutes", 1),
                elapsedRealtimeAtStart = o.optLong("elapsedRealtimeAtStart", android.os.SystemClock.elapsedRealtime()),
                stepGoal = o.optInt("stepGoal", 0),
                initialStepsAtStart = o.optInt("initialStepsAtStart", 0)
            )
        } catch (_: Throwable) { null }
    }
}
