package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.data.repository.TaskRepository

class OverdueTaskWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = TaskRepository(database)
            val now = System.currentTimeMillis()

            val overdueOccurrences = repository.getOverdueOccurrences(now)
            overdueOccurrences.forEach { occurrence ->
                repository.markOccurrenceMissed(occurrence.id)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
