package com.neurasamu.build.solo_leveling_tasker.data.repository

import androidx.room.withTransaction
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.data.model.EventEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.EventType
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryStatus
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.solo_leveling_tasker.domain.engine.GamificationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StatsRepository(private val database: AppDatabase) {

    private val playerStatsDao = database.playerStatsDao()
    private val eventDao = database.eventDao()
    private val recoveryQuestDao = database.recoveryQuestDao()
    private val taskDebtDao = database.taskDebtDao()

    val playerStats: Flow<PlayerStatsEntity?> = playerStatsDao.observePlayerStats()

    suspend fun getPlayerStats(): PlayerStatsEntity? = withContext(Dispatchers.IO) {
        playerStatsDao.getPlayerStats()
    }

    suspend fun initializeStatsIfEmpty() = withContext(Dispatchers.IO) {
        if (playerStatsDao.getPlayerStats() == null) {
            playerStatsDao.insertOrUpdate(PlayerStatsEntity(id = 1L))
        }
    }

    fun getRecentEvents(limit: Int = 30): Flow<List<EventEntity>> =
        eventDao.getRecentEvents(limit)

    fun getActiveRecoveryQuests(): Flow<List<RecoveryQuestEntity>> =
        recoveryQuestDao.getActiveRecoveryQuests()

    fun getUnresolvedDebts(): Flow<List<TaskDebtEntity>> =
        taskDebtDao.getUnresolvedDebts()

    suspend fun completeRecoveryQuest(questId: Long): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val quest = recoveryQuestDao.getRecoveryQuestById(questId) ?: return@withTransaction false
            if (quest.status != RecoveryStatus.ACTIVE) return@withTransaction false

            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()
            val result = GamificationEngine.processRecoveryCompletion(currentStats)

            val updatedQuest = quest.copy(
                status = RecoveryStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
            recoveryQuestDao.updateRecoveryQuest(updatedQuest)
            playerStatsDao.insertOrUpdate(result.updatedStats)

            eventDao.insertEvent(
                EventEntity(
                    occurrenceId = quest.originOccurrenceId,
                    taskId = quest.taskId,
                    type = EventType.RECOVERY_COMPLETED,
                    epDelta = 0,
                    healthDelta = result.healthGained,
                    note = "Completed Recovery Quest #$questId"
                )
            )

            true
        }
    }
}
