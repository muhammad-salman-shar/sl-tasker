package com.neurasamu.build.solo_leveling_tasker.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.neurasamu.build.solo_leveling_tasker.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake Up Hunter"
        val dismissMethod = intent.getStringExtra("DISMISS_METHOD") ?: "EASY"
        val pinCode = intent.getStringExtra("PIN_CODE") ?: ""

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("DISMISS_METHOD", dismissMethod)
            putExtra("PIN_CODE", pinCode)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
