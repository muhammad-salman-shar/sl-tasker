package com.neurasamu.build.sl_tasker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PreAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_PRE = "pre_alarm_channel"
        const val ACTION_SKIP = "PRE_ALARM_SKIP"
        const val ACTION_DISMISS = "PRE_ALARM_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val hour = intent.getIntExtra("ALARM_HOUR", 7)
        val minute = intent.getIntExtra("ALARM_MINUTE", 0)
        val action = intent.action

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (action == ACTION_SKIP || action == ACTION_DISMISS) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        if (action == ACTION_SKIP) {
                            // Skip = disable alarm & cancel ring
                            db.alarmDao().updateEnabled(alarmId, false)
                        } else {
                            // Dismiss = turn off this occurrence only (keep enabled for repeat)
                            db.alarmDao().updateEnabled(alarmId, false)
                        }
                    }
                } catch (_: Throwable) {
                } finally {
                    nm.cancel((alarmId + 500_000).toInt())
                    pendingResult.finish()
                }
            }
            return
        }

        // Regular pre-alarm notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_PRE,
                "Upcoming Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warns 5 minutes before an alarm"
                enableVibration(true)
            }
            nm.createNotificationChannel(ch)
        }

        val skipIntent = Intent(context, PreAlarmReceiver::class.java).apply {
            this.action = ACTION_SKIP
            putExtra("ALARM_ID", alarmId)
        }
        val skipPending = PendingIntent.getBroadcast(
            context, (alarmId + 200_001).toInt(), skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, PreAlarmReceiver::class.java).apply {
            this.action = ACTION_DISMISS
            putExtra("ALARM_ID", alarmId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, (alarmId + 200_002).toInt(), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeStr = String.format("%02d:%02d", hour, minute)
        val notification = NotificationCompat.Builder(context, CHANNEL_PRE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm in 5 minutes: $label")
            .setContentText("Ringing at $timeStr. Skip or dismiss now if not needed.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .addAction(0, "SKIP NOW", skipPending)
            .addAction(0, "DISMISS", dismissPending)
            .build()

        nm.notify((alarmId + 500_000).toInt(), notification)
    }
}
