package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePeriodsTest {

    @Test
    fun `默认节次表为 13 节且与 BIT101 一致`() {
        val p = SchedulePeriods.DEFAULT
        assertEquals(13, p.size)
        assertEquals(8 * 60, p.first().startMinute)
        assertEquals(8 * 60 + 45, p.first().endMinute)
        assertEquals(20 * 60 + 10, p.last().startMinute)
        assertEquals(20 * 60 + 55, p.last().endMinute)
        // 相邻节次不重叠
        assertTrue(p.zipWithNext().all { (a, b) -> b.startMinute > a.endMinute })
        // 编号连续
        assertTrue(p.mapIndexed { i, it -> it.number == i + 1 }.all { it })
        // 第2节 = 08:50-09:35（用户举例）
        assertEquals(8 * 60 + 50, SchedulePeriods.startMinute(2))
        assertEquals(9 * 60 + 35, SchedulePeriods.endMinute(2))
    }

    @Test
    fun `periodIndexForMinute 边界`() {
        assertEquals(0, SchedulePeriods.periodIndexForMinute(8 * 60))
        assertEquals(0, SchedulePeriods.periodIndexForMinute(8 * 60 + 44))
        assertEquals(1, SchedulePeriods.periodIndexForMinute(8 * 60 + 50))
        // 晚于最后一节 -> 最后一节
        assertEquals(12, SchedulePeriods.periodIndexForMinute(20 * 60 + 55))
        assertEquals(12, SchedulePeriods.periodIndexForMinute(23 * 60))
        // 早于第一节 -> 第一节
        assertEquals(0, SchedulePeriods.periodIndexForMinute(7 * 60))
    }

    @Test
    fun `periodRange 起止跨节`() {
        assertEquals(0..0, SchedulePeriods.periodRange(8 * 60, 8 * 60 + 45))
        assertEquals(0..1, SchedulePeriods.periodRange(8 * 60, 9 * 60 + 35))   // 1-2节
        assertEquals(0..2, SchedulePeriods.periodRange(8 * 60, 10 * 60 + 40))  // 1-3节
        assertEquals(2..3, SchedulePeriods.periodRange(9 * 60 + 55, 11 * 60 + 30)) // 3-4节
        // 19:20 在第12节，20:06（>12节结束20:05，<13节开始20:10）也落在12节 → 12..12
        assertEquals(11..12, SchedulePeriods.periodRange(19 * 60 + 20, 20 * 60 + 6))
        // 完整一天 08:00-20:55 → 1..13节
        assertEquals(0..12, SchedulePeriods.periodRange(8 * 60, 20 * 60 + 55))
    }

    @Test
    fun `时间表序列化与解析往返`() {
        val text = SchedulePeriods.toTimeTableString(SchedulePeriods.DEFAULT)
        assertEquals(SchedulePeriods.DEFAULT, SchedulePeriods.parseTimeTable(text))
    }

    @Test
    fun `非法时间表解析返回 null`() {
        assertNull(SchedulePeriods.parseTimeTable(null))
        assertNull(SchedulePeriods.parseTimeTable(""))
        assertNull(SchedulePeriods.parseTimeTable("08:00"))                    // 缺一列
        assertNull(SchedulePeriods.parseTimeTable("08:00,09:00\n09:30,09:00")) // 结束早于开始
        assertNull(SchedulePeriods.parseTimeTable("08:00,09:00\n08:30,10:00")) // 相邻重叠
    }

    @Test
    fun `自定义时间表参与节次计算`() {
        val periods = SchedulePeriods.parseTimeTable("07:00,07:30\n08:00,08:45")!!
        assertEquals(2, periods.size)
        assertEquals(0, SchedulePeriods.periodIndexForMinute(7 * 60, periods))
        assertEquals(1, SchedulePeriods.periodIndexForMinute(8 * 60, periods))
        assertEquals(0..1, SchedulePeriods.periodRange(7 * 60, 8 * 60 + 45, periods))
        assertEquals(7 * 60, SchedulePeriods.startMinute(1, periods))
        assertEquals(8 * 60 + 45, SchedulePeriods.endMinute(2, periods))
    }
}