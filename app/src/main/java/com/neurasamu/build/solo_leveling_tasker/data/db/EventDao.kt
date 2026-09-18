package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neurasamu.build.solo_leveling_tasker.data.model.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Query("SELECT * FROM events WHERE occurrenceId = :occurrenceId ORDER BY timestamp ASC")
    fun getEventsForOccurrence(occurrenceId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE taskId = :taskId ORDER BY timestamp ASC")
    fun getEventsForTask(taskId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>
}
