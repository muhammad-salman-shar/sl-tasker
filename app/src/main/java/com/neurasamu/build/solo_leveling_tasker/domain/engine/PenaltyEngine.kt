package com.neurasamu.build.solo_leveling_tasker.domain.engine

import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.Priority
import kotlin.math.max

data class PenaltyResult(
    val updatedStats: PlayerStatsEntity,
    val healthLost: Int,
    val streakReset: Boolean,
    val recoveryModeTriggered: Boolean
)

object PenaltyEngine {

    const val MIN_HEALTH = 0
    const val RECOVERY_THRESHOLD = 50

    fun processMissedTask(
        currentStats: PlayerStatsEntity,
        priority: Priority,
        difficulty: Difficulty
    ): PenaltyResult {
        val baseDamage = when (difficulty) {
            Difficulty.EASY -> 5
            Difficulty.NORMAL -> 10
            Difficulty.HARD -> 15
            Difficulty.EXTREME -> 25
        }

        val multiplier = when (priority) {
            Priority.LOW -> 1.0f
            Priority.MEDIUM -> 1.2f
            Priority.HIGH -> 1.5f
            Priority.CRITICAL -> 2.0f
        }

        val totalDamage = (baseDamage * multiplier).toInt()
        val newHealth = max(MIN_HEALTH, currentStats.health - totalDamage)
        val shouldTriggerRecovery = newHealth < RECOVERY_THRESHOLD
        val streakReset = currentStats.streak > 0

        val updatedStats = currentStats.copy(
            health = newHealth,
            streak = 0,
            totalMissed = currentStats.totalMissed + 1,
            recoveryModeActive = shouldTriggerRecovery || currentStats.recoveryModeActive
        )

        return PenaltyResult(
            updatedStats = updatedStats,
            healthLost = totalDamage,
            streakReset = streakReset,
            recoveryModeTriggered = shouldTriggerRecovery
        )
    }
}
