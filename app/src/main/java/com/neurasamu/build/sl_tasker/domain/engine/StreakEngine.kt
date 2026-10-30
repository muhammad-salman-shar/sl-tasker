package com.neurasamu.build.sl_tasker.domain.engine

import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import java.util.Calendar

object StreakEngine {

    data class Result(
        val newStreak: Int,
        val newLastDay: Long,
        val broken: Boolean,
        val milestoneReached: Int?
    )

    private val MILESTONES = setOf(3, 7, 14, 30, 60, 100, 365)

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun compute(current: PlayerStatsEntity): Result {
        val today = startOfDay(System.currentTimeMillis())
        val last = if (current.lastStreakDayEpoch <= 0L) 0L
                   else startOfDay(current.lastStreakDayEpoch)

        if (last == 0L) {
            return Result(1, today, broken = false, milestoneReached = null)
        }
        if (last == today) {
            return Result(current.streak, today, broken = false, milestoneReached = null)
        }
        val dayDiff = ((today - last) / (24L * 60L * 60L * 1000L)).toInt()
        return when {
            dayDiff == 1 -> {
                val ns = current.streak + 1
                val milestone = if (ns in MILESTONES) ns else null
                Result(ns, today, broken = false, milestoneReached = milestone)
            }
            else -> {
                Result(1, today, broken = current.streak > 0, milestoneReached = null)
            }
        }
    }
}
