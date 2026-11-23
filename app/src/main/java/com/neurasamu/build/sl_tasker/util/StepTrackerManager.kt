package com.neurasamu.build.sl_tasker.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.min

/**
 * Hardware Step Counter manager (Walk-to-Earn).
 * - Uses Sensor.TYPE_STEP_COUNTER (and TYPE_STEP_DETECTOR fallback).
 * - Zero background battery drain: only listens while registered in foreground/active session.
 * - Every 500 real steps = +25 HP + 10 minutes (600s) of earnable scroll-time quota.
 */
object StepTrackerManager : SensorEventListener {

    const val STEPS_PER_REWARD = 500
    const val HP_PER_500_STEPS = 25
    const val SCROLL_SECONDS_PER_500_STEPS = 600

    private const val PREFS = "sl_step_tracker_prefs"
    private const val KEY_DAY_EPOCH = "day_epoch"
    private const val KEY_BASELINE_RAW = "baseline_raw"
    private const val KEY_TODAY_STEPS = "today_steps"
    private const val KEY_WEEKLY_STEPS = "weekly_steps"
    private const val KEY_WEEK_EPOCH = "week_epoch"
    private const val KEY_REWARDED_MILESTONES = "rewarded_milestones_today"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var sensorManager: SensorManager? = null

    @Volatile
    private var isListening = false

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps

    private val _weeklySteps = MutableStateFlow(0)
    val weeklySteps: StateFlow<Int> = _weeklySteps

    private val _sensorAvailable = MutableStateFlow(true)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        rolloverIfNewDay(ctx)
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _todaySteps.value = p.getInt(KEY_TODAY_STEPS, 0)
        _weeklySteps.value = p.getInt(KEY_WEEKLY_STEPS, 0)
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sensorManager = sm
        val hasCounter = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sm?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        _sensorAvailable.value = hasCounter
    }

    fun startListening(context: Context) {
        val ctx = context.applicationContext
        if (appContext == null) init(ctx)
        if (isListening) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val sm = sensorManager ?: (ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager) ?: return
        val counter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val detector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        val sensor = counter ?: detector ?: run {
            _sensorAvailable.value = false
            return
        }
        try {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            isListening = true
        } catch (_: Throwable) {}
    }

    fun stopListening() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Throwable) {}
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val ctx = appContext ?: return
        val ev = event ?: return
        rolloverIfNewDay(ctx)
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        when (ev.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val rawTotal = ev.values.firstOrNull()?.toInt() ?: return
                val savedBaseline = p.getInt(KEY_BASELINE_RAW, -1)
                if (savedBaseline < 0 || rawTotal < savedBaseline) {
                    val carryOver = p.getInt(KEY_TODAY_STEPS, 0)
                    p.edit().putInt(KEY_BASELINE_RAW, rawTotal - carryOver).apply()
                    return
                }
                val newToday = (rawTotal - savedBaseline).coerceAtLeast(0)
                val prevToday = p.getInt(KEY_TODAY_STEPS, 0)
                val delta = (newToday - prevToday).coerceAtLeast(0)
                if (delta > 0) {
                    val newWeekly = p.getInt(KEY_WEEKLY_STEPS, 0) + delta
                    p.edit()
                        .putInt(KEY_TODAY_STEPS, newToday)
                        .putInt(KEY_WEEKLY_STEPS, newWeekly)
                        .apply()
                    _todaySteps.value = newToday
                    _weeklySteps.value = newWeekly
                    checkAndAwardStepMilestones(ctx, newToday)
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                val inc = ev.values.size.coerceAtLeast(1)
                val newToday = p.getInt(KEY_TODAY_STEPS, 0) + inc
                val newWeekly = p.getInt(KEY_WEEKLY_STEPS, 0) + inc
                p.edit()
                    .putInt(KEY_TODAY_STEPS, newToday)
                    .putInt(KEY_WEEKLY_STEPS, newWeekly)
                    .apply()
                _todaySteps.value = newToday
                _weeklySteps.value = newWeekly
                checkAndAwardStepMilestones(ctx, newToday)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkAndAwardStepMilestones(ctx: Context, currentSteps: Int) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val earnedMilestones = currentSteps / STEPS_PER_REWARD
        val alreadyAwarded = p.getInt(KEY_REWARDED_MILESTONES, 0)
        if (earnedMilestones <= alreadyAwarded) return

        val newlyEarned = earnedMilestones - alreadyAwarded
        p.edit().putInt(KEY_REWARDED_MILESTONES, earnedMilestones).apply()

        val hpGain = newlyEarned * HP_PER_500_STEPS
        val scrollSecGain = newlyEarned * SCROLL_SECONDS_PER_500_STEPS

        scope.launch {
            try {
                val db = AppDatabase.getInstance(ctx)
                val statsDao = db.playerStatsDao()
                val cur = statsDao.getPlayerStats() ?: PlayerStatsEntity()
                val newHp = min(GamificationEngine.MAX_HEALTH, cur.health + hpGain)
                statsDao.insertOrUpdate(
                    cur.copy(
                        health = newHp,
                        recoveryModeActive = if (newHp >= 50) false else cur.recoveryModeActive
                    )
                )
                BlockPrefs(ctx).grantScrollQuotaSeconds(scrollSecGain)
                SystemEvents.toast(
                    "Walk-to-Earn (${earnedMilestones * STEPS_PER_REWARD} steps): +$hpGain HP & +${scrollSecGain / 60}m Scroll Quota!"
                )
            } catch (_: Throwable) {}
        }
    }

    private fun rolloverIfNewDay(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        val todayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        val weekKey = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)

        val storedDay = p.getInt(KEY_DAY_EPOCH, -1)
        val storedWeek = p.getInt(KEY_WEEK_EPOCH, -1)

        if (storedDay != todayKey) {
            p.edit()
                .putInt(KEY_DAY_EPOCH, todayKey)
                .putInt(KEY_BASELINE_RAW, -1)
                .putInt(KEY_TODAY_STEPS, 0)
                .putInt(KEY_REWARDED_MILESTONES, 0)
                .apply()
            _todaySteps.value = 0
        }
        if (storedWeek != weekKey) {
            p.edit()
                .putInt(KEY_WEEK_EPOCH, weekKey)
                .putInt(KEY_WEEKLY_STEPS, 0)
                .apply()
            _weeklySteps.value = 0
        }
    }
}
