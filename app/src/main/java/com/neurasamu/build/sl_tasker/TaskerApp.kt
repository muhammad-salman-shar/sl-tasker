package com.neurasamu.build.sl_tasker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neurasamu.build.sl_tasker.worker.CleanupWorker
import com.neurasamu.build.sl_tasker.worker.OverdueTaskWorker
import java.util.concurrent.TimeUnit

class TaskerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.neurasamu.build.sl_tasker.util.SoundFx.setup()
        createNotificationChannels()
        scheduleOverdueWorker()
        scheduleCleanupWorker()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val questChannel = NotificationChannel(
                "quest_reminders_channel",
                "Quest Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for urgent hunter quests"
                enableVibration(true)
            }

            val alarmChannel = NotificationChannel(
                "alarm_foreground_channel",
                "Hunter Alarm Ringing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active ringing hunter wake alarm"
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(questChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    private fun scheduleCleanupWorker() {
        val work = PeriodicWorkRequestBuilder<CleanupWorker>(
            30, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CleanupWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
    }

    private fun scheduleOverdueWorker() {
        val overdueWorkRequest = PeriodicWorkRequestBuilder<OverdueTaskWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OverdueTaskWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            overdueWorkRequest
        )
    }
}
