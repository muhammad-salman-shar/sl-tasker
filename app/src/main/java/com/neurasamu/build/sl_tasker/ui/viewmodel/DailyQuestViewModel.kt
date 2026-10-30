package com.neurasamu.build.sl_tasker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.DailyQuestEntity
import com.neurasamu.build.sl_tasker.data.repository.DailyQuestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailyQuestViewModel(
    application: Application,
    private val repo: DailyQuestRepository
) : AndroidViewModel(application) {

    val quests: StateFlow<List<DailyQuestEntity>> = repo.getToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) { repo.ensureTodayExists() }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) { repo.ensureTodayExists() }
    }

    fun complete(id: Long) {
        viewModelScope.launch(Dispatchers.IO) { repo.completeQuest(id) }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(app)
            return DailyQuestViewModel(app, DailyQuestRepository(db)) as T
        }
    }
}
