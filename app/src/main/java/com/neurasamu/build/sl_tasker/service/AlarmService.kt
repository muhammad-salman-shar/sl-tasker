package com.neurasamu.build.sl_tasker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.neurasamu.build.sl_tasker.ui.alarm.AlarmRingActivity

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var ringExtras: Intent? = null

    companion object {
        const val CHANNEL_ID = "alarm_foreground_channel"
        const val NOTIFICATION_ID = 9001
        const val ACTION_STOP = "ACTION_STOP_ALARM"
        const val ACTION_SHOW_RING = "ACTION_SHOW_RING"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopAlarm()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SHOW_RING) {
            val ring = buildRingIntent()
            try { startActivity(ring) } catch (_: Throwable) {}
            return START_STICKY
        }

        val alarmId = intent?.getLongExtra("ALARM_ID", -1L) ?: -1L
        val alarmLabel = intent?.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent?.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val pinCode = intent?.getStringExtra("PIN_CODE") ?: ""
        val snoozeEnabled = intent?.getBooleanExtra("SNOOZE_ENABLED", true) ?: true
        val snoozeMinutes = intent?.getIntExtra("SNOOZE_MINUTES", 10) ?: 10
        val vibrate = intent?.getBooleanExtra("VIBRATE", true) ?: true
        val soundUri = intent?.getStringExtra("SOUND_URI") ?: ""

        // Cache extras so we can rebuild the ring intent from the notification action.
        ringExtras = Intent().apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("DISMISS_METHOD", dismissMethod)
            putExtra("PIN_CODE", pinCode)
            putExtra("SNOOZE_ENABLED", snoozeEnabled)
            putExtra("SNOOZE_MINUTES", snoozeMinutes)
        }

        startForeground(NOTIFICATION_ID, buildNotification(alarmLabel, alarmId))
        playAlarm(vibrate, soundUri)

        // Try to launch ring activity as full-screen
        val ringIntent = buildRingIntent()
        try { startActivity(ringIntent) } catch (_: Throwable) {}

        return START_STICKY
    }

    private fun buildRingIntent(): Intent {
        val extras = ringExtras
        return Intent(this, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (extras != null) putExtras(extras)
        }
    }

    private fun playAlarm(vibrate: Boolean = true, soundUri: String = "") {
        try {
            val alertUri = if (soundUri.isNotBlank()) {
                android.net.Uri.parse(soundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}

        if (vibrate) {
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopAlarm() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        vibrator?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hunter Alarm Ringing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active ringing alarm"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(label: String, alarmId: Long): Notification {
        // Tap on notification → opens the ring screen
        val showIntent = buildRingIntent()
        val showPending = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Show" action also re-opens ring screen (in case user dismissed by accident)
        val actionIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SHOW_RING
        }
        val actionPending = PendingIntent.getService(
            this,
            (alarmId + 50).toInt(),
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ALARM RINGING: $label")
            .setContentText("Tap to open the dismiss screen")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(showPending, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(showPending)
            .addAction(0, "OPEN", actionPending)
            .build()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
