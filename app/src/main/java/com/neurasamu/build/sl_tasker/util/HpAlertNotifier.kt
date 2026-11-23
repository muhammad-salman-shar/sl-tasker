package com.neurasamu.build.sl_tasker.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.neurasamu.build.sl_tasker.security.LicenseManager
import com.neurasamu.build.sl_tasker.ui.MainActivity

/**
 * Bullet-proof alert notifier for HP drop warnings and task auto-delete warnings.
 */
object HpAlertNotifier {

    private const val CHANNEL_ID = "sl_hp_drop_alerts"
    private const val CHANNEL_NAME = "HP Drop & Task Alerts"
    private const val NOTIF_ID = 9401
    private const val WARN_PREFS = "sl_auto_delete_warnings"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        ensureChannel(ctx)
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val ch = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Live alerts whenever your Hunter HP drops or tasks near expiry"
                        enableVibration(true)
                    }
                    nm.createNotificationChannel(ch)
                }
            } catch (_: Throwable) {}
        }
    }

    fun notifyAutoDeleteWarning(occurrenceId: Long, taskTitle: String, minutesLeft: Int) {
        val ctx = appContext ?: return
        val key = "warned_$occurrenceId"
        val prefs = ctx.getSharedPreferences(WARN_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        try {
            ensureChannel(ctx)
            prefs.edit().putBoolean(key, true).apply()

            val safeMins = minutesLeft.coerceAtLeast(1)
            val title = "Task Expiring Soon: $taskTitle"
            val body = "Complete \"$taskTitle\" within $safeMins min or it will be auto-deleted as missed."

            val openIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                (occurrenceId % 50_000L).toInt() + 9500,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify((occurrenceId % 50_000L).toInt() + 9500, notif)
        } catch (_: Throwable) {}
    }

    fun notifyHpDrop(oldHp: Int, newHp: Int, reason: String? = null) {
        val ctx = appContext ?: return
        if (!LicenseManager.isPro(ctx)) return
        if (oldHp <= 0 || newHp >= oldHp) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        try {
            ensureChannel(ctx)
            val dropped = oldHp - newHp
            val title = if (newHp <= 0) {
                "CRITICAL: HP Reached 0!"
            } else {
                "HP Dropped (-$dropped) • $newHp / 100 HP Left"
            }
            val body = when {
                newHp <= 0 -> "Your Hunter HP has hit 0. Complete recovery quests immediately!"
                !reason.isNullOrBlank() -> "$reason — $newHp HP remaining."
                newHp <= 30 -> "Danger zone! Only $newHp HP remaining. Complete a task to recover."
                else -> "You lost $dropped HP. Current health: $newHp / 100 HP."
            }

            val openIntent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                ctx,
                NOTIF_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .build()

            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIF_ID, notif)
        } catch (_: Throwable) {}
    }
}
