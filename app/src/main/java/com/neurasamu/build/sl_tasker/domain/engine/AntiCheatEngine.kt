package com.neurasamu.build.sl_tasker.domain.engine

import android.content.Context
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.data.model.TaskEntity

data class AntiCheatEvaluation(
    val isCheesed: Boolean,
    val cappedHpReward: Int?,
    val cappedEpReward: Int?,
    val updatedStatsWithShame: PlayerStatsEntity,
    val warningMessage: String?
)

/**
 * Closes the "1-minute Critical task in background" exploit and detects rapid task farming.
 * - Full HARD reward (40 HP, 30 EP) requires >= 20 min realistic duration.
 * - Full CRITICAL reward (50 HP, 50 EP) requires >= 30 min realistic duration.
 * - Short 1-19 min Hard/Critical tasks receive capped minimal rewards (+5 HP, +3 EP) and log a strike.
 * - Rapid burst farming (>= 4 completions within 10 minutes or >= 2 short-duration cheese attempts)
 *   applies a visible 24-hour temporary shaming badge ("Shortcut Hunter / Cheese Detected").
 */
object AntiCheatEngine {

    const val MIN_HARD_MINUTES = 20
    const val MIN_CRITICAL_MINUTES = 30
    const val SHAMING_DURATION_MS = 24L * 60L * 60L * 1000L
    const val SHAME_BADGE_TITLE = "Shortcut Hunter / Cheese Detected"

    private const val PREFS_NAME = "sl_anticheat_prefs"
    private const val KEY_RECENT_TIMESTAMPS = "recent_completion_ts"

    fun minimumMinutesFor(difficulty: Difficulty): Int = when (difficulty) {
        Difficulty.MEDIUM -> 0
        Difficulty.HARD -> MIN_HARD_MINUTES
        Difficulty.CRITICAL -> MIN_CRITICAL_MINUTES
    }

    fun evaluateCompletion(
        context: Context,
        task: TaskEntity,
        currentStats: PlayerStatsEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): AntiCheatEvaluation {
        val minRequired = minimumMinutesFor(task.difficulty)
        val shortDurationCheese = minRequired > 0 && task.durationMinutes < minRequired

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val windowStart = nowMillis - 10L * 60L * 1000L
        val existingTs = (prefs.getString(KEY_RECENT_TIMESTAMPS, "") ?: "")
            .split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .filter { it >= windowStart }
            .toMutableList()
        existingTs.add(nowMillis)
        prefs.edit().putString(KEY_RECENT_TIMESTAMPS, existingTs.joinToString(",")).apply()

        val rapidFarming = existingTs.size >= 4
        val newStrikes = if (shortDurationCheese || rapidFarming) {
            currentStats.cheatStrikeCount + 1
        } else {
            currentStats.cheatStrikeCount
        }

        val triggerShame = rapidFarming || (shortDurationCheese && newStrikes >= 2)
        val updatedStats = if (triggerShame) {
            currentStats.copy(
                cheatStrikeCount = newStrikes,
                shamingUntilMillis = nowMillis + SHAMING_DURATION_MS,
                shamingReason = SHAME_BADGE_TITLE
            )
        } else {
            currentStats.copy(cheatStrikeCount = newStrikes)
        }

        val warning = when {
            triggerShame -> "Anti-Cheat: $SHAME_BADGE_TITLE badge applied (24h). Rewards capped."
            shortDurationCheese -> "Anti-Cheat: ${task.difficulty.name} full reward needs >= ${minRequired}m. Reward capped (+5 HP)."
            else -> null
        }

        return AntiCheatEvaluation(
            isCheesed = shortDurationCheese || rapidFarming,
            cappedHpReward = if (shortDurationCheese || rapidFarming) 5 else null,
            cappedEpReward = if (shortDurationCheese || rapidFarming) 3 else null,
            updatedStatsWithShame = updatedStats,
            warningMessage = warning
        )
    }
}
