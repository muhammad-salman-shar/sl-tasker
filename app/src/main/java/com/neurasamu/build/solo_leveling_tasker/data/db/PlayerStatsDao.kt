package com.neurasamu.build.solo_leveling_tasker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: PlayerStatsEntity)

    @Update
    suspend fun updateStats(stats: PlayerStatsEntity)

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun observePlayerStats(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun getPlayerStats(): PlayerStatsEntity?

    @Query("UPDATE player_stats SET health = :health WHERE id = 1")
    suspend fun updateHealth(health: Int)

    @Query("UPDATE player_stats SET ep = :ep WHERE id = 1")
    suspend fun updateEp(ep: Int)

    @Query("UPDATE player_stats SET level = :level WHERE id = 1")
    suspend fun updateLevel(level: Int)

    @Query("UPDATE player_stats SET criticalActiveOccurrenceId = :occurrenceId WHERE id = 1")
    suspend fun setCriticalActiveOccurrenceId(occurrenceId: Long?)

    @Query("UPDATE player_stats SET focusLockActive = :active WHERE id = 1")
    suspend fun setFocusLock(active: Boolean)

    @Query("UPDATE player_stats SET recoveryModeActive = :active WHERE id = 1")
    suspend fun setRecoveryMode(active: Boolean)
}
