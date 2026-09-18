package com.neurasamu.build.solo_leveling_tasker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neurasamu.build.solo_leveling_tasker.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sl_tasker_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val ALARM_DEFAULT_PIN = stringPreferencesKey("alarm_default_pin")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val themeName = preferences[THEME_MODE] ?: ThemeMode.DARK.name
            runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.DARK)
        }

    val blockedPackagesFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[BLOCKED_PACKAGES] ?: setOf(
                "com.instagram.android",
                "com.google.android.youtube",
                "com.facebook.katana",
                "com.twitter.android",
                "com.zhiliaoapp.musically"
            )
        }

    val alarmPinFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[ALARM_DEFAULT_PIN] ?: "1234"
        }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }

    val vibrationEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[VIBRATION_ENABLED] ?: true
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun setBlockedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKED_PACKAGES] = packages
        }
    }

    suspend fun setAlarmPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_DEFAULT_PIN] = pin
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }
}
