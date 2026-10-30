package com.neurasamu.build.sl_tasker.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.ChecklistRtl
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neurasamu.build.sl_tasker.ui.alarm.AlarmScreen
import com.neurasamu.build.sl_tasker.ui.components.AiHubLogo
import com.neurasamu.build.sl_tasker.ui.home.HomeScreen
import com.neurasamu.build.sl_tasker.ui.stats.StatsScreen
import com.neurasamu.build.sl_tasker.ui.player.PlayerSettingsScreen
import com.neurasamu.build.sl_tasker.ui.tasks.TasksScreen
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.sl_tasker.data.block.BlockPrefs
import com.neurasamu.build.sl_tasker.ui.theme.SoloLevelingTheme
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.viewmodel.AlarmViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.ui.ai.AiSheet
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.neurasamu.build.sl_tasker.data.player.PlayerProfileStore
import com.neurasamu.build.sl_tasker.ui.intro.IntroActivity
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import com.neurasamu.build.sl_tasker.ui.components.LevelUpOverlay
import com.neurasamu.build.sl_tasker.util.SoundFx
import kotlinx.coroutines.flow.collectLatest
import com.neurasamu.build.sl_tasker.util.SystemEvents
import com.neurasamu.build.sl_tasker.ui.viewmodel.DailyQuestViewModel

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels { TaskViewModel.Factory(application) }
    private val statsViewModel: StatsViewModel by viewModels { StatsViewModel.Factory(application) }
    private val alarmViewModel: AlarmViewModel by viewModels { AlarmViewModel.Factory(application) }
    private val dailyQuestViewModel: DailyQuestViewModel by viewModels { DailyQuestViewModel.Factory(application) }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> advanceAfterReturn() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        lifecycleScope.launch {
            val completed = try {
                PlayerProfileStore(applicationContext).isCompleted()
            } catch (_: Throwable) {
                false
            }

            if (!completed) {
                startActivity(Intent(this@MainActivity, IntroActivity::class.java))
                finish()
                return@launch
            }

            setContent {
                SoloLevelingTheme {
                    MainAppScaffold(
                        taskViewModel = taskViewModel,
                        statsViewModel = statsViewModel,
                        alarmViewModel = alarmViewModel,
                        dailyQuestViewModel = dailyQuestViewModel
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Continue permissions flow if we returned from a settings screen
        try {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            if (prefs.getString(permQueueKey, null) != null && !permFlowRunning) {
                advanceAfterReturn()
            }
        } catch (_: Throwable) {}

        // If a timer is active, ensure its service is running and overlay visible.
        try {
            val active = com.neurasamu.build.sl_tasker.data.timer.TimerRuntime.active.value
            if (active != null) {
                val svc = Intent(this, com.neurasamu.build.sl_tasker.service.TimerService::class.java)
                svc.action = com.neurasamu.build.sl_tasker.service.TimerService.ACTION_SHOW_OVERLAY
                startService(svc)
            }
        } catch (_: Throwable) {}
    }

    private val permQueueKey = "pending_perms_queue"

    @Volatile private var permFlowRunning = false

    private fun checkAndRequestPermissions() {
        // Fire runtime notification permission first (popup), then continue chain
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        ensurePermQueueInitialized()
        continueSpecialPermissionsFlow()
    }

    private fun ensurePermQueueInitialized() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getString(permQueueKey, null) == null) {
            prefs.edit()
                .putString(permQueueKey, "exact_alarm,full_screen,battery,overlay")
                .apply()
        }
    }

    private fun continueSpecialPermissionsFlow() {
        if (permFlowRunning) return
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val queue = prefs.getString(permQueueKey, "") ?: ""
        val list = queue.split(",").filter { it.isNotBlank() }
        if (list.isEmpty()) return

        val next = list.first()
        val remaining = list.drop(1)
        prefs.edit().putString(permQueueKey, remaining.joinToString(",")).apply()

        permFlowRunning = true
        val launched = tryLaunchSpecialPermission(next)
        if (!launched) {
            permFlowRunning = false
            continueSpecialPermissionsFlow()
        }
    }

    private fun tryLaunchSpecialPermission(key: String): Boolean {
        return try {
            when (key) {
                "exact_alarm" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
                    val am = getSystemService(AlarmManager::class.java)
                    if (am == null || am.canScheduleExactAlarms()) return false
                    val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                    true
                }
                "full_screen" -> {
                    if (Build.VERSION.SDK_INT < 34) return false
                    val nm = getSystemService(android.app.NotificationManager::class.java)
                    if (nm == null || nm.canUseFullScreenIntent()) return false
                    val i = Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT")
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                    true
                }
                "battery" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
                    val pm = getSystemService(android.os.PowerManager::class.java)
                    if (pm == null || pm.isIgnoringBatteryOptimizations(packageName)) return false
                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                    true
                }
                "overlay" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
                    if (Settings.canDrawOverlays(this)) return false
                    val i = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(i)
                    true
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun advanceAfterReturn() {
        permFlowRunning = false
        lifecycleScope.launch {
            kotlinx.coroutines.delay(300L)
            continueSpecialPermissionsFlow()
        }
    }
}

private data class TabItem(
    val title: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    alarmViewModel: AlarmViewModel,
    dailyQuestViewModel: DailyQuestViewModel
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        TabItem("Home", Icons.Rounded.Cottage),
        TabItem("Tasks", Icons.Rounded.ChecklistRtl),
        TabItem("Alarm", Icons.Rounded.Alarm),
        TabItem("Player", Icons.Rounded.MilitaryTech),
    )
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            taskViewModel.applyOverduePenalties()
            delay(10_000L)
        }
    }
    var showSettings by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var levelUpShown by remember { mutableStateOf<Int?>(null) }
    val blockPrefs = remember { BlockPrefs(context) }
    val criticalActive by blockPrefs.criticalActive.collectAsState(initial = false)
    val pStats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        SystemEvents.toasts.collectLatest { msg ->
            try {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (_: Throwable) {}
        }
    }
    val currentLevel = pStats?.level ?: 1
    androidx.compose.runtime.LaunchedEffect(pStats?.health, pStats?.ep, pStats?.streak) {
        try {
            val mgr = android.appwidget.AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(context, com.neurasamu.build.sl_tasker.widget.StatsWidgetProvider::class.java)
            )
            ids.forEach { id ->
                com.neurasamu.build.sl_tasker.widget.StatsWidgetProvider.updateWidget(context, mgr, id)
            }
        } catch (_: Throwable) {}
    }
    androidx.compose.runtime.LaunchedEffect(currentLevel) {
        if (currentLevel > 1 && levelUpShown != currentLevel) {
            levelUpShown = currentLevel
            SoundFx.levelUp()
            try {
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 60, 60, 120), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(longArrayOf(0, 60, 60, 60, 60, 120), -1)
                }
            } catch (_: Throwable) {}
        }
    }
    val playerStats by statsViewModel.playerStats.collectAsStateWithLifecycle()
    val locked = criticalActive || (playerStats?.health ?: 100) <= 30

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = tabs[selectedTabIndex].title,
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryManaBlue
                        )
                        LiveClock()
                    }
                },
                actions = {
                    if (selectedTabIndex == 3) {
                        IconButton(
                            onClick = {
                                if (locked) {
                                    val msg = if (criticalActive) "Critical task in progress" else "Health too low — recover first"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                } else {
                                    showSettings = true
                                }
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Block System Settings",
                                tint = if (locked) com.neurasamu.build.sl_tasker.ui.theme.TextMuted else PrimaryManaBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, CircleShape)
                            .clickable { showAiSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AiHubLogo(size = 34.dp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = PrimaryManaBlue,
                    actionIconContentColor = PrimaryManaBlue
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) DarkSurface else DarkCard)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryManaBlue else DarkCard,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) PrimaryManaBlue else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PrimaryManaBlue else TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    taskViewModel = taskViewModel,
                    statsViewModel = statsViewModel,
                    dailyQuestViewModel = dailyQuestViewModel
                )
                1 -> TasksScreen(
                    taskViewModel = taskViewModel,
                    statsViewModel = statsViewModel
                )
                2 -> AlarmScreen(
                    alarmViewModel = alarmViewModel
                )
                3 -> StatsScreen(
                    statsViewModel = statsViewModel
                )
            }
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showAiSheet) { showAiSheet = false }
    BackHandler(enabled = selectedTabIndex != 0 && !showSettings && !showAiSheet) {
        selectedTabIndex = 0
    }

    if (showSettings) {
        PlayerSettingsScreen(onClose = { showSettings = false })
    }

    levelUpShown?.let { lvl ->
        LevelUpOverlay(level = lvl, onDismiss = { levelUpShown = null })
    }

    if (showAiSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { showAiSheet = false },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(enabled = false) { }
            ) {
                AiSheet(onClose = { showAiSheet = false })
            }
        }
    }
}

@Composable
private fun LiveClock() {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val sdf = remember { java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()) }
    val sdfDate = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val now = java.util.Date(nowMillis)
    Text(
        text = "${sdf.format(now)}  |  ${sdfDate.format(now)}",
        style = MaterialTheme.typography.labelSmall,
        color = com.neurasamu.build.sl_tasker.ui.theme.TextMuted,
        fontSize = 10.sp
    )
}
