package com.neurasamu.build.sl_tasker.data.block

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.blockDataStore by preferencesDataStore("block_prefs")

enum class BlockMode { OFF, TEST, STRICT }

data class BlockState(
    val mode: BlockMode = BlockMode.OFF,
    val selectedPackages: Set<String> = emptySet()
)

class BlockPrefs(private val context: Context) {

    private val keyMode = stringPreferencesKey("mode")
    private val keySelected = stringPreferencesKey("selected")

    val state: Flow<BlockState> = context.blockDataStore.data.map { p ->
        val mode = runCatching { BlockMode.valueOf(p[keyMode] ?: "OFF") }.getOrDefault(BlockMode.OFF)
        val pkgs = (p[keySelected] ?: "").split(",").filter { it.isNotBlank() }.toSet()
        BlockState(mode, pkgs)
    }

    suspend fun setMode(mode: BlockMode) {
        context.blockDataStore.edit { it[keyMode] = mode.name }
    }

    suspend fun setSelected(packages: Set<String>) {
        context.blockDataStore.edit { it[keySelected] = packages.joinToString(",") }
    }

    suspend fun togglePackage(pkg: String, on: Boolean) {
        context.blockDataStore.edit { p ->
            val cur = (p[keySelected] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (on) cur.add(pkg) else cur.remove(pkg)
            p[keySelected] = cur.joinToString(",")
        }
    }
}
