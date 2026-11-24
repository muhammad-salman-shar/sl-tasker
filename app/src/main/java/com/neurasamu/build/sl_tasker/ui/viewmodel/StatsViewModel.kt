package com.neurasamu.build.sl_tasker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.EventEntity
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.sl_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.sl_tasker.data.repository.StatsRepository
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

class StatsViewModel(
    application: Application,
    private val statsRepository: StatsRepository
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            statsRepository.initializeStatsIfEmpty()
        }
    }

    val playerStats: StateFlow<PlayerStatsEntity> = statsRepository.playerStats
        .map { it ?: PlayerStatsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStatsEntity()
        )

    val recentEvents: StateFlow<List<EventEntity>> = statsRepository.getRecentEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val weeklyEvents: StateFlow<List<EventEntity>> = statsRepository.getWeeklyEvents()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recoveryQuests: StateFlow<List<RecoveryQuestEntity>> = statsRepository.getActiveRecoveryQuests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unresolvedDebts: StateFlow<List<TaskDebtEntity>> = statsRepository.getUnresolvedDebts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun completeRecovery(questId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            statsRepository.completeRecoveryQuest(questId)
        }
    }

    fun awardBonus(hp: Int, ep: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(getApplication())
                val dao = db.playerStatsDao()
                val cur = dao.getPlayerStats() ?: PlayerStatsEntity()
                val newHp = min(GamificationEngine.MAX_HEALTH, cur.health + hp.coerceAtLeast(0))
                val totalEp = cur.ep + ep.coerceAtLeast(0)
                val newLevel = (totalEp / GamificationEngine.EP_PER_LEVEL) + 1
                dao.insertOrUpdate(
                    cur.copy(
                        health = newHp,
                        ep = totalEp,
                        level = maxOf(cur.level, newLevel),
                        recoveryModeActive = if (newHp >= 50) false else cur.recoveryModeActive
                    )
                )
            } catch (_: Throwable) {}
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = AppDatabase.getInstance(application)
            val repo = StatsRepository(db)
            return StatsViewModel(application, repo) as T
        }
    }
}
