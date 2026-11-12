package com.neurasamu.build.sl_tasker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.block.PROTECTED_PACKAGES
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.ui.block.BlockOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var blockPrefs: BlockPrefs
    private lateinit var database: AppDatabase
    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastPenaltyCheckAt = 0L
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

        // Per-package debounce only (800ms) — no global throttle
        val t = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && t - lastBlockedAt < 800L) return

        serviceScope.launch {
            try {
                val now = System.currentTimeMillis()

                // CRITICAL FIX: Refresh HP before deciding to block.
                // This ensures stale HP (from when app was closed) gets updated.
                // Throttled to once per 20s to avoid DB spam.
                if (now - lastPenaltyCheckAt > 20_000L) {
                    lastPenaltyCheckAt = now
                    try {
                        TaskRepository(database).applyOverduePenalties(now)
                    } catch (_: Throwable) {}
                }

                val state = blockPrefs.state.first()
                val stats = database.playerStatsDao().getPlayerStats()
                val health = stats?.health ?: 100

                val testOn = state.mode == BlockMode.TEST
                val strictOn = state.mode == BlockMode.STRICT
                val healthLow = health < 40

                val shouldBlock: Boolean = when {
                    testOn -> pkg in state.selectedPackages
                    healthLow -> if (strictOn) true else pkg in state.selectedPackages
                    else -> false
                }
                if (!shouldBlock) return@launch

                lastBlockedPkg = pkg
                lastBlockedAt = System.currentTimeMillis()

                // Launch overlay with retry (startActivity from a service can fail silently)
                mainHandler.post { launchOverlayWithRetry(pkg, retries = 2) }
            } catch (_: Throwable) {}
        }
    }

    private fun launchOverlayWithRetry(pkg: String, retries: Int) {
        try {
            val intent = Intent(applicationContext, BlockOverlayActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra("blocked_pkg", pkg)
            }
            applicationContext.startActivity(intent)
        } catch (_: Throwable) {
            if (retries > 0) {
                mainHandler.postDelayed({ launchOverlayWithRetry(pkg, retries - 1) }, 300L)
            } else {
                try {
                    Toast.makeText(applicationContext, "Blocked: $pkg", Toast.LENGTH_SHORT).show()
                } catch (_: Throwable) {}
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
