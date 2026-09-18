package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryQuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecoveryQuest(quest: RecoveryQuestEntity): Long

    @Update
    suspend fun updateRecoveryQuest(quest: RecoveryQuestEntity)

    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getActiveRecoveryQuests(): Flow<List<RecoveryQuestEntity>>

    @Query("SELECT * FROM recovery_quests WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    suspend fun getActiveRecoveryQuestsList(): List<RecoveryQuestEntity>

    @Query("SELECT * FROM recovery_quests WHERE id = :id LIMIT 1")
    suspend fun getRecoveryQuestById(id: Long): RecoveryQuestEntity?

    @Query("SELECT * FROM recovery_quests WHERE originOccurrenceId = :occurrenceId LIMIT 1")
    suspend fun getRecoveryQuestForOccurrence(occurrenceId: Long): RecoveryQuestEntity?

    @Query("SELECT COUNT(*) FROM recovery_quests WHERE status = 'ACTIVE'")
    fun getActiveRecoveryCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recovery_quests WHERE status = 'COMPLETED'")
    fun getTotalRecoveredCount(): Flow<Int>
}
