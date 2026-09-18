package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey
    val id: Long = 1L,
    val health: Int = 100,
    val ep: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val lastStreakDayEpoch: Long = 0L,
    val recoveryModeActive: Boolean = false,
    val focusLockActive: Boolean = false,
    val cycleStartedAt: Long = System.currentTimeMillis(),
    val totalCompleted: Int = 0,
    val totalMissed: Int = 0,
    val totalRecoveries: Int = 0,
    val highestEpMilestone: Int = 0,
    val criticalActiveOccurrenceId: Long? = null
)
