package com.neurasamu.build.sl_tasker.data.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.playerProfileStore by preferencesDataStore("player_profile")

data class PlayerProfile(
    val name: String = "",
    val gender: String = "",
    val age: Int = 18,
    val playerId: String = "",
    val introCompleted: Boolean = false
)

class PlayerProfileStore(private val context: Context) {

    private val kName = stringPreferencesKey("name")
    private val kGender = stringPreferencesKey("gender")
    private val kAge = intPreferencesKey("age")
    private val kPlayerId = stringPreferencesKey("playerId")
    private val kCompleted = booleanPreferencesKey("introCompleted")

    val profile: Flow<PlayerProfile> = context.playerProfileStore.data.map { p ->
        PlayerProfile(
            name = p[kName] ?: "",
            gender = p[kGender] ?: "",
            age = p[kAge] ?: 18,
            playerId = p[kPlayerId] ?: "",
            introCompleted = p[kCompleted] ?: false
        )
    }

    suspend fun save(name: String, gender: String, age: Int, playerId: String) {
        context.playerProfileStore.edit { p ->
            p[kName] = name.trim()
            p[kGender] = gender.trim()
            p[kAge] = age.coerceIn(12, 90)
            p[kPlayerId] = playerId.trim()
            p[kCompleted] = true
        }
    }

    suspend fun isCompleted(): Boolean =
        context.playerProfileStore.data.first()[kCompleted] ?: false
}
