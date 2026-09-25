package com.shiguang.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shiguang.app.data.dao.CountdownDao
import com.shiguang.app.data.dao.DdlDao
import com.shiguang.app.data.dao.HabitDao
import com.shiguang.app.data.dao.ScheduleDao
import com.shiguang.app.data.entity.CountdownEntity
import com.shiguang.app.data.entity.DdlEntity
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
        DdlEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countdownDao(): CountdownDao
    abstract fun habitDao(): HabitDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun ddlDao(): DdlDao

    companion object {
        private const val DB_NAME = "studymate.db"

        /** v1 -> v2：schedules 增加 note 列（BIT101 导入备注：教师 · 周次）。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN note TEXT DEFAULT NULL")
            }
        }

        /** v2 -> v3：habits 增加 mode 列（周期/自由两种打卡模式），默认周期模式。 */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN mode INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v3 -> v4：新建 ddls 表（课程作业截止）。 */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ddls` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`courseName` TEXT, " +
                        "`dueAt` INTEGER NOT NULL, " +
                        "`submitMethod` TEXT, " +
                        "`note` TEXT, " +
                        "`completed` INTEGER NOT NULL DEFAULT 0, " +
                        "`completedAt` INTEGER)"
                )
            }
        }

        /** v4 -> v5：schedules 增加 weekParity 列（单双周），默认每周。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN weekParity INTEGER NOT NULL DEFAULT 0")
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { instance = it }
            }
    }
}