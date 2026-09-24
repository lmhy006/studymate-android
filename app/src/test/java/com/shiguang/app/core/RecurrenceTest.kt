package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {

    private val d = { s: String -> LocalDate.parse(s) }

    @Test
    fun `weekday mask 周一 to 周日`() {
        assertTrue(WeekdayMask.contains(1, d("2025-01-06"))) // 周一
        assertFalse(WeekdayMask.contains(1, d("2025-01-07")))
        assertTrue(WeekdayMask.contains(1 shl 5, d("2025-01-11"))) // 周六 2025-01-11
        assertEquals("周一/周三/周五", WeekdayMask.names(1 or (1 shl 2) or (1 shl 4)))
    }

    @Test
    fun `occurrencesBetween 命中窗口内的周一`() {
        // 2025-06 的周一：2, 9, 16, 23, 30
        val result = Recurrence.occurrencesBetween(
            ruleStart = d("2025-06-01"),
            ruleEnd = d("2025-06-30"),
            weekdaysMask = 1,
            rangeStart = d("2025-06-05"),
            rangeEnd = d("2025-06-25"),
        )
        assertEquals(listOf(d("2025-06-09"), d("2025-06-16"), d("2025-06-23")), result)
    }

    @Test
    fun `occurrencesBetween 区间无交集返回空`() {
        val result = Recurrence.occurrencesBetween(
            ruleStart = d("2025-06-01"),
            ruleEnd = d("2025-06-10"),
            weekdaysMask = 1,
            rangeStart = d("2025-07-01"),
            rangeEnd = d("2025-07-31"),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `habitOccurrence 每3天`() {
        val start = d("2025-01-01")
        assertEquals(d("2025-01-01"), Recurrence.habitOccurrence(start, 3, 0))
        assertEquals(d("2025-01-04"), Recurrence.habitOccurrence(start, 3, 1))
        assertEquals(d("2025-01-13"), Recurrence.habitOccurrence(start, 3, 4))
    }

    @Test
    fun `lastOccurrenceIndex 计算`() {
        val start = d("2025-01-01")
        // 2025-01-08 是第 7 天（diff=7），每3天 → index 2
        assertEquals(2, Recurrence.lastOccurrenceIndex(start, 3, d("2025-01-08")))
        // 2025-01-01 当天 → index 0
        assertEquals(0, Recurrence.lastOccurrenceIndex(start, 3, d("2025-01-01")))
        // 起点晚于今天 → -1
        assertEquals(-1, Recurrence.lastOccurrenceIndex(start, 3, d("2024-12-31")))
    }

    @Test
    fun `isTodayOccurrence 是否为周期日`() {
        val start = d("2025-01-01")
        assertTrue(Recurrence.isTodayOccurrence(start, 3, d("2025-01-01")))
        assertTrue(Recurrence.isTodayOccurrence(start, 3, d("2025-01-04")))
        assertFalse(Recurrence.isTodayOccurrence(start, 3, d("2025-01-05")))
        assertTrue(Recurrence.isTodayOccurrence(start, 1, d("2025-06-15"))) // 每日恒真
    }

    @Test
    fun `isHabitOccurrence 判断任意一天`() {
        val start = d("2025-01-01")
        assertTrue(Recurrence.isHabitOccurrence(start, 3, d("2025-01-07")))
        assertFalse(Recurrence.isHabitOccurrence(start, 3, d("2025-01-08")))
        assertFalse(Recurrence.isHabitOccurrence(start, 3, d("2024-12-31"))) // 早于起点
        assertTrue(Recurrence.isHabitOccurrence(start, 1, d("2025-01-02")))
    }
}