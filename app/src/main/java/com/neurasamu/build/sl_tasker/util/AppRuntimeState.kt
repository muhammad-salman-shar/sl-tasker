package com.neurasamu.build.sl_tasker.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.data.timer.ActiveTimer
import com.neurasamu.build.sl_tasker.data.timer.TimerRuntime
import com.neurasamu.build.sl_tasker.data.timer.TimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

object AppRuntimeState {

    private val startedActivities = AtomicInteger(0)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized = false

    val isAppInForeground: Boolean
        get() = startedActivities.get() > 0

    fun init(app: Application) {
        if (initialized) return
        initialized = true
        appContext = app.applicationContext

        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                val prev = startedActivities.getAndIncrement()
                if (prev == 0) {
                    // Catch up any time spent late OUTSIDE SL Tasker before entering,
                    // only if no task timer was actively running.
                    val ctx = appContext ?: return
                    val now = System.currentTimeMillis()
                    ioScope.launch {
                        try {
                            if (!hasActiveTimer()) {
                                val db = AppDatabase.getInstance(ctx)
                                TaskRepository(db).applyOverduePenalties(
                                    now = now,
                                    allowForegroundCatchup = true
                                )
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                val remaining = startedActivities.decrementAndGet()
                if (remaining <= 0) {
                    startedActivities.set(0)
                    // Stamp overdue clock on exit so minutes spent inside SL Tasker never
                    // accumulate retroactive HP drain after leaving the app.
                    val ctx = appContext ?: return
                    val now = System.currentTimeMillis()
                    ioScope.launch {
                        try {
                            val db = AppDatabase.getInstance(ctx)
                            TaskRepository(db).freezeOverdueClock(now)
                        } catch (_: Throwable) {}
                    }
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    suspend fun getActiveTimer(): ActiveTimer? {
        val mem = TimerRuntime.active.value
        if (mem != null && mem.remainingSeconds() > 0) return mem
        val ctx = appContext ?: return null
        val saved = try {
            TimerStore(ctx).load()
        } catch (_: Throwable) {
            null
        }
        return if (saved != null && saved.remainingSeconds() > 0) saved else null
    }

    suspend fun hasActiveTimer(): Boolean = getActiveTimer() != null
}
