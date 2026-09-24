package com.shiguang.app.data.repo

import com.shiguang.app.data.dao.ScheduleDao
import com.shiguang.app.data.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val dao: ScheduleDao) {

    fun observeAll(): Flow<List<ScheduleEntity>> = dao.observeAll()

    suspend fun save(entity: ScheduleEntity): Long =
        if (entity.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            entity.id
        }

    suspend fun delete(entity: ScheduleEntity) = dao.delete(entity)
}