package com.neurasamu.build.sl_tasker.ui.timer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.neurasamu.build.sl_tasker.data.timer.TimerRuntime
import com.neurasamu.build.sl_tasker.data.timer.TimerStore
import com.neurasamu.build.sl_tasker.service.TimerService
import com.neurasamu.build.sl_tasker.ui.theme.SoloLevelingTheme
import kotlinx.coroutines.launch

class TimerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            if (TimerRuntime.active.value == null) {
                val stored = TimerStore(applicationContext).load()
                if (stored != null && stored.remainingSeconds() > 0) {
                    TimerRuntime.set(stored)
                    try {
                        ContextCompat.startForegroundService(
                            this@TimerActivity,
                            TimerService.startIntent(this@TimerActivity, stored)
                        )
                    } catch (_: Throwable) {}
                } else {
                    finish()
                    return@launch
                }
            }

            setContent {
                SoloLevelingTheme {
                    TimerFullScreen(
                        onPause = { sendAction(TimerService.ACTION_PAUSE) },
                        onResume = { sendAction(TimerService.ACTION_RESUME) },
                        onGiveUp = {
                            sendAction(TimerService.ACTION_GIVE_UP)
                            finish()
                        },
                        onClose = { finish() }
                    )
                }
            }
        }

        // Back = minimize app so overlay stays, not close
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        sendAction(TimerService.ACTION_HIDE_OVERLAY)
    }

    override fun onPause() {
        super.onPause()
        if (TimerRuntime.active.value != null) {
            sendAction(TimerService.ACTION_SHOW_OVERLAY)
        }
    }

    private fun sendAction(action: String) {
        try {
            val i = Intent(this, TimerService::class.java).apply { this.action = action }
            startService(i)
        } catch (_: Throwable) {}
    }
}
