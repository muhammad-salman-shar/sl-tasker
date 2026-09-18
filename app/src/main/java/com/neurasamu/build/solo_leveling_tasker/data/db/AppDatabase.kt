package com.neurasamu.build.solo_leveling_tasker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neurasamu.build.solo_leveling_tasker.data.model.AlarmEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.EventEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.OccurrenceEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.PlayerStatsEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.RecoveryQuestEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskDebtEntity
import com.neurasamu.build.solo_leveling_tasker.data.model.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        OccurrenceEntity::class,
        EventEntity::class,
        TaskDebtEntity::class,
        RecoveryQuestEntity::class,
        PlayerStatsEntity::class,
        AlarmEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun occurrenceDao(): OccurrenceDao
    abstract fun eventDao(): EventDao
    abstract fun taskDebtDao(): TaskDebtDao
    abstract fun recoveryQuestDao(): RecoveryQuestDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sl_tasker_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
