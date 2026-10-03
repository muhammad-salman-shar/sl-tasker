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

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels { TaskViewModel.Factory(application) }
    private val statsViewModel: StatsViewModel by viewModels { StatsViewModel.Factory(application) }
    private val alarmViewModel: AlarmViewModel by viewModels { AlarmViewModel.Factory(application) }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            SoloLevelingTheme {
                MainAppScaffold(
                    taskViewModel = taskViewModel,
                    statsViewModel = statsViewModel,
                    alarmViewModel = alarmViewModel
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms()) {
                try {
                    val i = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    i.data = Uri.parse("package:$packageName")
                    startActivity(i)
                } catch (_: Exception) {
                }
            }
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
    alarmViewModel: AlarmViewModel
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
    val blockPrefs = remember { BlockPrefs(context) }
    val criticalActive by blockPrefs.criticalActive.collectAsState(initial = false)
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
                            .clickable {
                                Toast.makeText(context, "AI Hub coming soon", Toast.LENGTH_SHORT).show()
                            },
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
                    statsViewModel = statsViewModel
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

    if (showSettings) {
        PlayerSettingsScreen(onClose = { showSettings = false })
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
