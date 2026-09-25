package com.shiguang.app.core

import java.time.LocalTime

/**
 * 节次（课表节）时间表——默认值取自 BIT101-Android 的设置默认值（13 节）。
 * 支持自定义（设置 → 日程设置 → 时间表），周视图与课表导入均使用活动时间表。
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

    /** BIT101 同款时间表文本（每行 "HH:mm,HH:mm"，可用 [toTimeTableString] 还原）。 */
    fun toTimeTableString(periods: List<SchedulePeriod>): String =
        periods.joinToString("\n") {
            "${DateUtils.timeText(it.startMinute)},${DateUtils.timeText(it.endMinute)}"
        }

    /**
     * 解析时间表文本（每行 "HH:mm,HH:mm"，节次先后有序且不重叠）；非法/为空返回 null。
     * 编号按解析顺序自动重新编号。
     */
    fun parseTimeTable(text: String?): List<SchedulePeriod>? {
        if (text.isNullOrBlank()) return null
        val result = mutableListOf<SchedulePeriod>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val parts = line.split(",")
            if (parts.size != 2) return null
            val start = runCatching { LocalTime.parse(parts[0].trim()) }.getOrNull() ?: return null
            val end = runCatching { LocalTime.parse(parts[1].trim()) }.getOrNull() ?: return null
            if (!end.isAfter(start)) return null
            if (result.isNotEmpty() && !start.isAfter(
                    LocalTime.of(result.last().endMinute / 60, result.last().endMinute % 60)
                )
            ) {
                return null
            }
            result.add(
                SchedulePeriod(
                    number = result.size + 1,
                    startMinute = start.hour * 60 + start.minute,
                    endMinute = end.hour * 60 + end.minute,
                )
            )
        }
        return result.takeIf { it.isNotEmpty() }
    }

    /** minute 所在（或下一个）节次的索引；早于第一节返回 0，晚于最后一节返回最后。 */
    fun periodIndexForMinute(minute: Int, periods: List<SchedulePeriod> = DEFAULT): Int {
        val idx = periods.indexOfFirst { minute < it.endMinute }
        return if (idx == -1) periods.lastIndex else idx.coerceAtLeast(0)
    }

    /** [startMinute, endMinute) 跨越的节次行范围（含收尾节次）。 */
    fun periodRange(startMinute: Int, endMinute: Int, periods: List<SchedulePeriod> = DEFAULT): IntRange {
        val a = periodIndexForMinute(startMinute, periods)
        val b = periodIndexForMinute((endMinute - 1).coerceAtLeast(startMinute), periods)
        return a..b.coerceAtLeast(a)
    }

    /** 第 [section] 节的开始分钟（1 起）。 */
    fun startMinute(section: Int, periods: List<SchedulePeriod> = DEFAULT): Int =
        periods.getOrNull(section - 1)?.startMinute ?: 0

    /** 第 [section] 节的结束分钟（1 起）。 */
    fun endMinute(section: Int, periods: List<SchedulePeriod> = DEFAULT): Int =
        periods.getOrNull(section - 1)?.endMinute ?: startMinute(section, periods)

    fun periodStartText(period: SchedulePeriod): String = DateUtils.timeText(period.startMinute)

    fun periodRangeText(period: SchedulePeriod): String =
        "${DateUtils.timeText(period.startMinute)}-${DateUtils.timeText(period.endMinute)}"
}