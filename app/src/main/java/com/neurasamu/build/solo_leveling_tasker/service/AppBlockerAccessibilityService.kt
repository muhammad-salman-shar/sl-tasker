package com.neurasamu.build.solo_leveling_tasker.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.neurasamu.build.solo_leveling_tasker.data.datastore.UserPreferences
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var userPreferences: UserPreferences
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        userPreferences = UserPreferences(applicationContext)
        database = AppDatabase.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage == packageName) return

        serviceScope.launch {
            try {
                val stats = database.playerStatsDao().getPlayerStats()
                val isFocusLockActive = stats?.focusLockActive == true

                if (!isFocusLockActive) return@launch

                val blockedPackages = userPreferences.blockedPackagesFlow.first()
                if (blockedPackages.contains(targetPackage)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            applicationContext,
                            "⚔️ SYSTEM WARNING: Focus Lock active! Clear your quest first.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
