package com.neurasamu.build.solo_leveling_tasker.data.repository

import com.neurasamu.build.solo_leveling_tasker.data.db.AlarmDao
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AlarmRepository(private val alarmDao: AlarmDao) {

    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun insertAlarm(alarm: AlarmEntity): Long = withContext(Dispatchers.IO) {
        alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun deleteAlarmById(id: Long) = withContext(Dispatchers.IO) {
        alarmDao.deleteAlarmById(id)
    }

    suspend fun getAlarmById(id: Long): AlarmEntity? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmById(id)
    }

    suspend fun getEnabledAlarms(): List<AlarmEntity> = withContext(Dispatchers.IO) {
        alarmDao.getEnabledAlarms()
    }

    suspend fun toggleAlarm(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        alarmDao.updateEnabled(id, enabled)
    }
}
