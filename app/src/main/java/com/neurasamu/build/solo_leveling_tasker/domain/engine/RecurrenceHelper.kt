package com.neurasamu.build.solo_leveling_tasker.domain.engine

import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import java.util.Calendar

object RecurrenceHelper {

    fun calculateNextOccurrence(
        baseScheduledAt: Long,
        repeatRule: RepeatRule,
        customRepeatDays: String = ""
    ): Long? {
        if (repeatRule == RepeatRule.ONCE) return null

        val calendar = Calendar.getInstance().apply {
            timeInMillis = baseScheduledAt
        }
        val now = System.currentTimeMillis()

        while (calendar.timeInMillis <= now) {
            when (repeatRule) {
                RepeatRule.ONCE -> return null
                RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.MON_TO_FRI -> {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    } while (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
                }
                RepeatRule.SAT_AND_SUN -> {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    } while (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY)
                }
                RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatRule.CUSTOM -> {
                    val daysList = parseCustomDays(customRepeatDays)
                    if (daysList.isEmpty()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } else {
                        do {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        } while (!daysList.contains(calendar.get(Calendar.DAY_OF_WEEK)))
                    }
                }
            }
        }
        return calendar.timeInMillis
    }

    private fun parseCustomDays(customDaysStr: String): Set<Int> {
        if (customDaysStr.isBlank()) return emptySet()
        return customDaysStr.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }
}
