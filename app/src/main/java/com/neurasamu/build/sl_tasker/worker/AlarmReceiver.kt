package com.neurasamu.build.sl_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import com.neurasamu.build.sl_tasker.service.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val pinCode = intent.getStringExtra("PIN_CODE") ?: ""

        // Schedule next occurrence (or disable if one-time)
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
                if (!alarm.enabled) return@launch
                if (alarm.repeatRule == RepeatRule.ONCE) {
                    db.alarmDao().updateEnabled(alarmId, false)
                } else {
                    AlarmScheduler(context).scheduleAlarm(alarm)
                }
            } catch (_: Throwable) {
            } finally {
                pendingResult.finish()
            }
        }

        val snoozeEnabled = intent.getBooleanExtra("SNOOZE_ENABLED", true)
        val snoozeMinutes = intent.getIntExtra("SNOOZE_MINUTES", 10)
        val vibrate = intent.getBooleanExtra("VIBRATE", true)
        val soundUri = intent.getStringExtra("SOUND_URI") ?: ""
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
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
