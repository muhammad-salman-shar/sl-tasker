package com.neurasamu.build.sl_tasker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.block.PROTECTED_PACKAGES
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.ui.block.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var blockPrefs: BlockPrefs
    private lateinit var database: AppDatabase

    private var lastCheckAt = 0L
    private var lastBlockedPkg: String? = null
    private var lastBlockedAt = 0L

    override fun onCreate() {
        super.onCreate()
        blockPrefs = BlockPrefs(applicationContext)
        database = AppDatabase.getInstance(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg in PROTECTED_PACKAGES) return

        val now = System.currentTimeMillis()
        if (now - lastCheckAt < 250L) return
        lastCheckAt = now

        serviceScope.launch {
            try {
                val state = blockPrefs.state.first()
                val criticalActive = blockPrefs.criticalActive.first()
                val stats = database.playerStatsDao().getPlayerStats()
                val healthLow = (stats?.health ?: 100) <= 30

                val testOn = state.mode == BlockMode.TEST
                val strictOn = state.mode == BlockMode.STRICT

                val triggered = criticalActive || healthLow || testOn
                if (!triggered) return@launch

                val shouldBlock = when {
                    testOn -> pkg in state.selectedPackages
                    strictOn -> true
                    else -> pkg in state.selectedPackages
                }
                if (!shouldBlock) return@launch

                val t = System.currentTimeMillis()
                if (pkg == lastBlockedPkg && t - lastBlockedAt < 600L) return@launch
                lastBlockedPkg = pkg
                lastBlockedAt = t

                val intent = Intent(applicationContext, BlockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra("blocked_pkg", pkg)
                }
                applicationContext.startActivity(intent)
            } catch (_: Throwable) {
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
