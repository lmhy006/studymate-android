package com.shiguang.app.core

import java.time.LocalDate

/**
 * 连续打卡统计（纯函数，可单测）。
 * checked 为已打卡的 epochDay 集合；today 为“今天”。
 */
object Streak {

    /**
     * 当前连续打卡天数（含今天，若今天到期且已打卡）。
     * 今天到期但尚未打卡时，从上一个周期往前数（即“只要今天打上就续上”的效果）；
     * 若最近一个周期已经错过（断签），返回 0。
     */
    fun current(start: LocalDate, intervalDays: Int, checked: Set<Long>, today: LocalDate): Int {
        val lastK = Recurrence.lastOccurrenceIndex(start, intervalDays, today)
        if (lastK < 0) return 0
        val lastDue = Recurrence.habitOccurrence(start, intervalDays, lastK)
        var countFrom = lastK
        if (lastDue == today && today.toEpochDay() !in checked) {
            if (lastK == 0) return 0
            countFrom = lastK - 1
        }
        var streak = 0
        var k = countFrom
        while (k >= 0 && Recurrence.habitOccurrence(start, intervalDays, k).toEpochDay() in checked) {
            streak++
            k--
        }
        return streak
    }

    /** 历史最长连续打卡天数。 */
    fun longest(start: LocalDate, intervalDays: Int, checked: Set<Long>, today: LocalDate): Int {
        val lastK = Recurrence.lastOccurrenceIndex(start, intervalDays, today)
        if (lastK < 0) return 0
        var best = 0
        var run = 0
        for (k in 0..lastK) {
            if (Recurrence.habitOccurrence(start, intervalDays, k).toEpochDay() in checked) {
                run++
                if (run > best) best = run
            } else {
                run = 0
            }
        }
        return best
    }

    /** 累计打卡次数。 */
    fun total(start: LocalDate, intervalDays: Int, checked: Set<Long>, today: LocalDate): Int {
        val lastK = Recurrence.lastOccurrenceIndex(start, intervalDays, today)
        if (lastK < 0) return 0
        return (0..lastK).count { Recurrence.habitOccurrence(start, intervalDays, it).toEpochDay() in checked }
    }

    /** 断签次数：已经过去（早于今天）且未打卡的周期数。 */
    fun missed(start: LocalDate, intervalDays: Int, checked: Set<Long>, today: LocalDate): Int {
        val lastK = Recurrence.lastOccurrenceIndex(start, intervalDays, today)
        if (lastK < 0) return 0
        return (0..lastK).count { k ->
            val d = Recurrence.habitOccurrence(start, intervalDays, k)
            d.isBefore(today) && d.toEpochDay() !in checked
        }
    }
}