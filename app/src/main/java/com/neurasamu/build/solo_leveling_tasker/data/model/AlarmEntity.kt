package com.neurasamu.build.solo_leveling_tasker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val repeatRule: RepeatRule = RepeatRule.ONCE,
    val customDays: String = "",
    val soundUri: String = "",
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val dismissMethod: DismissMethod = DismissMethod.EASY,
    val pinCode: String = "",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
