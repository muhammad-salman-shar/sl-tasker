package com.neurasamu.build.sl_tasker.data.repository

import androidx.room.withTransaction
import com.neurasamu.build.sl_tasker.data.db.AppDatabase
import com.neurasamu.build.sl_tasker.data.model.DailyQuestEntity
import com.neurasamu.build.sl_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.sl_tasker.domain.engine.DailyQuestGenerator
import com.neurasamu.build.sl_tasker.domain.engine.GamificationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DailyQuestRepository(private val db: AppDatabase) {

    private val dao = db.dailyQuestDao()
    private val statsDao = db.playerStatsDao()

    fun getToday(): Flow<List<DailyQuestEntity>> =
        dao.getForDay(DailyQuestGenerator.startOfToday())

    suspend fun ensureTodayExists() = withContext(Dispatchers.IO) {
        val today = DailyQuestGenerator.startOfToday()
        val count = dao.countForDay(today)
        if (count == 0) {
            val generated = DailyQuestGenerator.generateForToday(today, 3)
            generated.forEach { dao.insert(it) }
        }
        // Cleanup old (keep 7 days history)
        val cutoff = today - 7L * 24L * 60L * 60L * 1000L
        dao.deleteOlderThan(cutoff)
    }

    suspend fun completeQuest(id: Long): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val quest = dao.getById(id) ?: return@withTransaction false
            if (quest.completed) return@withTransaction false

            dao.update(quest.copy(completed = true, completedAt = System.currentTimeMillis()))

            val stats = statsDao.getPlayerStats() ?: PlayerStatsEntity()

            val rawEp = stats.ep + quest.bonusEp
            val extraLevels = rawEp / GamificationEngine.EP_PER_LEVEL
            val newEp = rawEp % GamificationEngine.EP_PER_LEVEL
            val newLevel = stats.level + extraLevels
            val newHp = (stats.health + quest.bonusHp).coerceAtMost(GamificationEngine.MAX_HEALTH)

            // Check all 3 quests done today → perfect day bonus
            val today = DailyQuestGenerator.startOfToday()
            val all = dao.getForDayList(today)
            val allDone = all.all { it.completed }

            var finalEp = newEp
            var finalLevel = newLevel
            var finalHp = newHp

            if (allDone && all.size >= 3) {
                val bonus = 20
                val raw = finalEp + bonus
                finalLevel += raw / GamificationEngine.EP_PER_LEVEL
                finalEp = raw % GamificationEngine.EP_PER_LEVEL
                finalHp = (finalHp + 10).coerceAtMost(GamificationEngine.MAX_HEALTH)
            }

            val updated = stats.copy(
                ep = finalEp,
                level = finalLevel,
                health = finalHp,
                recoveryModeActive = if (finalHp >= 50) false else stats.recoveryModeActive
            )
            statsDao.insertOrUpdate(updated)

            com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                "✅ Daily: +${quest.bonusEp} EP, +${quest.bonusHp} HP"
            )
            if (allDone && all.size >= 3) {
                com.neurasamu.build.sl_tasker.util.SystemEvents.toast(
                    "🏆 PERFECT DAY! +20 EP, +10 HP"
                )
            }
            true
        }
    }
}
