package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val scheduler = AlarmScheduler(context)

                val enabledAlarms = database.alarmDao().getEnabledAlarms()
                enabledAlarms.forEach { alarm ->
                    scheduler.scheduleAlarm(alarm)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
