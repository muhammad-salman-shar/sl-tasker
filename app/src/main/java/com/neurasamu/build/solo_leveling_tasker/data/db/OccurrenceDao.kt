package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrence(occurrence: OccurrenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOccurrences(occurrences: List<OccurrenceEntity>): List<Long>

    @Update
    suspend fun updateOccurrence(occurrence: OccurrenceEntity)

    @Query("SELECT * FROM occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrenceById(id: Long): OccurrenceEntity?

    @Query("SELECT * FROM occurrences WHERE taskId = :taskId ORDER BY scheduledAt ASC")
    fun getOccurrencesForTask(taskId: Long): Flow<List<OccurrenceEntity>>

    @Query("SELECT * FROM occurrences WHERE status IN (:statuses) ORDER BY scheduledAt ASC")
    fun getOccurrencesByStatus(statuses: List<OccurrenceStatus>): Flow<List<OccurrenceEntity>>

    @Query("SELECT * FROM occurrences WHERE status = 'PENDING' AND deadlineAt < :currentTimeMillis")
    suspend fun getOverdueOccurrences(currentTimeMillis: Long): List<OccurrenceEntity>

    @Query("SELECT * FROM occurrences WHERE id = :id")
    fun observeOccurrenceById(id: Long): Flow<OccurrenceEntity?>

    @Query("SELECT * FROM occurrences ORDER BY scheduledAt DESC")
    fun getAllOccurrences(): Flow<List<OccurrenceEntity>>
}
