package com.neurasamu.build.sl_tasker.data.block

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.blockDataStore by preferencesDataStore("block_prefs")

val PROTECTED_PACKAGES: Set<String> = setOf(
    "com.neurasamu.build.sl_tasker",
    "com.android.settings",
    "com.android.systemui",
    "com.android.dialer",
    "com.google.android.dialer",
    "com.android.mms",
    "com.google.android.apps.messaging",
    "com.android.phone",
    "com.android.server.telecom"
)

enum class BlockMode { OFF, TEST, STRICT }

data class BlockState(
    val mode: BlockMode = BlockMode.OFF,
    val selectedPackages: Set<String> = emptySet(),
    val sealCode: String = "",
    val sealLabel: String = "",
    val sealLockEnabled: Boolean = false,
    val earnedScrollSeconds: Int = 0,
    val scrollUnlockedUntilMillis: Long = 0L
) {
    val hasRegisteredSeal: Boolean get() = sealCode.isNotBlank()
    fun isScrollSessionActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return scrollUnlockedUntilMillis > nowMillis
    }
}

class BlockPrefs(private val context: Context) {

    private val keyMode = stringPreferencesKey("mode")
    private val keyCritical = booleanPreferencesKey("critical_active")
    private val keySelected = stringPreferencesKey("selected")
    private val keySealCode = stringPreferencesKey("seal_code")
    private val keySealLabel = stringPreferencesKey("seal_label")
    private val keySealLock = booleanPreferencesKey("seal_lock_enabled")
    private val keyScrollQuotaSec = intPreferencesKey("earned_scroll_seconds")
    private val keyScrollUnlockUntil = longPreferencesKey("scroll_unlocked_until")

    val state: Flow<BlockState> = context.blockDataStore.data.map { p ->
        val mode = runCatching { BlockMode.valueOf(p[keyMode] ?: "OFF") }.getOrDefault(BlockMode.OFF)
        val pkgs = (p[keySelected] ?: "").split(",").filter { it.isNotBlank() }.toSet()
        BlockState(
            mode = mode,
            selectedPackages = pkgs,
            sealCode = p[keySealCode] ?: "",
            sealLabel = p[keySealLabel] ?: "",
            sealLockEnabled = p[keySealLock] ?: false,
            earnedScrollSeconds = (p[keyScrollQuotaSec] ?: 0).coerceAtLeast(0),
            scrollUnlockedUntilMillis = p[keyScrollUnlockUntil] ?: 0L
        )
    }

    val criticalActive: Flow<Boolean> = context.blockDataStore.data.map { it[keyCritical] ?: false }

    suspend fun setMode(mode: BlockMode) {
        context.blockDataStore.edit { it[keyMode] = mode.name }
    }

    suspend fun setCriticalActive(on: Boolean) {
        context.blockDataStore.edit { it[keyCritical] = on }
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

    suspend fun registerPhysicalSeal(code: String, label: String) {
        context.blockDataStore.edit { p ->
            p[keySealCode] = code.trim()
            p[keySealLabel] = label.trim().ifBlank { "Physical Seal" }
        }
    }

    suspend fun setSealLockEnabled(enabled: Boolean) {
        context.blockDataStore.edit { p ->
            p[keySealLock] = enabled
        }
    }

    suspend fun grantScrollQuotaSeconds(addSeconds: Int) {
        if (addSeconds <= 0) return
        context.blockDataStore.edit { p ->
            val cur = (p[keyScrollQuotaSec] ?: 0).coerceAtLeast(0)
            p[keyScrollQuotaSec] = (cur + addSeconds).coerceAtMost(7200)
        }
    }

    suspend fun activateScrollUnlock(minutes: Int, consumeEarnedQuota: Boolean = true): Boolean {
        val st = state.first()
        val reqSec = minutes.coerceAtLeast(1) * 60
        if (consumeEarnedQuota && st.earnedScrollSeconds < reqSec) {
            return false
        }
        val now = System.currentTimeMillis()
        context.blockDataStore.edit { p ->
            if (consumeEarnedQuota) {
                val cur = (p[keyScrollQuotaSec] ?: 0).coerceAtLeast(0)
                p[keyScrollQuotaSec] = (cur - reqSec).coerceAtLeast(0)
            }
            val currentUntil = p[keyScrollUnlockUntil] ?: 0L
            val base = maxOf(now, currentUntil)
            p[keyScrollUnlockUntil] = base + reqSec * 1000L
        }
        return true
    }
}
