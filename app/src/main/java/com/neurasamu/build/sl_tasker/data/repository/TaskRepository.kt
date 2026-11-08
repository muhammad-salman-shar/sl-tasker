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
import com.neurasamu.build.sl_tasker.data.model.ScheduleMode
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
            if (occurrence.status == OccurrenceStatus.COMPLETED) return@withTransaction false
            val task = taskDao.getTaskById(occurrence.taskId) ?: return@withTransaction false
            // Defense-in-depth time gate — UI prevents this, but enforce here too
            if (!task.allowEarlyComplete && System.currentTimeMillis() < occurrence.scheduledAt) {
                return@withTransaction false
            }
            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()

            val nowMs = System.currentTimeMillis()
            val updatedOccurrence = occurrence.copy(
                status = OccurrenceStatus.COMPLETED,
                completedAt = nowMs,
                completedCleanupAt = nowMs + 24L * 60L * 60L * 1000L
            )
            occurrenceDao.updateOccurrence(updatedOccurrence)

            // ---- Streak update BEFORE gamification ----
            val streakResult = com.neurasamu.build.sl_tasker.domain.engine.StreakEngine.compute(currentStats)
            val streakUpdated = currentStats.copy(
                streak = streakResult.newStreak,
                lastStreakDayEpoch = streakResult.newLastDay
            )

            val gameResult = GamificationEngine.processQuestCompletion(
                currentStats = streakUpdated,
                difficulty = task.difficulty,
                streakBonusActive = true
            )

            // Milestone bonus EP (goes into epModulo within level)
            var finalStats = gameResult.updatedStats
            val milestoneBonus = when (streakResult.milestoneReached) {
                3 -> 5
                7 -> 15
                14 -> 30
                30 -> 60
                60 -> 100
                100 -> 150
                365 -> 500
                else -> 0
            }
            if (milestoneBonus > 0) {
                val rawEp = finalStats.ep + milestoneBonus
                val extraLevels = rawEp / GamificationEngine.EP_PER_LEVEL
                finalStats = finalStats.copy(
                    ep = rawEp % GamificationEngine.EP_PER_LEVEL,
                    level = finalStats.level + extraLevels
                )
            }
            // ---- Recovery quest update ----
            try {
                val activeRecovery = recoveryQuestDao.getActiveRecoveryQuestsList()
                if (activeRecovery.isNotEmpty()) {
                    val rq = activeRecovery.first()
                    val newEarned = rq.earnedEp + gameResult.epGained
                    if (newEarned >= rq.requiredEp) {
                        recoveryQuestDao.updateRecoveryQuest(
                            rq.copy(
                                status = com.neurasamu.build.sl_tasker.data.model.RecoveryStatus.COMPLETED,
                                earnedEp = newEarned,
                                completedAt = System.currentTimeMillis()
                            )
                        )
                        val hpNow = finalStats.health
                        val restored = (hpNow + com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE)
                            .coerceAtMost(com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.MAX_HP)
                        finalStats = finalStats.copy(
                            health = restored,
                            totalRecoveries = finalStats.totalRecoveries + 1,
                            recoveryModeActive = restored < 50
                        )
                        playerStatsDao.insertOrUpdate(finalStats)
                        com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                            "💚 RECOVERED! +${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE} HP"
                        )
                    } else {
                        recoveryQuestDao.updateRecoveryQuest(rq.copy(earnedEp = newEarned))
                        val remaining = rq.requiredEp - newEarned
                        com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                            "⚡ Recovery: $remaining EP to go"
                        )
                    }
                }
            } catch (_: Throwable) {}

            playerStatsDao.insertOrUpdate(finalStats)

            // Notify via SystemEvents (survives across repository/UI)
            try {
                if (streakResult.milestoneReached != null) {
                    com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                        "🔥 ${streakResult.milestoneReached}-day streak! +$milestoneBonus EP"
                    )
                } else if (streakResult.broken) {
                    com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                        "💔 Streak reset. Start again."
                    )
                }
            } catch (_: Throwable) {}

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
                // One-time task: archive it so it disappears from active lists
                taskDao.archiveTask(task.id)
            } else if (task.scheduleMode == ScheduleMode.NO_TIME) {
                // NO_TIME repeat: do NOT create next occurrence here.
                // Cleanup worker will create a fresh instance after 24h of completed retention.
            } else {
                // Repeat task: only create a next occurrence if none exists AND the
                // calculated next time is strictly in the future (prevents duplicates
                // and phantom same-time occurrences).
                val now = System.currentTimeMillis()
                val existingNext = occurrenceDao.getNextPendingOccurrence(task.id, now)
                if (existingNext == null) {
                    val nextScheduled = RecurrenceHelper.calculateNextOccurrence(
                        baseScheduledAt = occurrence.scheduledAt,
                        repeatRule = task.repeatRule,
                        customRepeatDays = task.customRepeatDays
                    )
                    // Ensure the next occurrence is strictly after the one we just completed
                    // AND after now — prevents infinite same-time loops on repeat tasks.
                    val minNext = maxOf(now, occurrence.scheduledAt + 60_000L)
                    if (nextScheduled != null && nextScheduled >= minNext) {
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
                // Only create if no active recovery quest exists
                val existing = recoveryQuestDao.getActiveRecoveryQuestsList()
                if (existing.isEmpty()) {
                    recoveryQuestDao.insertRecoveryQuest(
                        RecoveryQuestEntity(
                            taskId = task.id,
                            originOccurrenceId = occurrenceId,
                            requiredEp = com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.TARGET_EP
                        )
                    )
                    com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                        "⚠️ PENALTY ZONE: Earn ${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.TARGET_EP} EP to restore ${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE} HP"
                    )
                }
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

                // NO_TIME + lifetime: never missed, never penalized
                if (task.scheduleMode == ScheduleMode.NO_TIME && task.validityDays == 0) continue

                // Determine if truly expired
                val expired: Boolean = when (task.scheduleMode) {
                    ScheduleMode.NO_TIME -> {
                        val expiresAt = occ.createdAt + task.validityDays.toLong() * 24L * 60L * 60L * 1000L
                        now >= expiresAt
                    }
                    ScheduleMode.TIME -> now >= occ.deadlineAt + 30L * 60L * 1000L
                }

                if (expired) {
                    // NO_TIME tasks: apply one-time penalty (no per-minute has run)
                    if (task.scheduleMode == ScheduleMode.NO_TIME) {
                        try {
                            val penaltyResult = PenaltyEngine.processMissedTask(
                                currentStats = stats,
                                priority = task.priority,
                                difficulty = task.difficulty
                            )
                            stats = penaltyResult.updatedStats
                        } catch (_: Throwable) {}
                    }
                    // Mark occurrence MISSED (TIME tasks: per-minute already drained; no extra)
                    occurrenceDao.updateOccurrence(
                        occ.copy(
                            status = OccurrenceStatus.MISSED,
                            completedCleanupAt = now + 24L * 60L * 60L * 1000L
                        )
                    )
                    try {
                        eventDao.insertEvent(
                            EventEntity(
                                occurrenceId = occ.id,
                                taskId = task.id,
                                type = EventType.OCCURRENCE_MISSED,
                                epDelta = 0,
                                healthDelta = 0,
                                note = "Missed: " + task.title
                            )
                        )
                    } catch (_: Throwable) {}
                    changed = true
                } else if (task.scheduleMode == ScheduleMode.TIME) {
                    // Ongoing per-minute penalty for TIME tasks not yet expired
                    val since = if (occ.lastPenaltyAt == 0L) occ.scheduledAt else occ.lastPenaltyAt
                    val elapsedMin = ((now - since) / 60_000L).toInt()
                    if (elapsedMin <= 0) continue
                    val perMin = PenaltyEngine.penaltyPerMinute(task.difficulty)
                    val damage = perMin * elapsedMin
                    val newHealth = (stats.health - damage).coerceAtLeast(0)
                    stats = stats.copy(
                        health = newHealth,
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
            }

            if (changed) {
                playerStatsDao.insertOrUpdate(stats)
            }
            changed
        }
    }

    suspend fun getPendingOccurrencesForTask(taskId: Long): List<OccurrenceEntity> =
        withContext(Dispatchers.IO) { occurrenceDao.getPendingByTask(taskId) }

    suspend fun deleteOccurrence(id: Long) = withContext(Dispatchers.IO) {
        occurrenceDao.deleteOccurrenceById(id)
    }

    suspend fun dropHealth(amount: Int): Int = withContext(Dispatchers.IO) {
        database.withTransaction {
            val stats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()
            val newHp = (stats.health - amount).coerceAtLeast(0)
            playerStatsDao.insertOrUpdate(
                stats.copy(
                    health = newHp,
                    recoveryModeActive = newHp < 50 || stats.recoveryModeActive
                )
            )
            newHp
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

    suspend fun performCleanup(now: Long): Int = withContext(Dispatchers.IO) {
        database.withTransaction {
            val candidates = occurrenceDao.getCleanupCandidates(now)
            var deleted = 0
            for (occ in candidates) {
                try {
                    val task = taskDao.getTaskById(occ.taskId)
                    // Only COMPLETED + Repeat tasks get a fresh instance
                    if (occ.status == OccurrenceStatus.COMPLETED &&
                        task != null &&
                        task.repeatRule != com.neurasamu.build.sl_tasker.data.model.RepeatRule.ONCE
                    ) {
                        // Only create if no PENDING already exists for this task
                        val existingPending = taskDao.getPendingOverdueOccurrences(Long.MAX_VALUE)
                            .any { it.taskId == task.id && it.status == OccurrenceStatus.PENDING }
                        if (!existingPending) {
                            val scheduledAt: Long = when (task.scheduleMode) {
                                ScheduleMode.NO_TIME -> now
                                ScheduleMode.TIME -> {
                                    com.neurasamu.build.sl_tasker.domain.scheduler.ScheduleHelper
                                        .nextTrigger(
                                            hour = task.reminderMinutesOfDay / 60,
                                            minute = task.reminderMinutesOfDay % 60,
                                            daysCsv = task.customRepeatDays,
                                            repeat = task.repeatRule
                                        ) ?: (now + 60_000L)
                                }
                            }
                            val deadlineAt: Long = when (task.scheduleMode) {
                                ScheduleMode.NO_TIME -> {
                                    if (task.validityDays > 0)
                                        scheduledAt + task.validityDays.toLong() * 24L * 60L * 60L * 1000L
                                    else
                                        Long.MAX_VALUE - 1L
                                }
                                ScheduleMode.TIME -> scheduledAt + task.durationMinutes * 60L * 1000L
                            }
                            occurrenceDao.insertOccurrence(
                                OccurrenceEntity(
                                    taskId = task.id,
                                    scheduledAt = scheduledAt,
                                    deadlineAt = deadlineAt,
                                    durationMinutes = task.durationMinutes,
                                    createdAt = now
                                )
                            )
                        }
                    }
                    occurrenceDao.deleteOccurrenceById(occ.id)
                    deleted++
                } catch (_: Throwable) {
                    // Skip this one, continue
                }
            }
            deleted
        }
    }
}
