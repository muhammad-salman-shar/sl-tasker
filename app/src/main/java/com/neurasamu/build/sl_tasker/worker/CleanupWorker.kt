package com.neurasamu.build.sl_tasker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neurasamu.build.sl_tasker.data.datastore.UserPreferences
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository

class CleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repo = TaskRepository(db)
            val autoDelete = runCatching {
                UserPreferences(applicationContext).isAutoDeleteMissedTasksEnabled()
            }.getOrDefault(false)
            repo.performCleanup(System.currentTimeMillis(), autoDeleteMissed = autoDelete)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
