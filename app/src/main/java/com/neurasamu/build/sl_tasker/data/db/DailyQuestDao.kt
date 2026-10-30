package com.neurasamu.build.sl_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neurasamu.build.sl_tasker.data.model.DailyQuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyQuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(q: DailyQuestEntity): Long

    @Update
    suspend fun update(q: DailyQuestEntity)

    @Query("SELECT * FROM daily_quests WHERE dayEpoch = :dayEpoch ORDER BY id ASC")
    fun getForDay(dayEpoch: Long): Flow<List<DailyQuestEntity>>

    @Query("SELECT * FROM daily_quests WHERE dayEpoch = :dayEpoch ORDER BY id ASC")
    suspend fun getForDayList(dayEpoch: Long): List<DailyQuestEntity>

    @Query("SELECT COUNT(*) FROM daily_quests WHERE dayEpoch = :dayEpoch")
    suspend fun countForDay(dayEpoch: Long): Int

    @Query("SELECT * FROM daily_quests WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DailyQuestEntity?

    @Query("DELETE FROM daily_quests WHERE dayEpoch < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
