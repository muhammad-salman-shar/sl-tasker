package com.neurasamu.build.sl_tasker.ui.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neurasamu.build.sl_tasker.ui.dashboard.DashboardScreen
import com.neurasamu.build.sl_tasker.ui.viewmodel.StatsViewModel
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel

@Composable
fun TasksScreen(
    taskViewModel: TaskViewModel,
    statsViewModel: StatsViewModel,
    modifier: Modifier = Modifier
) {
    DashboardScreen(
        taskViewModel = taskViewModel,
        statsViewModel = statsViewModel,
        modifier = modifier
    )
}
