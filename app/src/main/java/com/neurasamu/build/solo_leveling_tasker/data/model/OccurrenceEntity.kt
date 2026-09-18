package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "occurrences",
    indices = [Index(value = ["taskId"]), Index(value = ["status"])]
)
data class OccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val taskId: Long,
    val scheduledAt: Long,
    val deadlineAt: Long,
    val durationMinutes: Int = 30,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val penaltyAppliedCount: Int = 0,
    val startedAt: Long? = null,
    val criticalEndsAt: Long? = null
)
