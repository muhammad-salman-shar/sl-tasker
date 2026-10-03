package com.neurasamu.build.sl_tasker.data.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiDataStore by preferencesDataStore("ai_prefs")

data class AiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val timeoutSec: Int = 30
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank()
}

class AiSettings(private val context: Context) {

    private val kUrl = stringPreferencesKey("ai_base_url")
    private val kKey = stringPreferencesKey("ai_api_key")
    private val kModel = stringPreferencesKey("ai_model")
    private val kTimeout = intPreferencesKey("ai_timeout")

    val config: Flow<AiConfig> = context.aiDataStore.data.map { p ->
        AiConfig(
            baseUrl = p[kUrl] ?: "",
            apiKey = p[kKey] ?: "",
            model = p[kModel] ?: "",
            timeoutSec = p[kTimeout] ?: 30
        )
    }

    suspend fun save(baseUrl: String, apiKey: String, model: String, timeoutSec: Int) {
        context.aiDataStore.edit { p ->
            p[kUrl] = baseUrl.trim()
            p[kKey] = apiKey.trim()
            p[kModel] = model.trim()
            p[kTimeout] = timeoutSec.coerceIn(5, 120)
        }
    }
}
