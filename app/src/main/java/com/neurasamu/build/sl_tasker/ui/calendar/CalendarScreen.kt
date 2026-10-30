package com.neurasamu.build.sl_tasker.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.sl_tasker.ui.theme.AccentGold
import com.neurasamu.build.sl_tasker.ui.theme.DarkBackground
import com.neurasamu.build.sl_tasker.ui.theme.DarkBorder
import com.neurasamu.build.sl_tasker.ui.theme.DarkCard
import com.neurasamu.build.sl_tasker.ui.theme.DarkSurface
import com.neurasamu.build.sl_tasker.ui.theme.DangerPenaltyRed
import com.neurasamu.build.sl_tasker.ui.theme.PrimaryBlue
import com.neurasamu.build.sl_tasker.ui.theme.SuccessGreen
import com.neurasamu.build.sl_tasker.ui.theme.TextMuted
import com.neurasamu.build.sl_tasker.ui.theme.TextPrimary
import com.neurasamu.build.sl_tasker.ui.viewmodel.TaskViewModel
import java.util.Calendar

@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allOccurrences by taskViewModel.allOccurrences.collectAsStateWithLifecycle()
    val tasks by taskViewModel.allActiveTasks.collectAsStateWithLifecycle()

    val taskMap = remember(tasks) { tasks.associateBy { it.id } }

    var monthOffset by remember { mutableIntStateOf(0) }
    val selectedCal = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
    }
    val year = selectedCal.get(Calendar.YEAR)
    val month = selectedCal.get(Calendar.MONTH)

    var selectedDay by remember { mutableIntStateOf(-1) }

    // Group occurrences by day-of-month for selected month
    val byDay = remember(allOccurrences, year, month) {
        allOccurrences.groupBy { occ ->
            val c = Calendar.getInstance().apply { timeInMillis = occ.scheduledAt }
            if (c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month) {
                c.get(Calendar.DAY_OF_MONTH)
            } else -1
        }.filterKeys { it > 0 }
    }

    val firstDayOfWeek = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    }
    val daysInMonth = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val monthNames = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    val dayHeaders = listOf("S","M","T","W","T","F","S")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header with month nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { monthOffset -= 1; selectedDay = -1 }) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Prev", tint = PrimaryBlue)
            }
            Text(
                "${monthNames[month]} $year",
                color = PrimaryBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { monthOffset += 1; selectedDay = -1 }) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next", tint = PrimaryBlue)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Day headers
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { h ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(h, color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Grid
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        Column {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (c in 0..6) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - firstDayOfWeek + 1
                        val valid = dayNum in 1..daysInMonth
                        val itemsOnDay = if (valid) byDay[dayNum] ?: emptyList() else emptyList()
                        val isSelected = valid && dayNum == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> PrimaryBlue.copy(alpha = 0.2f)
                                        valid -> DarkSurface.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) PrimaryBlue else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = valid) {
                                    selectedDay = if (selectedDay == dayNum) -1 else dayNum
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (valid) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$dayNum",
                                        color = if (isSelected) PrimaryBlue else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (itemsOnDay.isNotEmpty()) {
                                        Spacer(Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            itemsOnDay.take(3).forEach { occ ->
                                                val isDone = occ.status.name == "COMPLETED"
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isDone) SuccessGreen
                                                            else if (occ.status.name == "MISSED") DangerPenaltyRed
                                                            else AccentGold
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "TASKS ON ${if (selectedDay > 0) "${monthNames[month]} $selectedDay" else "MONTH"}",
            color = AccentGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(8.dp))

        val dayTasks: List<Pair<OccurrenceEntity, com.neurasamu.build.sl_tasker.data.model.TaskEntity>> =
            if (selectedDay > 0) {
                (byDay[selectedDay] ?: emptyList()).mapNotNull { occ ->
                    taskMap[occ.taskId]?.let { occ to it }
                }.sortedBy { it.0.scheduledAt }
            } else {
                emptyList()
            }

        if (dayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (selectedDay > 0) "No tasks on this day." else "Select a day to see tasks.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(dayTasks) { (occ, task) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDone = occ.status.name == "COMPLETED"
                        val isMissed = occ.status.name == "MISSED"
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> SuccessGreen
                                        isMissed -> DangerPenaltyRed
                                        else -> AccentGold
                                    }
                                )
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val c = Calendar.getInstance().apply { timeInMillis = occ.scheduledAt }
                            val hh = c.get(Calendar.HOUR_OF_DAY)
                            val mm = c.get(Calendar.MINUTE)
                            val timeStr = String.format("%02d:%02d", hh, mm)
                            val statusStr = when {
                                isDone -> "DONE"
                                isMissed -> "MISSED"
                                else -> "PENDING"
                            }
                            Text(
                                "$timeStr - ${task.difficulty.name} - $statusStr",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
