package com.shiguang.app.data.repo

import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.dao.HabitDao
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.data.entity.HabitRecordEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val dao: HabitDao) {

    fun observeHabits(): Flow<List<HabitEntity>> = dao.observeHabits()

    fun observeAllRecords(): Flow<List<HabitRecordEntity>> = dao.observeAllRecords()

    /** 新增习惯，周期锚点默认从今天开始；mode = 周期/自由。 */
    suspend fun addHabit(name: String, emoji: String, intervalDays: Int, mode: Int): Long =
        dao.insert(
            HabitEntity(
                name = name,
                emoji = emoji,
                intervalDays = intervalDays.coerceAtLeast(1),
                startEpochDay = DateUtils.today().toEpochDay(),
                mode = mode,
            )
        )

    suspend fun deleteHabit(habit: HabitEntity) = dao.delete(habit)

    suspend fun checkIn(habitId: Long, epochDay: Long) =
        dao.insertRecord(HabitRecordEntity(habitId = habitId, epochDay = epochDay))

    suspend fun undoCheckIn(habitId: Long, epochDay: Long) = dao.deleteRecord(habitId, epochDay)
}