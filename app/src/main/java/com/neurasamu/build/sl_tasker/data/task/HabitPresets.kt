package com.neurasamu.build.sl_tasker.data.task

import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.ScheduleMode

data class RespectfulHabitPreset(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val subtitle: String,
    val difficulty: Difficulty,
    val reminderMinutesOfDay: Int,
    val durationMinutes: Int,
    val scheduleMode: ScheduleMode = ScheduleMode.TIME,
    val isAdvanced: Boolean = false,
    val stepGoal: Int = 0
)

/**
 * Culturally inclusive, respectful daily routine presets suitable for all users.
 */
object HabitPresets {
    val all: List<RespectfulHabitPreset> = listOf(
        RespectfulHabitPreset(
            id = "skincare",
            title = "Daily Skincare",
            categoryLabel = "Self-Care",
            subtitle = "Morning & evening gentle routine",
            difficulty = Difficulty.MEDIUM,
            reminderMinutesOfDay = 8 * 60 + 30,
            durationMinutes = 0,
            scheduleMode = ScheduleMode.NO_TIME
        ),
        RespectfulHabitPreset(
            id = "morning_walk",
            title = "Morning Walk",
            categoryLabel = "Wellness",
            subtitle = "25m outdoor walk · 1,000 steps",
            difficulty = Difficulty.HARD,
            reminderMinutesOfDay = 7 * 60,
            durationMinutes = 25,
            scheduleMode = ScheduleMode.NO_TIME,
            isAdvanced = true,
            stepGoal = 1000
        ),
        RespectfulHabitPreset(
            id = "hydration",
            title = "Hydration",
            categoryLabel = "Wellness",
            subtitle = "8 glasses of water through the day",
            difficulty = Difficulty.MEDIUM,
            reminderMinutesOfDay = 10 * 60,
            durationMinutes = 0,
            scheduleMode = ScheduleMode.NO_TIME
        ),
        RespectfulHabitPreset(
            id = "study_focus",
            title = "Study Focus",
            categoryLabel = "Study",
            subtitle = "30m distraction-free deep study",
            difficulty = Difficulty.HARD,
            reminderMinutesOfDay = 16 * 60,
            durationMinutes = 30,
            scheduleMode = ScheduleMode.NO_TIME
        ),
        RespectfulHabitPreset(
            id = "screen_detox",
            title = "Screen Detox",
            categoryLabel = "Mindfulness",
            subtitle = "30m calm offline break",
            difficulty = Difficulty.HARD,
            reminderMinutesOfDay = 21 * 60,
            durationMinutes = 30,
            scheduleMode = ScheduleMode.NO_TIME
        ),
        RespectfulHabitPreset(
            id = "self_care",
            title = "Self-Care",
            categoryLabel = "Self-Care",
            subtitle = "Rest, stretch & personal recharge",
            difficulty = Difficulty.MEDIUM,
            reminderMinutesOfDay = 19 * 60 + 30,
            durationMinutes = 0,
            scheduleMode = ScheduleMode.NO_TIME
        ),
        RespectfulHabitPreset(
            id = "prayer_reflection",
            title = "Prayer / Reflection",
            categoryLabel = "Reflection",
            subtitle = "Quiet gratitude, prayer & intention",
            difficulty = Difficulty.MEDIUM,
            reminderMinutesOfDay = 5 * 60 + 30,
            durationMinutes = 0,
            scheduleMode = ScheduleMode.NO_TIME
        )
    )
}
