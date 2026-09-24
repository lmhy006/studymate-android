package com.shiguang.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shiguang.app.data.entity.CountdownEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CountdownDao {

    /** 全部倒数日，按目标日升序。 */
    @Query("SELECT * FROM countdowns ORDER BY targetEpochDay ASC")
    fun observeAll(): Flow<List<CountdownEntity>>

    /** 桌面小组件候选：置顶优先，其次最近的目标日。 */
    @Query("SELECT * FROM countdowns ORDER BY pinned DESC, targetEpochDay ASC LIMIT 1")
    suspend fun widgetCandidate(): CountdownEntity?

    @Query("SELECT * FROM countdowns WHERE id = :id")
    suspend fun getById(id: Long): CountdownEntity?

    @Insert
    suspend fun insert(entity: CountdownEntity): Long

    @Update
    suspend fun update(entity: CountdownEntity)

    @Delete
    suspend fun delete(entity: CountdownEntity)

    @Query("UPDATE countdowns SET pinned = 0")
    suspend fun clearPinned()
}