package com.shiguang.app.data.repo

import com.shiguang.app.data.dao.DdlDao
import com.shiguang.app.data.entity.DdlEntity
import kotlinx.coroutines.flow.Flow

class DdlRepository(private val dao: DdlDao) {

    fun observeAll(): Flow<List<DdlEntity>> = dao.observeAll()

    suspend fun save(entity: DdlEntity): Long =
        if (entity.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            entity.id
        }

    suspend fun delete(entity: DdlEntity) = dao.delete(entity)

    /** 勾选完成（completedAt=现在）或撤销完成。 */
    suspend fun setCompleted(id: Long, completed: Boolean) {
        dao.setCompleted(id, completed, if (completed) System.currentTimeMillis() else null)
    }

    /** 自动释放：删除 已完成 且 完成时间不晚于 cutoff（= now - releaseDays 天）。 */
    suspend fun releaseCompletedBefore(cutoffMillis: Long) = dao.deleteReleased(cutoffMillis)
}