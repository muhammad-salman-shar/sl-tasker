package com.neurasamu.build.sl_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import com.neurasamu.build.sl_tasker.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Acquire wake lock so service can start even in deep sleep
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SLTasker:AlarmReceiver"
        )
        try {
            wakeLock.acquire(60_000L) // 60s max
        } catch (_: Throwable) {}

        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val pinCode = intent.getStringExtra("PIN_CODE") ?: ""
        val snoozeEnabled = intent.getBooleanExtra("SNOOZE_ENABLED", true)
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 10)
        val vibrate = intent.getBooleanExtra("VIBRATE", true)
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""

        // Start foreground service immediately
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("DISMISS_METHOD", dismissMethod)
            putExtra("PIN_CODE", pinCode)
            putExtra("SNOOZE_ENABLED", snoozeEnabled)
            putExtra("SNOOZE_MINUTES", snoozeMinutes)
            putExtra("VIBRATE", vibrate)
            putExtra("SOUND_URI", soundUri)
        }
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Throwable) {}

        // Reschedule next occurrence / disable in background
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null && alarm.enabled) {
                    if (alarm.repeatRule == RepeatRule.ONCE) {
                        db.alarmDao().updateEnabled(alarmId, false)
                    } else {
                        AlarmScheduler(context).scheduleAlarm(alarm)
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try { wakeLock.release() } catch (_: Throwable) {}
                pendingResult.finish()
            }
        }

        // Fallback: release lock after 30s if background job stalls
        scope.launch {
            delay(30_000L)
            try { if (wakeLock.isHeld) wakeLock.release() } catch (_: Throwable) {}
        }
    }
}
