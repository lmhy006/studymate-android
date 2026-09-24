package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakTest {

    private val d = { s: String -> LocalDate.parse(s) }
    private val day = { s: String -> d(s).toEpochDay() }

    @Test
    fun `每日习惯 连续打卡3天`() {
        val start = d("2025-01-01")
        val checked = setOf(day("2025-01-01"), day("2025-01-02"), day("2025-01-03"))
        val today = d("2025-01-03")
        assertEquals(3, Streak.current(start, 1, checked, today))
        assertEquals(3, Streak.longest(start, 1, checked, today))
        assertEquals(3, Streak.total(start, 1, checked, today))
        assertEquals(0, Streak.missed(start, 1, checked, today))
    }

    @Test
    fun `今天到期未打卡 连续数从上一周期算`() {
        val start = d("2025-01-01")
        val checked = setOf(day("2025-01-01"), day("2025-01-02"))
        val today = d("2025-01-03") // 今天还没打
        assertEquals(2, Streak.current(start, 1, checked, today))
    }

    @Test
    fun `昨天就断签 当前连续为0`() {
        val start = d("2025-01-01")
        val checked = setOf(day("2025-01-01"))
        val today = d("2025-01-03") // 01-02 断签
        assertEquals(0, Streak.current(start, 1, checked, today))
        assertEquals(1, Streak.longest(start, 1, checked, today))
        assertEquals(1, Streak.missed(start, 1, checked, today))
    }

    @Test
    fun `每3天习惯 断签统计`() {
        val start = d("2025-01-01")
        // 周期日：01-01, 01-04, 01-07, 01-10, 01-13
        val checked = setOf(day("2025-01-01"), day("2025-01-07"), day("2025-01-13"))
        val today = d("2025-01-14")
        assertEquals(2, Streak.missed(start, 3, checked, today)) // 01-04 与 01-10 均未打
        assertEquals(1, Streak.longest(start, 3, checked, today)) // 三次打卡互不相连
        assertEquals(1, Streak.current(start, 3, checked, today)) // 最近周期 01-13 已打，从它往前数
        assertEquals(3, Streak.total(start, 3, checked, today))
    }

    @Test
    fun `起点晚于今天 全部为0`() {
        val start = d("2025-02-01")
        val today = d("2025-01-15")
        assertEquals(0, Streak.current(start, 1, emptySet(), today))
        assertEquals(0, Streak.longest(start, 1, emptySet(), today))
        assertEquals(0, Streak.total(start, 1, emptySet(), today))
        assertEquals(0, Streak.missed(start, 1, emptySet(), today))
    }

    @Test
    fun `每周习惯`() {
        val start = d("2025-01-06") // 周一
        val checked = setOf(day("2025-01-06"), day("2025-01-13"))
        val today = d("2025-01-20") // 今天到期未打
        assertEquals(2, Streak.current(start, 7, checked, today))
        assertEquals(2, Streak.longest(start, 7, checked, today))
        assertEquals(0, Streak.missed(start, 7, checked, today))
    }
}