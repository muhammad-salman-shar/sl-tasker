package com.neurasamu.build.sl_tasker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.neurasamu.build.sl_tasker.data.block.BlockMode
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.block.PROTECTED_PACKAGES
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
    private var lastBlockedAt = 0L
    private var lastBlockedPkg: String? = null

    override fun onCreate() {
        super.onCreate()
        blockPrefs = BlockPrefs(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage == packageName) return
        if (targetPackage in PROTECTED_PACKAGES) return

        serviceScope.launch {
            try {
                val state = blockPrefs.state.first()
                val shouldBlock = when (state.mode) {
                    BlockMode.OFF -> false
                    BlockMode.TEST -> targetPackage in state.selectedPackages
                    BlockMode.STRICT -> true
                }
                if (!shouldBlock) return@launch

                val now = System.currentTimeMillis()
                if (targetPackage == lastBlockedPkg && now - lastBlockedAt < 400L) return@launch
                lastBlockedPkg = targetPackage
                lastBlockedAt = now

                performGlobalAction(GLOBAL_ACTION_HOME)

                val intent = Intent(applicationContext, BlockOverlayActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                    .putExtra("blocked_pkg", targetPackage)
                applicationContext.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
