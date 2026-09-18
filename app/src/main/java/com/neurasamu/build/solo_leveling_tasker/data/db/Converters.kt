package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.TypeConverter
import com.neurasamu.build.solo_leveling_tasker.data.model.Category
import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.DismissMethod
import com.neurasamu.build.solo_leveling_tasker.data.model.EventType
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.solo_leveling_tasker.data.model.Priority
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryStatus
import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskType

class Converters {

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIUM)

    @TypeConverter
    fun fromTaskType(value: TaskType): String = value.name

    @TypeConverter
    fun toTaskType(value: String): TaskType = runCatching { TaskType.valueOf(value) }.getOrDefault(TaskType.DAY)

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = runCatching { Difficulty.valueOf(value) }.getOrDefault(Difficulty.NORMAL)

    @TypeConverter
    fun fromRepeatRule(value: RepeatRule): String = value.name

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule = runCatching { RepeatRule.valueOf(value) }.getOrDefault(RepeatRule.ONCE)

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus): String = value.name

    @TypeConverter
    fun toOccurrenceStatus(value: String): OccurrenceStatus = runCatching { OccurrenceStatus.valueOf(value) }.getOrDefault(OccurrenceStatus.PENDING)

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = runCatching { EventType.valueOf(value) }.getOrDefault(EventType.PENALTY_APPLIED)

    @TypeConverter
    fun fromRecoveryStatus(value: RecoveryStatus): String = value.name

    @TypeConverter
    fun toRecoveryStatus(value: String): RecoveryStatus = runCatching { RecoveryStatus.valueOf(value) }.getOrDefault(RecoveryStatus.ACTIVE)

    @TypeConverter
    fun fromCategory(value: Category): String = value.name

    @TypeConverter
    fun toCategory(value: String): Category = runCatching { Category.valueOf(value) }.getOrDefault(Category.WORK)

    @TypeConverter
    fun fromDismissMethod(value: DismissMethod): String = value.name

    @TypeConverter
    fun toDismissMethod(value: String): DismissMethod = runCatching { DismissMethod.valueOf(value) }.getOrDefault(DismissMethod.EASY)
}
