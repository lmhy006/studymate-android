package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 倒数日。
 * targetEpochDay：目标日的 epochDay（LocalDate.toEpochDay()）。
 * pinned：是否在桌面小组件上展示（全局仅允许一个置顶）。
 */
@Entity(tableName = "countdowns")
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetEpochDay: Long,
    val note: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)