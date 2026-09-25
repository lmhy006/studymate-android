package com.shiguang.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shiguang.app.data.entity.DdlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DdlDao {

    /** 未完成在前（按截止时间升序），完成在后。 */
    @Query("SELECT * FROM ddls ORDER BY completed ASC, dueAt ASC, id DESC")
    fun observeAll(): Flow<List<DdlEntity>>

    @Insert
    suspend fun insert(entity: DdlEntity): Long

    @Update
    suspend fun update(entity: DdlEntity)

    @Delete
    suspend fun delete(entity: DdlEntity)

    @Query("UPDATE ddls SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    /** 释放：删除 已完成 且 完成时间不晚于 cutoff 的记录。 */
    @Query("DELETE FROM ddls WHERE completed = 1 AND completedAt IS NOT NULL AND completedAt <= :cutoff")
    suspend fun deleteReleased(cutoff: Long)
}