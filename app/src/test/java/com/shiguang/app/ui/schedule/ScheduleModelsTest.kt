package com.shiguang.app.ui.schedule

import com.shiguang.app.core.WeekParity
import com.shiguang.app.data.entity.ScheduleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 日程物化（单次 + 批量周期展开）测试。
 * 覆盖需求 3 的核心：日期范围 × 星期掩码 × 时间段 的自动生成。
 */
class ScheduleModelsTest {

    private val d = { s: String -> LocalDate.parse(s) }

    @Test
    fun `单次日程在区间内返回一次且字段正确`() {
        val schedule = ScheduleEntity(
            id = 1,
            title = "线性代数",
            singleEpochDay = d("2025-06-10").toEpochDay(),
            startMinute = 8 * 60,
            endMinute = 9 * 60 + 50,
        )
        val result = materializeOccurrences(listOf(schedule), d("2025-06-08"), d("2025-06-15"))
        assertEquals(1, result.size)
        val occurrence = result.first()
        assertEquals("线性代数", occurrence.title)
        assertEquals(d("2025-06-10"), occurrence.date)
        assertEquals(8 * 60, occurrence.startMinute)
        assertEquals(9 * 60 + 50, occurrence.endMinute)
        assertEquals(1L, occurrence.scheduleId)
    }

    @Test
    fun `单次日程在查询区间外返回空`() {
        val schedule = ScheduleEntity(
            id = 1,
            title = "x",
            singleEpochDay = d("2025-06-10").toEpochDay(),
            startMinute = 0,
            endMinute = 60,
        )
        assertTrue(materializeOccurrences(listOf(schedule), d("2025-06-12"), d("2025-06-30")).isEmpty())
    }

    @Test
    fun `周期日程按星期掩码自动展开`() {
        // 2025-06 的周一：2, 9, 16, 23, 30；查询窗口 06-05~06-25 命中 9/16/23
        val schedule = ScheduleEntity(
            id = 1,
            title = "英语早读",
            colorIndex = 2,
            repeatStartEpochDay = d("2025-06-01").toEpochDay(),
            repeatEndEpochDay = d("2025-06-30").toEpochDay(),
            weekdaysMask = 1, // bit0 = 周一
            startMinute = 8 * 60,
            endMinute = 9 * 60 + 50,
        )
        val dates = materializeOccurrences(listOf(schedule), d("2025-06-05"), d("2025-06-25"))
            .map { it.date }
        assertEquals(listOf(d("2025-06-09"), d("2025-06-16"), d("2025-06-23")), dates)
        val first = materializeOccurrences(listOf(schedule), d("2025-06-05"), d("2025-06-25")).first()
        assertEquals(2, first.colorIndex)
        assertEquals("英语早读", first.title)
        assertEquals(8 * 60, first.startMinute)
    }

    @Test
    fun `周期日程支持单双周过滤`() {
        // 2025-09：周一 = 01/08/15/22；第1周=09-01 所在周一，单周取 01、15，双周取 08、22
        fun schedule(parity: Int) = ScheduleEntity(
            id = 1,
            title = "慢跑",
            repeatStartEpochDay = d("2025-09-01").toEpochDay(),
            repeatEndEpochDay = d("2025-09-30").toEpochDay(),
            weekdaysMask = 1, // 周一
            weekParity = parity,
            startMinute = 7 * 60,
            endMinute = 8 * 60,
        )
        val odd = materializeOccurrences(listOf(schedule(WeekParity.ODD)), d("2025-09-01"), d("2025-09-28"))
            .map { it.date }
        assertEquals(listOf(d("2025-09-01"), d("2025-09-15")), odd)
        val even = materializeOccurrences(listOf(schedule(WeekParity.EVEN)), d("2025-09-01"), d("2025-09-28"))
            .map { it.date }
        assertEquals(listOf(d("2025-09-08"), d("2025-09-22")), even)
    }

    @Test
    fun `周期规则与查询区间无交集返回空`() {
        val schedule = ScheduleEntity(
            id = 1,
            title = "暑期课",
            repeatStartEpochDay = d("2025-07-01").toEpochDay(),
            repeatEndEpochDay = d("2025-07-31").toEpochDay(),
            weekdaysMask = 1,
            startMinute = 8 * 60,
            endMinute = 10 * 60,
        )
        assertTrue(materializeOccurrences(listOf(schedule), d("2025-06-01"), d("2025-06-30")).isEmpty())
    }

    @Test
    fun `结束时间不晚于开始时间时自动矫正为至少1分钟`() {
        val schedule = ScheduleEntity(
            id = 1,
            title = "课",
            singleEpochDay = d("2025-06-10").toEpochDay(),
            startMinute = 100,
            endMinute = 50,
        )
        val occurrence = materializeOccurrences(listOf(schedule), d("2025-06-10"), d("2025-06-10")).first()
        assertEquals(101, occurrence.endMinute)
    }

    @Test
    fun `空列表返回空结果`() {
        assertTrue(materializeOccurrences(emptyList(), d("2025-06-01"), d("2025-06-30")).isEmpty())
    }
}