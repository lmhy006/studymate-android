package com.shiguang.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.data.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit_records")
    fun observeAllRecords(): Flow<List<HabitRecordEntity>>

    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Delete
    suspend fun delete(habit: HabitEntity)

    /** 打卡：同一习惯同一周期日重复打卡直接忽略。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: HabitRecordEntity)

    /** 撤销打卡。 */
    @Query("DELETE FROM habit_records WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteRecord(habitId: Long, epochDay: Long)
}