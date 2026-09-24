package com.shiguang.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 习惯。
 * intervalDays：周期天数。1=每日，7=每周，N=自定义每 N 天一次。
 * startEpochDay：创建日（周期模式的锚点；自由模式仅作参考）。
 *
 * mode：
 * - [MODE_CYCLIC] 周期模式：以创建日为锚点的固定周期日，每周期最多打卡 1 次；
 * - [MODE_FREE] 自由模式：可随时打卡，打卡起算 N 天，超期记缺卡，下次打卡重新起算。
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val intervalDays: Int = 1,
    val startEpochDay: Long,
    val mode: Int = MODE_CYCLIC,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /** 周期模式：固定周期日网格，每周期最多打卡 1 次。 */
        const val MODE_CYCLIC = 0

        /** 自由模式：打卡日锚定，N 天内再打为连续，超期缺卡，打卡重置。 */
        const val MODE_FREE = 1
    }
}