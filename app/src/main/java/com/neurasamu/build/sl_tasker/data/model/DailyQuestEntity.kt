package com.neurasamu.build.sl_tasker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_quests",
    indices = [Index(value = ["dayEpoch"])]
)
data class DailyQuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val description: String,
    val targetCount: Int,
    val unit: String,
    val bonusEp: Int,
    val bonusHp: Int,
    val dayEpoch: Long,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
