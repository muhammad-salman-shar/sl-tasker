package com.neurasamu.build.sl_tasker.data.task

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.taskTypesDataStore by preferencesDataStore("task_types_store")

class TaskTypesStore(private val context: Context) {

    companion object {
        private val KEY_LEGACY_SET = stringSetPreferencesKey("types")
        private val KEY_ORDERED_JSON = stringPreferencesKey("types_ordered_json")
        private val KEY_SEEDED = booleanPreferencesKey("types_seeded_from_db")
    }

    private fun parseOrdered(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i, "").trim()
                    if (s.isNotBlank() && none { it.equals(s, ignoreCase = true) }) {
                        add(s)
                    }
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun encodeOrdered(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun mergeLists(orderedJson: String?, legacySet: Set<String>?): List<String> {
        val result = parseOrdered(orderedJson).toMutableList()
        (legacySet ?: emptySet())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .sortedBy { it.lowercase() }
            .forEach { item ->
                if (result.none { it.equals(item, ignoreCase = true) }) {
                    result.add(item)
                }
            }
        return result
    }

    val types: Flow<List<String>> = context.taskTypesDataStore.data.map { prefs ->
        mergeLists(prefs[KEY_ORDERED_JSON], prefs[KEY_LEGACY_SET])
    }

    suspend fun addType(type: String) {
        val trimmed = type.trim()
        if (trimmed.isBlank()) return
        context.taskTypesDataStore.edit { prefs ->
            val current = mergeLists(prefs[KEY_ORDERED_JSON], prefs[KEY_LEGACY_SET]).toMutableList()
            if (current.none { it.equals(trimmed, ignoreCase = true) }) {
                current.add(trimmed)
            }
            prefs[KEY_ORDERED_JSON] = encodeOrdered(current)
            prefs[KEY_LEGACY_SET] = current.toSet()
        }
    }

    suspend fun removeType(type: String) {
        val trimmed = type.trim()
        context.taskTypesDataStore.edit { prefs ->
            val current = mergeLists(prefs[KEY_ORDERED_JSON], prefs[KEY_LEGACY_SET])
                .filterNot { it.equals(trimmed, ignoreCase = true) }
            prefs[KEY_ORDERED_JSON] = encodeOrdered(current)
            prefs[KEY_LEGACY_SET] = current.toSet()
        }
    }

    suspend fun seedOnce(existingTaskTypes: List<String>) {
        val alreadySeeded = context.taskTypesDataStore.data.first()[KEY_SEEDED] == true
        if (alreadySeeded) return
        context.taskTypesDataStore.edit { prefs ->
            val current = mergeLists(prefs[KEY_ORDERED_JSON], prefs[KEY_LEGACY_SET]).toMutableList()
            existingTaskTypes
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { item ->
                    if (current.none { it.equals(item, ignoreCase = true) }) {
                        current.add(item)
                    }
                }
            prefs[KEY_ORDERED_JSON] = encodeOrdered(current)
            prefs[KEY_LEGACY_SET] = current.toSet()
            prefs[KEY_SEEDED] = true
        }
    }
}
