package com.neurasamu.build.solo_leveling_tasker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neurasamu.build.solo_leveling_tasker.ui.alarm.AlarmScreen
import com.neurasamu.build.solo_leveling_tasker.ui.dashboard.DashboardScreen
import com.neurasamu.build.solo_leveling_tasker.ui.stats.StatsScreen
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBackground
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkBorder
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkCard
import com.neurasamu.build.solo_leveling_tasker.ui.theme.DarkSurface
import com.neurasamu.build.solo_leveling_tasker.ui.theme.PrimaryManaBlue
import com.neurasamu.build.solo_leveling_tasker.ui.theme.SoloLevelingTheme
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextMuted
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextPrimary
import com.neurasamu.build.solo_leveling_tasker.ui.theme.TextSecondary
import com.neurasamu.build.solo_leveling_tasker.ui.viewmodel.AlarmViewModel
import com.neurasamu.build.solo_leveling_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.solo_leveling_tasker.ui.viewmodel.TaskViewModel

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
    }
}

@Composable
fun MainAppScaffold(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    alarmViewModel: AlarmViewModel
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("QUESTS", "ALARMS", "STATUS")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) DarkSurface else DarkCard)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryManaBlue else DarkCard,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) PrimaryManaBlue else TextMuted,
                                fontSize = 11.sp
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
                0 -> DashboardScreen(
                    taskViewModel = taskViewModel,
                    statsViewModel = statsViewModel
                )
                1 -> AlarmScreen(
                    alarmViewModel = alarmViewModel
                )
                2 -> StatsScreen(
                    statsViewModel = statsViewModel
                )
            }
        }
    }
}
