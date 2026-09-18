package com.neurasamu.build.solo_leveling_tasker.domain.engine

import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import kotlin.math.min

data class GamificationResult(
    val updatedStats: PlayerStatsEntity,
    val epGained: Int,
    val healthGained: Int,
    val leveledUp: Boolean,
    val levelsGained: Int
)

object GamificationEngine {

    const val EP_PER_LEVEL = 90
    const val MAX_HEALTH = 100

    fun processQuestCompletion(
        currentStats: PlayerStatsEntity,
        difficulty: Difficulty,
        streakBonusActive: Boolean = false
    ): GamificationResult {
        val baseEp = difficulty.baseEp
        val streakBonus = if (streakBonusActive && currentStats.streak >= 3) 2 else 0
        val totalEpGained = baseEp + streakBonus

        val rawEp = currentStats.ep + totalEpGained
        val levelsGained = rawEp / EP_PER_LEVEL
        val newEp = rawEp % EP_PER_LEVEL
        val newLevel = currentStats.level + levelsGained
        val leveledUp = levelsGained > 0

        val healthRestored = when (difficulty) {
            Difficulty.EASY -> 2
            Difficulty.NORMAL -> 5
            Difficulty.HARD -> 10
            Difficulty.EXTREME -> 15
        }
        val newHealth = min(MAX_HEALTH, currentStats.health + healthRestored)

        val updatedStats = currentStats.copy(
            health = newHealth,
            ep = newEp,
            level = newLevel,
            totalCompleted = currentStats.totalCompleted + 1,
            highestEpMilestone = if (newLevel > currentStats.level) newLevel * EP_PER_LEVEL else currentStats.highestEpMilestone,
            recoveryModeActive = if (newHealth >= 50) false else currentStats.recoveryModeActive
        )

        return GamificationResult(
            updatedStats = updatedStats,
            epGained = totalEpGained,
            healthGained = healthRestored,
            leveledUp = leveledUp,
            levelsGained = levelsGained
        )
    }

    fun processRecoveryCompletion(
        currentStats: PlayerStatsEntity,
        bonusHealth: Int = 20
    ): GamificationResult {
        val newHealth = min(MAX_HEALTH, currentStats.health + bonusHealth)
        val updatedStats = currentStats.copy(
            health = newHealth,
            totalRecoveries = currentStats.totalRecoveries + 1,
            recoveryModeActive = if (newHealth >= 50) false else currentStats.recoveryModeActive
        )

        return GamificationResult(
            updatedStats = updatedStats,
            epGained = 0,
            healthGained = bonusHealth,
            leveledUp = false,
            levelsGained = 0
        )
    }
}
