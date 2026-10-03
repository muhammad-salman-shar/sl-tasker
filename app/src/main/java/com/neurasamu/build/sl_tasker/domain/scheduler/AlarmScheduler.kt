package com.neurasamu.build.sl_tasker.domain.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.neurasamu.build.sl_tasker.data.model.AlarmEntity
import com.neurasamu.build.sl_tasker.worker.AlarmReceiver
import com.neurasamu.build.sl_tasker.worker.ReminderReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.enabled) return

        val triggerAt = ScheduleHelper.nextTrigger(
            hour = alarm.hour,
            minute = alarm.minute,
            daysCsv = alarm.customDays,
            repeat = alarm.repeatRule
        ) ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("DISMISS_METHOD", alarm.dismissMethod.name)
            putExtra("PIN_CODE", alarm.pinCode)
            putExtra("SNOOZE_ENABLED", alarm.snoozeEnabled)
            putExtra("SNOOZE_MINUTES", alarm.snoozeMinutes)
            putExtra("VIBRATE", alarm.vibrate)
            putExtra("SOUND_URI", alarm.soundUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setAlarmClock for reliability across Doze
        val showIntent = Intent(context, com.neurasamu.build.sl_tasker.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPi = PendingIntent.getActivity(
            context,
            alarm.id.toInt() + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showPi),
                pendingIntent
            )
        } catch (_: Throwable) {
            scheduleExact(triggerAt, pendingIntent)
        }
    }

    fun snooze(alarmId: Long, minutes: Int, label: String, dismissMethod: String, pinCode: String) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("DISMISS_METHOD", dismissMethod)
            putExtra("PIN_CODE", pinCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (alarmId + 700_000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, pi),
                pi
            )
        } catch (_: Throwable) {
            scheduleExact(triggerAt, pi)
        }
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleReminder(occurrenceId: Long, triggerAtMillis: Long, taskTitle: String) {
        // Allow trigger up to 5 seconds in the past (race condition when user creates task)
        if (triggerAtMillis + 5_000L < System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("OCCURRENCE_ID", occurrenceId)
            putExtra("TASK_TITLE", taskTitle)
        }

        val requestCode = (occurrenceId + 100_000).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing first
        alarmManager.cancel(pendingIntent)

        val showIntent = Intent(context, com.neurasamu.build.sl_tasker.ui.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OCCURRENCE_ID", occurrenceId)
        }
        val showPending = PendingIntent.getActivity(
            context,
            requestCode + 1,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Primary: setAlarmClock — strongest mode, bypasses Doze, shows status bar icon
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPending),
                pendingIntent
            )
            return
        } catch (_: SecurityException) {
        } catch (_: Throwable) {
        }

        // Fallback
        scheduleExact(triggerAtMillis, pendingIntent)
    }

    fun cancelReminder(occurrenceId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            (occurrenceId + 100_000).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun scheduleExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
