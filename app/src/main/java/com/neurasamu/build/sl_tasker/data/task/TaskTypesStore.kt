package com.neurasamu.build.sl_tasker.data.task

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.taskTypesDataStore by preferencesDataStore("task_types_store")

class TaskTypesStore(private val context: Context) {

    companion object {
        private val KEY = stringSetPreferencesKey("types")
    }

    val types: Flow<List<String>> = context.taskTypesDataStore.data.map { prefs ->
        (prefs[KEY] ?: emptySet())
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
    }

    suspend fun addType(type: String) {
        val trimmed = type.trim()
        if (trimmed.isBlank()) return
        context.taskTypesDataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current + trimmed
        }
    }

    suspend fun removeType(type: String) {
        context.taskTypesDataStore.edit { prefs ->
            val current = prefs[KEY] ?: emptySet()
            prefs[KEY] = current - type
        }
    }
}
