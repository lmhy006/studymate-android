package com.shiguang.app.core

/**
 * 节次（课表节）时间表——默认值取自 BIT101-Android 的设置默认值（13 节）。
 * 纯数据 + 纯函数，可单测。
 */
data class SchedulePeriod(
    val number: Int,
    /** 当日分钟数 */
    val startMinute: Int,
    val endMinute: Int,
)

object SchedulePeriods {

    /** BIT101 默认节次表（严格一致）。 */
    val DEFAULT: List<SchedulePeriod> = listOf(
        SchedulePeriod(1, 8 * 60 + 0, 8 * 60 + 45),
        SchedulePeriod(2, 8 * 60 + 50, 9 * 60 + 35),
        SchedulePeriod(3, 9 * 60 + 55, 10 * 60 + 40),
        SchedulePeriod(4, 10 * 60 + 45, 11 * 60 + 30),
        SchedulePeriod(5, 11 * 60 + 35, 12 * 60 + 20),
        SchedulePeriod(6, 13 * 60 + 20, 14 * 60 + 5),
        SchedulePeriod(7, 14 * 60 + 10, 14 * 60 + 55),
        SchedulePeriod(8, 15 * 60 + 15, 16 * 60 + 0),
        SchedulePeriod(9, 16 * 60 + 5, 16 * 60 + 50),
        SchedulePeriod(10, 16 * 60 + 55, 17 * 60 + 40),
        SchedulePeriod(11, 18 * 60 + 30, 19 * 60 + 15),
        SchedulePeriod(12, 19 * 60 + 20, 20 * 60 + 5),
        SchedulePeriod(13, 20 * 60 + 10, 20 * 60 + 55),
    )

    /** minute 所在（或下一个）节次的索引；早于第一节返回 0，晚于最后一节返回最后。 */
    fun periodIndexForMinute(minute: Int): Int {
        val idx = DEFAULT.indexOfFirst { minute < it.endMinute }
        return if (idx == -1) DEFAULT.lastIndex else idx.coerceAtLeast(0)
    }

    /** [startMinute, endMinute) 跨越的节次行范围（含收尾节次）。 */
    fun periodRange(startMinute: Int, endMinute: Int): IntRange {
        val a = periodIndexForMinute(startMinute)
        val b = periodIndexForMinute((endMinute - 1).coerceAtLeast(startMinute))
        return a..b.coerceAtLeast(a)
    }

    /** 第 [section] 节的开始分钟（1 起）。 */
    fun startMinute(section: Int): Int = DEFAULT.getOrNull(section - 1)?.startMinute ?: 0

    /** 第 [section] 节的结束分钟（1 起）。 */
    fun endMinute(section: Int): Int = DEFAULT.getOrNull(section - 1)?.endMinute ?: startMinute(section)

    fun periodStartText(period: SchedulePeriod): String = DateUtils.timeText(period.startMinute)

    fun periodRangeText(period: SchedulePeriod): String =
        "${DateUtils.timeText(period.startMinute)}-${DateUtils.timeText(period.endMinute)}"
}