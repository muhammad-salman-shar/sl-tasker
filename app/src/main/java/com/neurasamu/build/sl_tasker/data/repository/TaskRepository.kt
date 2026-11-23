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
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.data.model.ScheduleMode
import com.neurasamu.build.sl_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import com.neurasamu.build.sl_tasker.domain.engine.PenaltyEngine
import com.neurasamu.build.sl_tasker.domain.engine.RecurrenceHelper
import com.neurasamu.build.sl_tasker.util.HpAlertNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

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
            if (!task.allowEarlyComplete && System.currentTimeMillis() < occurrence.scheduledAt) {
                return@withTransaction false
            }
            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()

            val nowMs = System.currentTimeMillis()
            val midnightAt = nextMidnightMillis(nowMs)
            val cleanupAt = minOf(nowMs + 24L * 60L * 60L * 1000L, midnightAt)
            val updatedOccurrence = occurrence.copy(
                status = OccurrenceStatus.COMPLETED,
                completedAt = nowMs,
                completedCleanupAt = cleanupAt
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
                streakBonusActive = true,
                isPro = com.neurasamu.build.sl_tasker.security.LicenseManager.isPro()
            )

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
                            "RECOVERED! +${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE} HP"
                        )
                    } else {
                        recoveryQuestDao.updateRecoveryQuest(rq.copy(earnedEp = newEarned))
                        val remaining = rq.requiredEp - newEarned
                        com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                            "Recovery: $remaining EP to go"
                        )
                    }
                }
            } catch (_: Throwable) {}

            playerStatsDao.insertOrUpdate(finalStats)

            try {
                if (streakResult.milestoneReached != null) {
                    com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                        "${streakResult.milestoneReached}-day streak! +$milestoneBonus EP"
                    )
                } else if (streakResult.broken) {
                    com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                        "Streak reset. Start again."
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

            if (task.repeatRule == RepeatRule.ONCE) {
                taskDao.archiveTask(task.id)
            } else if (task.scheduleMode == ScheduleMode.NO_TIME) {
                // NO_TIME repeat: Cleanup worker spawns fresh instance at midnight / 24h reset.
            } else {
                val now = System.currentTimeMillis()
                val existingNext = occurrenceDao.getNextPendingOccurrence(task.id, now)
                if (existingNext == null) {
                    val nextScheduled = RecurrenceHelper.calculateNextOccurrence(
                        baseScheduledAt = occurrence.scheduledAt,
                        repeatRule = task.repeatRule,
                        customRepeatDays = task.customRepeatDays
                    )
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

            try {
                com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                    "Task complete: +${gameResult.epGained} EP, +${gameResult.healthGained} HP"
                )
            } catch (_: Throwable) {}

            true
        }
    }

    suspend fun markOccurrenceMissed(
        occurrenceId: Long,
        autoDeleteMissed: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        database.withTransaction {
            val occurrence = occurrenceDao.getOccurrenceById(occurrenceId) ?: return@withTransaction false
            val task = taskDao.getTaskById(occurrence.taskId) ?: return@withTransaction false
            val currentStats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()

            var healthLost = 0
            var recoveryTriggered = false
            if (task.hpDropEnabled) {
                val penaltyResult = PenaltyEngine.processMissedTask(
                    currentStats = currentStats,
                    priority = task.priority,
                    difficulty = task.difficulty
                )
                healthLost = penaltyResult.healthLost
                recoveryTriggered = penaltyResult.recoveryModeTriggered
                playerStatsDao.insertOrUpdate(penaltyResult.updatedStats)
                HpAlertNotifier.notifyHpDrop(
                    currentStats.health,
                    penaltyResult.updatedStats.health,
                    "Missed Quest: ${task.title}"
                )
            } else {
                // HP Drop disabled for this task: still count totalMissed, zero HP loss
                playerStatsDao.insertOrUpdate(
                    currentStats.copy(totalMissed = currentStats.totalMissed + 1)
                )
            }

            eventDao.insertEvent(
                EventEntity(
                    occurrenceId = occurrenceId,
                    taskId = task.id,
                    type = EventType.OCCURRENCE_MISSED,
                    epDelta = 0,
                    healthDelta = -healthLost,
                    note = if (autoDeleteMissed) "Auto-deleted missed: ${task.title}" else "Missed Quest: ${task.title}"
                )
            )

            if (autoDeleteMissed) {
                handleAutoDeleteMissedOccurrence(occurrence, task)
            } else {
                val updatedOccurrence = occurrence.copy(
                    status = OccurrenceStatus.MISSED,
                    penaltyAppliedCount = occurrence.penaltyAppliedCount + 1,
                    completedCleanupAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
                )
                occurrenceDao.updateOccurrence(updatedOccurrence)

                taskDebtDao.insertDebt(
                    TaskDebtEntity(
                        taskId = task.id,
                        occurrenceId = occurrenceId,
                        originalDeadline = occurrence.deadlineAt
                    )
                )
            }

            if (recoveryTriggered) {
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
                        "PENALTY ZONE: Earn ${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.TARGET_EP} EP to restore ${com.neurasamu.build.sl_tasker.domain.engine.RecoveryEngine.HP_RESTORE} HP"
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

    suspend fun freezeOverdueClock(now: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val overdue = taskDao.getPendingOverdueOccurrences(now)
            for (occ in overdue) {
                val task = taskDao.getTaskById(occ.taskId) ?: continue
                if (task.scheduleMode == ScheduleMode.TIME && occ.lastPenaltyAt < now) {
                    occurrenceDao.updateOccurrence(occ.copy(lastPenaltyAt = now))
                }
            }
        }
    }

    suspend fun applyOverduePenalties(
        now: Long,
        allowForegroundCatchup: Boolean = false,
        autoDeleteMissed: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        val timerActive = com.neurasamu.build.sl_tasker.util.AppRuntimeState.hasActiveTimer()
        val inForeground = com.neurasamu.build.sl_tasker.util.AppRuntimeState.isAppInForeground
        val protected = timerActive || (inForeground && !allowForegroundCatchup)

        database.withTransaction {
            val overdue = taskDao.getPendingOverdueOccurrences(now)
            if (overdue.isEmpty()) return@withTransaction false

            // Still fire approaching auto-delete warnings even while user is in-app
            if (autoDeleteMissed) {
                for (occ in overdue) {
                    val task = taskDao.getTaskById(occ.taskId) ?: continue
                    val expiresAt = computeExpiryMillis(occ, task)
                    if (expiresAt != null && now < expiresAt) {
                        val remainingMs = expiresAt - now
                        if (remainingMs <= 15L * 60L * 1000L) {
                            val minsLeft = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                            HpAlertNotifier.notifyAutoDeleteWarning(occ.id, task.title, minsLeft)
                        }
                    }
                }
            }

            if (protected) {
                for (occ in overdue) {
                    val task = taskDao.getTaskById(occ.taskId) ?: continue
                    if (task.scheduleMode == ScheduleMode.TIME && occ.lastPenaltyAt < now) {
                        occurrenceDao.updateOccurrence(occ.copy(lastPenaltyAt = now))
                    }
                }
                return@withTransaction false
            }

            var stats = playerStatsDao.getPlayerStats() ?: PlayerStatsEntity()
            var changed = false

            for (occ in overdue) {
                val task = taskDao.getTaskById(occ.taskId) ?: continue

                // NO_TIME + lifetime: never missed, never penalized
                if (task.scheduleMode == ScheduleMode.NO_TIME && task.validityDays == 0) continue

                val expiresAt = computeExpiryMillis(occ, task) ?: continue
                val expired = now >= expiresAt

                if (expired) {
                    if (task.scheduleMode == ScheduleMode.NO_TIME && task.hpDropEnabled) {
                        try {
                            val oldHp = stats.health
                            val penaltyResult = PenaltyEngine.processMissedTask(
                                currentStats = stats,
                                priority = task.priority,
                                difficulty = task.difficulty
                            )
                            stats = penaltyResult.updatedStats
                            HpAlertNotifier.notifyHpDrop(
                                oldHp,
                                stats.health,
                                "Missed: ${task.title}"
                            )
                        } catch (_: Throwable) {}
                    } else if (task.scheduleMode == ScheduleMode.NO_TIME) {
                        stats = stats.copy(totalMissed = stats.totalMissed + 1)
                    }

                    try {
                        eventDao.insertEvent(
                            EventEntity(
                                occurrenceId = occ.id,
                                taskId = task.id,
                                type = EventType.OCCURRENCE_MISSED,
                                epDelta = 0,
                                healthDelta = 0,
                                note = if (autoDeleteMissed) "Auto-deleted missed: ${task.title}" else "Missed: ${task.title}"
                            )
                        )
                    } catch (_: Throwable) {}

                    if (autoDeleteMissed) {
                        handleAutoDeleteMissedOccurrence(occ, task)
                    } else {
                        occurrenceDao.updateOccurrence(
                            occ.copy(
                                status = OccurrenceStatus.MISSED,
                                completedCleanupAt = now + 24L * 60L * 60L * 1000L
                            )
                        )
                    }
                    changed = true
                } else if (task.scheduleMode == ScheduleMode.TIME) {
                    // Approaching auto-delete warning (within 15 minutes of expiry)
                    if (autoDeleteMissed) {
                        val remainingMs = expiresAt - now
                        if (remainingMs <= 15L * 60L * 1000L) {
                            val minsLeft = ((remainingMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                            HpAlertNotifier.notifyAutoDeleteWarning(occ.id, task.title, minsLeft)
                        }
                    }

                    // Ongoing per-minute HP penalty ONLY when hpDropEnabled is true
                    if (!task.hpDropEnabled) continue

                    val since = if (occ.lastPenaltyAt == 0L) occ.scheduledAt else occ.lastPenaltyAt
                    val elapsedMin = ((now - since) / 60_000L).toInt()
                    if (elapsedMin <= 0) continue
                    val perMin = PenaltyEngine.penaltyPerMinute(task.difficulty)
                    val damage = perMin * elapsedMin

                    if (stats.health <= PenaltyEngine.MIN_HEALTH) {
                        continue
                    }

                    val oldHp = stats.health
                    val newHealth = (stats.health - damage).coerceAtLeast(PenaltyEngine.MIN_HEALTH)
                    stats = stats.copy(
                        health = newHealth,
                        recoveryModeActive = newHealth < 50 || stats.recoveryModeActive
                    )
                    HpAlertNotifier.notifyHpDrop(
                        oldHp,
                        newHealth,
                        "Overdue: ${task.title}"
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

    private suspend fun handleAutoDeleteMissedOccurrence(occ: OccurrenceEntity, task: TaskEntity) {
        occurrenceDao.deleteOccurrenceById(occ.id)
        if (task.repeatRule == RepeatRule.ONCE) {
            taskDao.archiveTask(task.id)
        } else {
            // Keep recurring task alive by scheduling its next cycle
            val now = System.currentTimeMillis()
            val existingNext = occurrenceDao.getNextPendingOccurrence(task.id, now)
            if (existingNext == null) {
                val nextScheduled = RecurrenceHelper.calculateNextOccurrence(
                    baseScheduledAt = occ.scheduledAt,
                    repeatRule = task.repeatRule,
                    customRepeatDays = task.customRepeatDays
                )
                val minNext = maxOf(now, occ.scheduledAt + 60_000L)
                if (nextScheduled != null && nextScheduled >= minNext) {
                    val durationMillis = task.durationMinutes * 60L * 1000L
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
    }

    private fun computeExpiryMillis(occ: OccurrenceEntity, task: TaskEntity): Long? {
        return when (task.scheduleMode) {
            ScheduleMode.NO_TIME -> {
                if (task.validityDays <= 0) null
                else occ.createdAt + task.validityDays.toLong() * 24L * 60L * 60L * 1000L
            }
            ScheduleMode.TIME -> occ.deadlineAt + 30L * 60L * 1000L
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
            val oldHp = stats.health
            val newHp = (stats.health - amount).coerceAtLeast(0)
            playerStatsDao.insertOrUpdate(
                stats.copy(
                    health = newHp,
                    recoveryModeActive = newHp < 50 || stats.recoveryModeActive
                )
            )
            HpAlertNotifier.notifyHpDrop(
                oldHp,
                newHp,
                "Penalty HP Drain"
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

    /**
     * Cleans up completed tasks after 24h OR at the 12:00 AM midnight boundary
     * so every morning starts with a clean task board.
     */
    suspend fun performCleanup(now: Long, autoDeleteMissed: Boolean = false): Int = withContext(Dispatchers.IO) {
        database.withTransaction {
            val midnightStart = StatsRepository.startOfTodayMidnightMillis(now)
            val candidates = occurrenceDao.getCleanupCandidates(now).toMutableList()

            // Also include any COMPLETED occurrences from before today's 12:00 AM midnight
            val oldCompleted = occurrenceDao.getCompletedBefore(midnightStart)
            val seenIds = candidates.map { it.id }.toMutableSet()
            for (c in oldCompleted) {
                if (seenIds.add(c.id)) candidates.add(c)
            }

            var deleted = 0
            for (occ in candidates) {
                try {
                    val task = taskDao.getTaskById(occ.taskId)
                    if (occ.status == OccurrenceStatus.COMPLETED &&
                        task != null &&
                        task.repeatRule != RepeatRule.ONCE
                    ) {
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
                } catch (_: Throwable) {}
            }

            if (autoDeleteMissed) {
                val missedList = occurrenceDao.getAllMissedOccurrences()
                for (mOcc in missedList) {
                    try {
                        val t = taskDao.getTaskById(mOcc.taskId)
                        if (t != null) {
                            handleAutoDeleteMissedOccurrence(mOcc, t)
                        } else {
                            occurrenceDao.deleteOccurrenceById(mOcc.id)
                        }
                        deleted++
                    } catch (_: Throwable) {}
                }
            }

            deleted
        }
    }

    companion object {
        fun nextMidnightMillis(nowMillis: Long = System.currentTimeMillis()): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}
