package com.neurasamu.build.solo_leveling_tasker.data.model

enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TaskType {
    DAY,
    CRITICAL,
    DEADLINE
}

enum class Difficulty(val baseEp: Int) {
    EASY(5),
    NORMAL(10),
    HARD(15),
    EXTREME(20)
}

enum class RepeatRule {
    ONCE,
    DAILY,
    MON_TO_FRI,
    SAT_AND_SUN,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

enum class OccurrenceStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    MISSED
}

enum class EventType {
    OCCURRENCE_COMPLETED,
    OCCURRENCE_MISSED,
    PENALTY_APPLIED,
    RECOVERY_COMPLETED,
    LEVEL_UP
}

enum class RecoveryStatus {
    ACTIVE,
    COMPLETED,
    FAILED
}

enum class Category {
    CODING,
    STUDY,
    FITNESS,
    WORK,
    LIFE
}

enum class DismissMethod {
    EASY,
    PIN
}

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}
