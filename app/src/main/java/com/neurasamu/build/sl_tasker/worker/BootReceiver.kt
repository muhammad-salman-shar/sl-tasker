package com.neurasamu.build.sl_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != "com.htc.intent.action.QUICKBOOT_POWERON") return

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SLTasker:BootReceiver")
        try { wakeLock.acquire(60_000L) } catch (_: Throwable) {}

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val scheduler = AlarmScheduler(context)
                val enabledAlarms = database.alarmDao().getEnabledAlarms()
                enabledAlarms.forEach { alarm ->
                    try { scheduler.scheduleAlarm(alarm) } catch (_: Throwable) {}
                }

                // Reschedule upcoming task reminders (phones reboot → alarms reset)
                val now = System.currentTimeMillis()
                val upcoming = database.occurrenceDao().getUpcomingPendingOccurrences(now)
                val taskDao = database.taskDao()
                upcoming.take(50).forEach { occ ->
                    try {
                        val task = taskDao.getTaskById(occ.taskId)
                        if (task != null && !task.archived) {
                            scheduler.scheduleReminder(occ.id, occ.scheduledAt, task.title)
                        }
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
            } finally {
                try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Throwable) {}
                pendingResult.finish()
            }
        }
    }
}
