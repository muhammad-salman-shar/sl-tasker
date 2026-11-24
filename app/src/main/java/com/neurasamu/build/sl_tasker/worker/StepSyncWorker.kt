package com.neurasamu.build.sl_tasker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neurasamu.build.sl_tasker.util.StepTrackerManager
import java.util.concurrent.TimeUnit

/**
 * Play-Store-compliant periodic step sync (no persistent background foreground service needed).
 * Reads the OS hardware TYPE_STEP_COUNTER delta every 30 minutes when enabled by user.
 */
class StepSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            StepTrackerManager.syncOnceInBackground(applicationContext)
        } catch (_: Throwable) {}
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "sl_all_day_step_sync"

        fun setEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences("sl_step_tracker_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("all_day_step_sync_enabled", enabled).apply()
            val wm = WorkManager.getInstance(context.applicationContext)
            if (enabled) {
                val req = PeriodicWorkRequestBuilder<StepSyncWorker>(30, TimeUnit.MINUTES).build()
                wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
            } else {
                wm.cancelUniqueWork(WORK_NAME)
            }
        }

        fun isEnabled(context: Context): Boolean {
            return context.getSharedPreferences("sl_step_tracker_prefs", Context.MODE_PRIVATE)
                .getBoolean("all_day_step_sync_enabled", false)
        }
    }
}
