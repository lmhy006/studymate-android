package com.shiguang.app.core

import java.time.LocalDate

/**
 * 星期掩码：bit0=周一 … bit6=周日。
 * 用于“批量周期日程”——例如“3月1日~6月30日之间每个周一上午 8:00~9:50”。
 */
object WeekdayMask {

    fun bit(d: LocalDate): Int = 1 shl DateUtils.weekdayIndex(d)

    fun contains(mask: Int, d: LocalDate): Boolean = (mask and bit(d)) != 0

    /** 掩码 -> "周一/周三/周五" 这种可读文本。 */
    fun names(mask: Int): String =
        (0..6)
            .filter { (mask and (1 shl it)) != 0 }
            .joinToString("/") { DateUtils.WEEKDAY_NAMES[it] }
            .ifEmpty { "（未选择）" }
}

/**
 * 周期事件物化（纯函数，可单测）。
 */
object Recurrence {

    /**
     * 周期规则 [ruleStart, ruleEnd]（含）在查询区间 [rangeStart, rangeEnd]（含）
     * 内、命中 [weekdaysMask] 的所有发生日期，升序返回。
     */
    fun occurrencesBetween(
        ruleStart: LocalDate,
        ruleEnd: LocalDate,
        weekdaysMask: Int,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<LocalDate> {
        val lo = if (ruleStart.isAfter(rangeStart)) ruleStart else rangeStart
        val hi = if (ruleEnd.isBefore(rangeEnd)) ruleEnd else rangeEnd
        if (lo.isAfter(hi)) return emptyList()
        return buildList {
            var d = lo
            while (!d.isAfter(hi)) {
                if (WeekdayMask.contains(weekdaysMask, d)) add(d)
                d = d.plusDays(1)
            }
        }
    }

    /**
     * 习惯打卡的第 k（>=0）个周期发生日：start + k * intervalDays。
     * intervalDays=1 每日，=7 每周，=N 自定义。
     */
    fun habitOccurrence(start: LocalDate, intervalDays: Int, k: Int): LocalDate =
        start.plusDays(k.toLong() * intervalDays)

    /** <= today 的最近一个周期序号；起点晚于今天返回 -1。 */
    fun lastOccurrenceIndex(start: LocalDate, intervalDays: Int, today: LocalDate): Int {
        if (today.isBefore(start)) return -1
        val diff = today.toEpochDay() - start.toEpochDay()
        return (diff / intervalDays).toInt()
    }

    /** 今天是否是本习惯的一个“周期日”（即今天到期）。 */
    fun isTodayOccurrence(start: LocalDate, intervalDays: Int, today: LocalDate): Boolean {
        val k = lastOccurrenceIndex(start, intervalDays, today)
        if (k < 0) return false
        return habitOccurrence(start, intervalDays, k) == today
    }

    /** 某天是否为该习惯的周期日。 */
    fun isHabitOccurrence(start: LocalDate, intervalDays: Int, date: LocalDate): Boolean {
        if (date.isBefore(start)) return false
        val diff = date.toEpochDay() - start.toEpochDay()
        return diff % intervalDays == 0L
    }
}