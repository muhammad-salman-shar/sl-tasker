package com.neurasamu.build.sl_tasker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.Difficulty
import com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.sl_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.sl_tasker.data.model.Priority
import com.neurasamu.build.sl_tasker.data.model.RepeatRule
import com.neurasamu.build.sl_tasker.data.model.TaskEntity
import com.neurasamu.build.sl_tasker.data.repository.TaskRepository
import com.neurasamu.build.sl_tasker.domain.scheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskOccurrenceItem(
    val task: TaskEntity,
    val occurrence: OccurrenceEntity
)

class TaskViewModel(
    application: Application,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : AndroidViewModel(application) {

    val activeQuests: StateFlow<List<TaskOccurrenceItem>> = combine(
        taskRepository.observeOccurrencesByStatus(listOf(OccurrenceStatus.PENDING)),
        taskRepository.getAllActiveTasks()
    ) { occurrences, tasks ->
        val taskMap = tasks.associateBy { it.id }
        occurrences.mapNotNull { occurrence ->
            val task = taskMap[occurrence.taskId]
            if (task != null) {
                TaskOccurrenceItem(task = task, occurrence = occurrence)
            } else {
                null
            }
        }.sortedBy { it.occurrence.deadlineAt }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun completeQuest(occurrenceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.completeOccurrence(occurrenceId)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.deleteTask(taskId)
        }
    }

    fun updateTaskFields(
        taskId: Long,
        title: String,
        description: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        customRepeatDays: String,
        durationMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = taskRepository.getTaskById(taskId) ?: return@launch
            val updated = existing.copy(
                title = title,
                description = description,
                difficulty = difficulty,
                reminderMinutesOfDay = reminderMinutesOfDay,
                customRepeatDays = customRepeatDays,
                durationMinutes = durationMinutes
            )
            taskRepository.updateTaskFields(updated)
        }
    }

    fun createQuest(
        title: String,
        description: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        customRepeatDays: String,
        durationMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, reminderMinutesOfDay / 60)
            cal.set(java.util.Calendar.MINUTE, reminderMinutesOfDay % 60)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            var scheduledAt = cal.timeInMillis
            if (scheduledAt <= System.currentTimeMillis()) {
                scheduledAt += 24L * 60 * 60 * 1000
            }
            val deadlineAt = scheduledAt + durationMinutes * 60 * 1000L

            val task = TaskEntity(
                title = title,
                description = description,
                priority = Priority.MEDIUM,
                difficulty = difficulty,
                repeatRule = RepeatRule.CUSTOM,
                customRepeatDays = customRepeatDays,
                durationMinutes = durationMinutes,
                reminderMinutesOfDay = reminderMinutesOfDay
            )
            val taskId = taskRepository.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
            if (scheduledAt > System.currentTimeMillis()) {
                alarmScheduler.scheduleReminder(taskId, scheduledAt, title)
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(application)
            val repo = TaskRepository(db)
            val scheduler = AlarmScheduler(application)
            return TaskViewModel(application, repo, scheduler) as T
        }
    }
}
