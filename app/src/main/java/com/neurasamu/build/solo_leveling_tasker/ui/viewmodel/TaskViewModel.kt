package com.neurasamu.build.solo_leveling_tasker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.solo_leveling_tasker.data.db.AppDatabase
import com.neurasamu.build.solo_leveling_tasker.data.model.Difficulty
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceStatus
import com.neurasamu.build.solo_leveling_tasker.data.model.Priority
import com.neurasamu.build.solo_leveling_tasker.data.model.RepeatRule
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskEntity
import com.neurasamu.build.solo_leveling_tasker.data.repository.TaskRepository
import com.neurasamu.build.solo_leveling_tasker.domain.scheduler.AlarmScheduler
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

    fun createQuest(
        title: String,
        description: String,
        priority: Priority,
        difficulty: Difficulty,
        repeatRule: RepeatRule,
        customRepeatDays: String,
        durationMinutes: Int,
        scheduledAt: Long,
        deadlineAt: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = TaskEntity(
                title = title,
                description = description,
                priority = priority,
                difficulty = difficulty,
                repeatRule = repeatRule,
                customRepeatDays = customRepeatDays,
                durationMinutes = durationMinutes
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
