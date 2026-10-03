package com.neurasamu.build.sl_tasker.domain.scheduler

import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import java.util.Calendar

/**
 * Days encoded 0=Sun, 1=Mon, ..., 6=Sat (matches task dialog DayShort list).
 */
object ScheduleHelper {

    fun parseDays(csv: String): Set<Int> =
        if (csv.isBlank()) emptySet()
        else csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    /**
     * Returns next trigger millis.
     * - ONCE + no days  -> today at (h,m) if future else tomorrow
     * - ONCE + days     -> next matching weekday
     * - Repeat + no days -> daily
     * - Repeat + days   -> next matching weekday (loop)
     * Returns null only if ONCE + days and no future match (should not happen).
     */
    fun nextTrigger(
        hour: Int,
        minute: Int,
        daysCsv: String,
        repeat: RepeatRule,
        from: Long = System.currentTimeMillis()
    ): Long? {
        val days = parseDays(daysCsv)
        val cal = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeat == RepeatRule.ONCE && days.isEmpty()) {
            if (cal.timeInMillis <= from) cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        val effectiveDays: Set<Int> = when {
            days.isNotEmpty() -> days
            repeat != RepeatRule.ONCE -> (0..6).toSet()
            else -> emptySet()
        }
        if (effectiveDays.isEmpty()) return null

        for (offset in 0..7) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, offset)
            if (c.timeInMillis <= from) continue
            val dow = c.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
            if (dow in effectiveDays) return c.timeInMillis
        }
        return null
    }
}
