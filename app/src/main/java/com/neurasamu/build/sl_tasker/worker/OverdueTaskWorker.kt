package com.neurasamu.build.sl_tasker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neurasamu.build.sl_tasker.data.datastore.UserPreferences
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository

class OverdueTaskWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = TaskRepository(database)
            val now = System.currentTimeMillis()
            val autoDelete = runCatching {
                UserPreferences(applicationContext).isAutoDeleteMissedTasksEnabled()
            }.getOrDefault(false)

            repository.applyOverduePenalties(now, autoDeleteMissed = autoDelete)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
