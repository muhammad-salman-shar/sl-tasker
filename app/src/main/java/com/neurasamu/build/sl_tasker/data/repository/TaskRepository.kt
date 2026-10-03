package com.neurasamu.build.sl_tasker.data.repository

import androidx.room.withTransaction
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.EventEntity
import com.neurasamu.build.sl_tasker.data.model.EventType
import com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.sl_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.data.model.Priority
import com.neurasamu.build.sl_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.sl_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.domain.engine.PenaltyEngine
import com.neurasamu.build.sl_tasker.domain.engine.RecurrenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val database: AppDatabase) {

    private val taskDao = database.taskDao()
    private val occurrenceDao = database.occurrenceDao()
    private val eventDao = database.eventDao()
    private val taskDebtDao = database.taskDebtDao()
    private val recoveryQuestDao = database.recoveryQuestDao()
    private val playerStatsDao = database.playerStatsDao()

    fun getAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.getAllActiveTasks()

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun getTaskById(taskId: Long): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(taskId)
    }

    suspend fun createTaskWithOccurrence(
        task: TaskEntity,
        scheduledAt: Long,
        deadlineAt: Long
    ): Pair<Long, Long> = withContext(Dispatchers.IO) {
        database.withTransaction {
            val taskId = taskDao.insertTask(task)
            val occurrence = OccurrenceEntity(
                taskId = taskId,
                scheduledAt = scheduledAt,
                deadlineAt = deadlineAt,
                durationMinutes = task.durationMinutes
            )
            val occId = occurrenceDao.insertOccurrence(occurrence)
            Pair(taskId, occId)
        }
    }

    suspend fun completeOccurrence(occurrenceId: Long): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val occurrence = occurrenceDao.getOccurrenceById(occurrenceId) ?: return@withTransaction false
            if (occurrence.scheduledAt > System.currentTimeMillis()) return@withTransaction false
            val task = taskDao.getTaskById(occurrence.taskId) ?: return@withTransaction false
            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()

            val updatedOccurrence = occurrence.copy(
                status = OccurrenceStatus.COMPLETED,
                completedAt = System.currentTimeMillis()
            )
            occurrenceDao.updateOccurrence(updatedOccurrence)

            val gameResult = GamificationEngine.processQuestCompletion(
                currentStats = currentStats,
                difficulty = task.difficulty,
                streakBonusActive = true
            )
            playerStatsDao.insertOrUpdate(gameResult.updatedStats)

            eventDao.insertEvent(
                EventEntity(
                    occurrenceId = occurrenceId,
                    taskId = task.id,
                    type = EventType.OCCURRENCE_COMPLETED,
                    epDelta = gameResult.epGained,
                    healthDelta = gameResult.healthGained,
                    note = "Cleared Quest: ${task.title}"
                )
            )

            if (gameResult.leveledUp) {
                eventDao.insertEvent(
                    EventEntity(
                        type = EventType.LEVEL_UP,
                        note = "Level Up! Reached Level ${gameResult.updatedStats.level}"
                    )
                )
            }

            taskDebtDao.resolveDebt(occurrenceId)

            if (task.repeatRule == com.neurasamu.build.sl_tasker.data.model.RepeatRule.ONCE) {
                taskDao.archiveTask(task.id)
            } else {
                val nextScheduled = RecurrenceHelper.calculateNextOccurrence(
                    baseScheduledAt = occurrence.scheduledAt,
                    repeatRule = task.repeatRule,
                    customRepeatDays = task.customRepeatDays
                )
                if (nextScheduled != null) {
                    val durationMillis = task.durationMinutes * 60 * 1000L
                    occurrenceDao.insertOccurrence(
                        OccurrenceEntity(
                            taskId = task.id,
                            scheduledAt = nextScheduled,
                            deadlineAt = nextScheduled + durationMillis,
                            durationMinutes = task.durationMinutes
                        )
                    )
                }
            }

            if (task.priority == Priority.CRITICAL && currentStats.criticalActiveOccurrenceId == occurrenceId) {
                playerStatsDao.setCriticalActiveOccurrenceId(null)
                playerStatsDao.setFocusLock(false)
            }

            true
        }
    }

    suspend fun markOccurrenceMissed(occurrenceId: Long): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val occurrence = occurrenceDao.getOccurrenceById(occurrenceId) ?: return@withTransaction false
            val task = taskDao.getTaskById(occurrence.taskId) ?: return@withTransaction false
            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()

            val updatedOccurrence = occurrence.copy(
                status = OccurrenceStatus.MISSED,
                penaltyAppliedCount = occurrence.penaltyAppliedCount + 1
            )
            occurrenceDao.updateOccurrence(updatedOccurrence)

            val penaltyResult = PenaltyEngine.processMissedTask(
                currentStats = currentStats,
                priority = task.priority,
                difficulty = task.difficulty
            )
            playerStatsDao.insertOrUpdate(penaltyResult.updatedStats)

            eventDao.insertEvent(
                EventEntity(
                    occurrenceId = occurrenceId,
                    taskId = task.id,
                    type = EventType.OCCURRENCE_MISSED,
                    epDelta = 0,
                    healthDelta = -penaltyResult.healthLost,
                    note = "Missed Quest: ${task.title}"
                )
            )

            taskDebtDao.insertDebt(
                TaskDebtEntity(
                    taskId = task.id,
                    occurrenceId = occurrenceId,
                    originalDeadline = occurrence.deadlineAt
                )
            )

            if (penaltyResult.recoveryModeTriggered) {
                recoveryQuestDao.insertRecoveryQuest(
                    RecoveryQuestEntity(
                        taskId = task.id,
                        originOccurrenceId = occurrenceId,
                        requiredEp = 30
                    )
                )
            }

            if (currentStats.criticalActiveOccurrenceId == occurrenceId) {
                playerStatsDao.setCriticalActiveOccurrenceId(null)
                playerStatsDao.setFocusLock(false)
            }

            true
        }
    }

    suspend fun applyOverduePenalties(now: Long): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val overdue = taskDao.getPendingOverdueOccurrences(now)
            if (overdue.isEmpty()) return@withTransaction false
            var stats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()
            var changed = false
            for (occ in overdue) {
                val task = taskDao.getTaskById(occ.taskId) ?: continue
                val since = if (occ.lastPenaltyAt == 0L) occ.scheduledAt else occ.lastPenaltyAt
                val elapsedMin = ((now - since) / 60_000L).toInt()
                if (elapsedMin <= 0) continue
                val perMin = PenaltyEngine.penaltyPerMinute(task.difficulty)
                val damage = perMin * elapsedMin
                val newHealth = (stats.health - damage).coerceAtLeast(0)
                stats = stats.copy(
                    health = newHealth,
                    totalMissed = stats.totalMissed + 1,
                    recoveryModeActive = newHealth < 50 || stats.recoveryModeActive
                )
                occurrenceDao.updateOccurrence(
                    occ.copy(
                        lastPenaltyAt = since + elapsedMin * 60_000L,
                        penaltyAppliedCount = occ.penaltyAppliedCount + elapsedMin
                    )
                )
                changed = true
            }
            if (changed) {
                playerStatsDao.insertOrUpdate(stats)
            }
            changed
        }
    }

    suspend fun archiveTask(taskId: Long) = withContext(Dispatchers.IO) {
        taskDao.archiveTask(taskId)
    }

    suspend fun deleteTask(taskId: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            taskDao.deleteOccurrencesForTask(taskId)
            taskDao.deleteTask(taskId)
        }
    }

    suspend fun updateTaskFields(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    fun observeOccurrencesByStatus(statuses: List<OccurrenceStatus>): Flow<List<OccurrenceEntity>> =
        occurrenceDao.getOccurrencesByStatus(statuses)

    fun observeAllOccurrences(): Flow<List<OccurrenceEntity>> =
        occurrenceDao.getAllOccurrences()

    suspend fun getOverdueOccurrences(currentTimeMillis: Long): List<OccurrenceEntity> =
        withContext(Dispatchers.IO) {
            occurrenceDao.getOverdueOccurrences(currentTimeMillis)
        }
}
