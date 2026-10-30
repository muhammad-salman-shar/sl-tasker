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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TaskOccurrenceItem(
    val task: TaskEntity,
    val occurrence: OccurrenceEntity
)

class TaskViewModel(
    application: Application,
    private val taskRepository: TaskRepository,
    private val alarmScheduler: AlarmScheduler
) : AndroidViewModel(application) {

    val completedQuests: StateFlow<List<TaskOccurrenceItem>> = combine(
        taskRepository.observeOccurrencesByStatus(listOf(OccurrenceStatus.COMPLETED)),
        taskRepository.getAllTasks()
    ) { occurrences, tasks ->
        val taskMap = tasks.associateBy { it.id }
        occurrences.mapNotNull { occurrence ->
            val task = taskMap[occurrence.taskId] ?: return@mapNotNull null
            TaskOccurrenceItem(task = task, occurrence = occurrence)
        }.sortedByDescending { it.occurrence.completedAt }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allOccurrences: StateFlow<List<com.neurasamu.build.sl_tasker.data.model.OccurrenceEntity>> =
        taskRepository.observeAllOccurrences()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val allActiveTasks: StateFlow<List<com.neurasamu.build.sl_tasker.data.model.TaskEntity>> =
        taskRepository.getAllActiveTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

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
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun applyOverduePenalties() {
        viewModelScope.launch(Dispatchers.IO) {
            taskRepository.applyOverduePenalties(System.currentTimeMillis())
        }
    }

    fun completeQuest(occurrenceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = taskRepository.completeOccurrence(occurrenceId)
                if (!success) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(getApplication(), "Task could not be completed right now.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(getApplication(), "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
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
        durationMinutes: Int,
        isRepeat: Boolean,
        allowEarlyComplete: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = taskRepository.getTaskById(taskId) ?: return@launch

            val oldOccs = taskRepository.getPendingOccurrencesForTask(taskId)
            oldOccs.forEach { occ ->
                alarmScheduler.cancelReminder(occ.id)
                taskRepository.deleteOccurrence(occ.id)
            }

            val repeatRuleVal = if (isRepeat) RepeatRule.CUSTOM else RepeatRule.ONCE

            val updated = existing.copy(
                title = title,
                description = description,
                difficulty = difficulty,
                reminderMinutesOfDay = reminderMinutesOfDay,
                customRepeatDays = customRepeatDays,
                durationMinutes = durationMinutes,
                repeatRule = repeatRuleVal,
                allowEarlyComplete = allowEarlyComplete
            )
            taskRepository.updateTaskFields(updated)

            val nextTrigger = com.neurasamu.build.sl_tasker.domain.scheduler.ScheduleHelper.nextTrigger(
                hour = reminderMinutesOfDay / 60,
                minute = reminderMinutesOfDay % 60,
                daysCsv = customRepeatDays,
                repeat = repeatRuleVal
            ) ?: (System.currentTimeMillis() + 60_000L)
            val deadlineAt = nextTrigger + durationMinutes * 60 * 1000L

            val (_, occId) = taskRepository.createTaskWithOccurrence(updated, nextTrigger, deadlineAt)
            alarmScheduler.scheduleReminder(occId, nextTrigger, title)
        }
    }

    fun createQuest(
        title: String,
        description: String,
        difficulty: Difficulty,
        reminderMinutesOfDay: Int,
        customRepeatDays: String,
        durationMinutes: Int,
        isRepeat: Boolean,
        allowEarlyComplete: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // IMPORTANT: isRepeat is user-intent, not "did they pick days".
            // A single-day task with a specific day is still ONCE (no weekly loop).
            val repeatRuleVal = if (isRepeat) RepeatRule.CUSTOM else RepeatRule.ONCE
            val nextTrigger = com.neurasamu.build.sl_tasker.domain.scheduler.ScheduleHelper.nextTrigger(
                hour = reminderMinutesOfDay / 60,
                minute = reminderMinutesOfDay % 60,
                daysCsv = customRepeatDays,
                repeat = repeatRuleVal
            ) ?: (System.currentTimeMillis() + 60_000L)
            val scheduledAt = nextTrigger
            val deadlineAt = scheduledAt + durationMinutes * 60 * 1000L

            val task = TaskEntity(
                title = title,
                description = description,
                priority = Priority.MEDIUM,
                difficulty = difficulty,
                repeatRule = repeatRuleVal,
                customRepeatDays = customRepeatDays,
                durationMinutes = durationMinutes,
                reminderMinutesOfDay = reminderMinutesOfDay,
                allowEarlyComplete = allowEarlyComplete
            )
            val (_, occurrenceId) = taskRepository.createTaskWithOccurrence(task, scheduledAt, deadlineAt)
            alarmScheduler.scheduleReminder(occurrenceId, scheduledAt, title)
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
