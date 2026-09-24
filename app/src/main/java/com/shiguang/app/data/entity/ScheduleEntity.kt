package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 日程。两种形态：
 *
 * 1. 单次日程：singleEpochDay != null；
 * 2. 批量周期日程（例：“3月1日~6月30日之间每个周一上午 8:00~9:50”）：
 *    repeatStartEpochDay..repeatEndEpochDay 为生效区间，weekdaysMask 为星期掩码
 *    （bit0=周一 … bit6=周日）。
 *
 * 时间为当日分钟数（0..1439），startMinute/endMinute 均非空。
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String? = null,
    val colorIndex: Int = 0,
    // 单次
    val singleEpochDay: Long? = null,
    // 周期
    val repeatStartEpochDay: Long? = null,
    val repeatEndEpochDay: Long? = null,
    val weekdaysMask: Int = 0,
    // 时间（当日分钟数）
    val startMinute: Int? = null,
    val endMinute: Int? = null,
) {
    val isRecurring: Boolean get() = repeatStartEpochDay != null

    /** 单次日程适用的日期；周期日程返回 null。 */
    val singleDate: java.time.LocalDate? get() = singleEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
}