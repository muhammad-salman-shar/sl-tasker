package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDebtDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: TaskDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: TaskDebtEntity)

    @Query("SELECT * FROM task_debt WHERE resolved = 0 ORDER BY debtCreatedAt ASC")
    fun getUnresolvedDebts(): Flow<List<TaskDebtEntity>>

    @Query("SELECT * FROM task_debt WHERE resolved = 0 ORDER BY debtCreatedAt ASC")
    suspend fun getUnresolvedDebtsList(): List<TaskDebtEntity>

    @Query("SELECT * FROM task_debt WHERE occurrenceId = :occurrenceId LIMIT 1")
    suspend fun getDebtForOccurrence(occurrenceId: Long): TaskDebtEntity?

    @Query("UPDATE task_debt SET resolved = 1, resolvedAt = :resolvedAt WHERE occurrenceId = :occurrenceId")
    suspend fun resolveDebt(occurrenceId: Long, resolvedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM task_debt WHERE resolved = 0")
    fun getUnresolvedDebtCount(): Flow<Int>
}
