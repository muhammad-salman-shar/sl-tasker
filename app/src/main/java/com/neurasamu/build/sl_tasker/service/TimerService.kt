package com.neurasamu.build.sl_tasker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.data.timer.ActiveTimer
import com.neurasamu.build.sl_tasker.data.timer.TimerRuntime
import com.neurasamu.build.sl_tasker.data.timer.TimerStore
import com.neurasamu.build.sl_tasker.ui.timer.TimerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_fgs_channel"
        const val NOTIF_ID = 9101
        const val ACTION_START = "TIMER_ACTION_START"
        const val ACTION_PAUSE = "TIMER_ACTION_PAUSE"
        const val ACTION_RESUME = "TIMER_ACTION_RESUME"
        const val ACTION_GIVE_UP = "TIMER_ACTION_GIVE_UP"
        const val ACTION_STOP = "TIMER_ACTION_STOP"
        const val ACTION_SHOW_OVERLAY = "TIMER_ACTION_SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "TIMER_ACTION_HIDE_OVERLAY"
        const val EXTRA_OCC_ID = "extra_occ_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_ACCENT = "extra_accent"

        fun startIntent(ctx: Context, timer: ActiveTimer): Intent =
            Intent(ctx, TimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OCC_ID, timer.occurrenceId)
                putExtra(EXTRA_TITLE, timer.taskTitle)
                putExtra(EXTRA_MINUTES, timer.durationMinutes)
                putExtra(EXTRA_ACCENT, timer.accentColorArgb)
            }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var store: TimerStore? = null
    private var repository: TaskRepository? = null

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var timeLabel: TextView? = null
    private var pauseBtn: TextView? = null
    private var overlayVisible = false
    private var overlayAllowed = false

    override fun onCreate() {
        super.onCreate()
        store = TimerStore(applicationContext)
        repository = TaskRepository(AppDatabase.getInstance(applicationContext))
        createChannel()
        ensureForeground(getString(R.string.app_name))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_GIVE_UP -> handleGiveUp()
            ACTION_STOP -> handleStop()
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
            else -> {
                // Service recreated — reload from store
                scope.launch {
                    val saved = store?.load()
                    if (saved != null) {
                        TimerRuntime.set(saved)
                        startTicker()
                        if (overlayVisible) ensureOverlay()
                    } else {
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val occId = intent.getLongExtra(EXTRA_OCC_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Task"
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 1).coerceAtLeast(1)
        val accent = intent.getIntExtra(EXTRA_ACCENT, 0xFF00E5FF.toInt())

        // Avoid duplicate start if already running same occurrence
        val existing = TimerRuntime.active.value
        if (existing != null && existing.occurrenceId == occId && existing.remainingSeconds() > 0) {
            return
        }

        val timer = ActiveTimer(
            occurrenceId = occId,
            taskTitle = title,
            totalSeconds = minutes * 60,
            startedAtMillis = System.currentTimeMillis(),
            elapsedBeforePauseSeconds = 0,
            isPaused = false,
            accentColorArgb = accent,
            durationMinutes = minutes
        )
        TimerRuntime.set(timer)
        scope.launch { store?.save(timer) }
        ensureForeground("Timer: $title")
        startTicker()
        // Overlay starts hidden — TimerActivity will hide/show as needed
    }

    private fun handlePause() {
        val t = TimerRuntime.active.value ?: return
        if (t.isPaused) return
        val paused = t.copy(
            isPaused = true,
            elapsedBeforePauseSeconds = t.elapsedNow(),
            startedAtMillis = System.currentTimeMillis()
        )
        TimerRuntime.set(paused)
        scope.launch { store?.save(paused) }
        updateOverlayUi()
    }

    private fun handleResume() {
        val t = TimerRuntime.active.value ?: return
        if (!t.isPaused) return
        val resumed = t.copy(
            isPaused = false,
            startedAtMillis = System.currentTimeMillis()
        )
        TimerRuntime.set(resumed)
        scope.launch { store?.save(resumed) }
        updateOverlayUi()
    }

    private fun handleGiveUp() {
        val t = TimerRuntime.active.value ?: return
        scope.launch {
            try { repository?.dropHealth(40) } catch (_: Throwable) {}
            clearTimerAndStop()
        }
    }

    private fun handleStop() {
        clearTimerAndStop()
    }

    private suspend fun clearTimerAndStop() {
        tickJob?.cancel()
        TimerRuntime.set(null)
        try { store?.clear() } catch (_: Throwable) {}
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(500L)
                val t = TimerRuntime.active.value ?: break
                if (t.remainingSeconds() <= 0) {
                    onTimerComplete(t)
                    break
                }
                updateOverlayUi()
            }
        }
    }

    private suspend fun onTimerComplete(t: ActiveTimer) {
        try { repository?.completeOccurrence(t.occurrenceId) } catch (_: Throwable) {}
        clearTimerAndStop()
    }

    // ---- Overlay (shown when app backgrounded / timer screen dismissed) ----

    fun showOverlay() {
        overlayVisible = true
        if (TimerRuntime.active.value != null) ensureOverlay()
    }

    fun hideOverlay() {
        overlayVisible = false
        removeOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlay() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(this)) {
            overlayAllowed = false
            return
        }
        overlayAllowed = true

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val accent = TimerRuntime.active.value?.accentColorArgb ?: 0xFF00E5FF.toInt()
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt())
            background = GradientDrawable().apply {
                cornerRadius = 28f * dp
                setColor(0xE60A0E18.toInt())
                setStroke((1.5f * dp).toInt(), accent)
            }
        }

        timeLabel = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.create("monospace", Typeface.BOLD)
            setPadding(0, 0, (10 * dp).toInt(), 0)
        }
        root.addView(timeLabel)

        pauseBtn = TextView(this).apply {
            text = "❚❚"
            setTextColor(accent)
            textSize = 18f
            setPadding((6 * dp).toInt(), 0, (6 * dp).toInt(), 0)
            setOnClickListener {
                if (TimerRuntime.active.value?.isPaused == true) {
                    startService(Intent(this@TimerService, TimerService::class.java).apply {
                        action = ACTION_RESUME
                    })
                } else {
                    startService(Intent(this@TimerService, TimerService::class.java).apply {
                        action = ACTION_PAUSE
                    })
                }
            }
        }
        root.addView(pauseBtn)

        val giveUpBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xFFFF1744.toInt())
            textSize = 18f
            setPadding((6 * dp).toInt(), 0, 0, 0)
            setOnClickListener {
                startService(Intent(this@TimerService, TimerService::class.java).apply {
                    action = ACTION_GIVE_UP
                })
            }
        }
        root.addView(giveUpBtn)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - (160 * dp).toInt())
            y = (200 * dp).toInt()
        }

        // Drag + tap detection
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        root.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX; downY = ev.rawY
                    startX = params.x; startY = params.y
                    moved = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8) moved = true
                    params.x = (startX + dx).toInt()
                    params.y = (startY + dy).toInt()
                    try { wm.updateViewLayout(root, params) } catch (_: Throwable) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // tap → open TimerActivity
                        try {
                            startActivity(Intent(this@TimerService, TimerActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        } catch (_: Throwable) {}
                    }
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(root, params)
            overlayView = root
            overlayParams = params
            updateOverlayUi()
        } catch (_: Throwable) {
            overlayView = null
        }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Throwable) {}
        overlayView = null
        overlayParams = null
        timeLabel = null
        pauseBtn = null
    }

    private fun updateOverlayUi() {
        val t = TimerRuntime.active.value ?: return
        val rem = t.remainingSeconds()
        val mm = rem / 60
        val ss = rem % 60
        timeLabel?.text = "%02d:%02d".format(mm, ss)
        pauseBtn?.text = if (t.isPaused) "▶" else "❚❚"
    }

    // ---- Foreground notification ----

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Task Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun ensureForeground(title: String) {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, TimerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer running")
            .setContentText(title)
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        scope.cancel()
        removeOverlay()
        super.onDestroy()
    }
}
