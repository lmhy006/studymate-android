package com.shiguang.app.data.repo

import com.shiguang.app.data.dao.CountdownDao
import com.shiguang.app.data.entity.CountdownEntity
import kotlinx.coroutines.flow.Flow

class CountdownRepository(private val dao: CountdownDao) {

    fun observeAll(): Flow<List<CountdownEntity>> = dao.observeAll()

    suspend fun widgetCandidate(): CountdownEntity? = dao.widgetCandidate()

    suspend fun getById(id: Long): CountdownEntity? = dao.getById(id)

    /** 新增或更新，返回 id。 */
    suspend fun save(entity: CountdownEntity): Long =
        if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity)
            entity.id
        }

    suspend fun delete(entity: CountdownEntity) = dao.delete(entity)

    /** 置顶某个倒数日到桌面小组件（全局唯一）。 */
    suspend fun setPinned(id: Long) {
        dao.clearPinned()
        dao.getById(id)?.let { dao.update(it.copy(pinned = true)) }
    }

    suspend fun clearPinned() = dao.clearPinned()
}