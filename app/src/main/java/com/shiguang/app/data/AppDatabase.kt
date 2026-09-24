package com.shiguang.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.shiguang.app.data.dao.CountdownDao
import com.shiguang.app.data.dao.HabitDao
import com.shiguang.app.data.dao.ScheduleDao
import com.shiguang.app.data.entity.CountdownEntity
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.data.entity.HabitRecordEntity
import com.shiguang.app.data.entity.ScheduleEntity

/**
 * 本地数据库（Room，完全离线）。
 * schema 导出到 app/schemas 目录用于后续版本迁移。
 */
@Database(
    entities = [
        CountdownEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        ScheduleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao
    abstract fun habitDao(): HabitDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        private const val DB_NAME = "studymate.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).build().also { instance = it }
            }
    }
}