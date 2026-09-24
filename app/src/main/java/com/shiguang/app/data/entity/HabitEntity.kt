package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯。
 * intervalDays：周期天数。1=每日，7=每周，N=自定义每 N 天一次。
 * startEpochDay：周期锚点（通常是创建那天），第 k 个周期日为 start + k*intervalDays。
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val intervalDays: Int = 1,
    val startEpochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
)