package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class DateUtilsTest {

    @Test
    fun `weekOf 从周一开始`() {
        val anchor = LocalDate.of(2025, 6, 5) // 周四
        val week = DateUtils.weekOf(anchor)
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2025, 6, 2), week.first()) // 周一
        assertEquals(LocalDate.of(2025, 6, 8), week.last())  // 周日
        assertEquals(DayOfWeek.MONDAY, week.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, week.last().dayOfWeek)
    }

    @Test
    fun `monthGrid 42格 起始为周一`() {
        val grid = DateUtils.monthGrid(2025, 6) // 2025-06-01 是周日
        assertEquals(42, grid.size)
        assertEquals(DayOfWeek.MONDAY, grid.first().dayOfWeek)
        // 2025-06-01 落在周日 → 格子首位应为 2025-05-26（周一）
        assertEquals(LocalDate.of(2025, 5, 26), grid.first())
        assertTrue(grid.contains(LocalDate.of(2025, 6, 1)))
    }

    @Test
    fun `monthGrid YearMonth 重载`() {
        assertEquals(
            DateUtils.monthGrid(2025, 6),
            DateUtils.monthGrid(YearMonth.of(2025, 6)),
        )
    }

    @Test
    fun `weekdayName 与索引`() {
        assertEquals("周一", DateUtils.weekdayName(LocalDate.of(2025, 6, 2)))
        assertEquals("周日", DateUtils.weekdayName(LocalDate.of(2025, 6, 8)))
        assertEquals(0, DateUtils.weekdayIndex(LocalDate.of(2025, 6, 2)))
        assertEquals(6, DateUtils.weekdayIndex(LocalDate.of(2025, 6, 8)))
    }

    @Test
    fun `timeText 格式化`() {
        assertEquals("08:00", DateUtils.timeText(8 * 60))
        assertEquals("09:50", DateUtils.timeText(9 * 60 + 50))
        assertEquals("23:59", DateUtils.timeText(23 * 60 + 59))
        assertEquals("00:00", DateUtils.timeText(0))
    }

    @Test
    fun `utcMillis 往返`() {
        val date = LocalDate.of(2025, 6, 5)
        assertEquals(date, DateUtils.fromUtcMillis(DateUtils.toUtcMillis(date)))
    }

    @Test
    fun `formatMonthDay 文案`() {
        assertEquals("6月5日", DateUtils.formatMonthDay(LocalDate.of(2025, 6, 5)))
    }
}