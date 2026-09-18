package com.neurasamu.build.solo_leveling_tasker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.neurasamu.build.solo_leveling_tasker.ui.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val occurrenceId = intent.getLongExtra("OCCURRENCE_ID", -1L)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Urgent Quest Pending"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "quest_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Quest Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications for upcoming hunter quests"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OCCURRENCE_ID", occurrenceId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            occurrenceId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚔️ Quest Warning: $taskTitle")
            .setContentText("The System has marked this quest as urgent. Clear it before the deadline!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((occurrenceId + 1000).toInt(), notification)
    }
}
