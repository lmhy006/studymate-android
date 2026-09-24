package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 打卡记录：一个周期日最多一条（主键即 习惯+日期）。
 * 删除习惯时级联删除其打卡记录。
 */
@Entity(
    tableName = "habit_records",
    primaryKeys = ["habitId", "epochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("habitId")],
)
data class HabitRecordEntity(
    val habitId: Long,
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)