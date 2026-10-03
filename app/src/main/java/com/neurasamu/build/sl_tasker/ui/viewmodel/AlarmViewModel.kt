package com.neurasamu.build.sl_tasker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.AlarmEntity
import com.neurasamu.build.sl_tasker.data.model.DismissMethod
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.data.repository.AlarmRepository
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    application: Application,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler
) : AndroidViewModel(application) {

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAlarm(
        hour: Int,
        minute: Int,
        label: String,
        dismissMethod: DismissMethod,
        pinCode: String,
        daysCsv: String,
        isRepeat: Boolean,
        snoozeEnabled: Boolean,
        snoozeMinutes: Int,
        vibrate: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newAlarm = AlarmEntity(
                hour = hour,
                minute = minute,
                label = label,
                dismissMethod = dismissMethod,
                pinCode = pinCode,
                customDays = daysCsv,
                repeatRule = if (isRepeat || daysCsv.isNotBlank()) RepeatRule.CUSTOM else RepeatRule.ONCE,
                snoozeEnabled = snoozeEnabled,
                snoozeMinutes = snoozeMinutes,
                vibrate = vibrate,
                enabled = true
            )
            val id = alarmRepository.insertAlarm(newAlarm)
            val insertedAlarm = newAlarm.copy(id = id)
            alarmScheduler.scheduleAlarm(insertedAlarm)
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmRepository.toggleAlarm(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(alarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            alarmScheduler.cancelAlarm(alarm.id)
            alarmRepository.deleteAlarm(alarm)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(application)
            val repo = AlarmRepository(db.alarmDao())
            val scheduler = AlarmScheduler(application)
            return AlarmViewModel(application, repo, scheduler) as T
        }
    }
}
