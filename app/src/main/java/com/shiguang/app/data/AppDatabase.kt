package com.shiguang.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shiguang.app.data.dao.CountdownDao
import com.shiguang.app.data.dao.HabitDao
import com.shiguang.app.data.dao.ScheduleDao
import com.shiguang.app.data.entity.CountdownEntity
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.data.entity.HabitRecordEntity
import com.shiguang.app.data.entity.ScheduleEntity

/**
 * 本地数据库（Room，完全离线）。
 * schema 导出到 app/schemas 目录用于版本迁移。
 */
@Database(
    entities = [
        CountdownEntity::class,
        HabitEntity::class,
        HabitRecordEntity::class,
        ScheduleEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao
    abstract fun habitDao(): HabitDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        private const val DB_NAME = "studymate.db"

        /** v1 -> v2：schedules 增加 note 列（BIT101 导入备注：教师 · 周次）。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN note TEXT DEFAULT NULL")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}