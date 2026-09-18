package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: Category = Category.WORK,
    val priority: Priority = Priority.MEDIUM,
    val taskType: TaskType = TaskType.DAY,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val repeatRule: RepeatRule = RepeatRule.ONCE,
    val customRepeatDays: String = "",
    val durationMinutes: Int = 30,
    val reminderOffsetMinutes: Int = 10,
    val criticalTimerMinutes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
