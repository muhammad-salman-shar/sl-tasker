package com.neurasamu.build.sl_tasker.data.timer

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.timerDataStore by preferencesDataStore("active_timer_store")

class TimerStore(private val context: Context) {

    private val kTimer = stringPreferencesKey("active_timer")

    suspend fun save(timer: ActiveTimer) {
        context.timerDataStore.edit { it[kTimer] = timer.toJson() }
    }

    suspend fun load(): ActiveTimer? {
        val json = context.timerDataStore.data.first()[kTimer] ?: return null
        return ActiveTimer.fromJson(json)
    }

    suspend fun clear() {
        context.timerDataStore.edit { it.remove(kTimer) }
    }
}
