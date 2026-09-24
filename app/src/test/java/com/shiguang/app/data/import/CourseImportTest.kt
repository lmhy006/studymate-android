package com.shiguang.app.data.import

import com.shiguang.app.core.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * BIT101 课表 JSON 导入解析与转换测试。
 */
class CourseImportTest {

    @Test
    fun `解析合法 JSON`() {
        val json = """{"courses":[{"name":"线性代数","teacher":"张老师","classroom":"A101","weekday":1,"start_section":1,"end_section":2,"weeks":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]}]}"""
        val (courses, errors) = parseCoursesJson(json)
        assertTrue(errors.isEmpty())
        assertEquals(1, courses.size)
        val c = courses.first()
        assertEquals("线性代数", c.name)
        assertEquals("张老师", c.teacher)
        assertEquals("A101", c.classroom)
        assertEquals(1, c.weekday)
        assertEquals(1, c.startSection)
        assertEquals(2, c.endSection)
        assertEquals(1, c.weeksMin)
        assertEquals(16, c.weeksMax)
    }

    @Test
    fun `非法课程进错误列表但不拖垮整体`() {
        val json = """{"courses":[
            {"name":"","weekday":9,"start_section":0,"end_section":0},
            {"name":"英语","weekday":3,"start_section":3,"end_section":3}
        ]}"""
        val (courses, errors) = parseCoursesJson(json)
        assertEquals(1, courses.size)
        assertEquals(1, errors.size)
        assertEquals("英语", courses.first().name)
    }

    @Test
    fun `非法 JSON 与空白输入`() {
        val bad = parseCoursesJson("not-json{")
        assertTrue(bad.first.isEmpty())
        assertTrue(bad.second.isNotEmpty())

        val blank = parseCoursesJson("   ")
        assertTrue(blank.first.isEmpty())
        assertTrue(blank.second.isNotEmpty())
    }

    @Test
    fun `转换到周期日程并按节次取时间`() {
        val json = """{"courses":[{"name":"线性代数","weekday":1,"start_section":1,"end_section":2,"weeks":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]}]}"""
        val (courses, _) = parseCoursesJson(json)
        val termStart = LocalDate.of(2025, 9, 1) // 周一
        val list = coursesToSchedules(courses, termStart)
        assertEquals(1, list.size)
        val s = list.first()
        assertEquals("线性代数", s.title)
        assertEquals(termStart.toEpochDay(), s.repeatStartEpochDay)
        assertEquals(termStart.plusDays(15 * 7L).toEpochDay(), s.repeatEndEpochDay) // 16 周
        assertEquals(1, s.weekdaysMask) // 周一
        assertEquals(8 * 60, s.startMinute)      // 第1节 08:00
        assertEquals(9 * 60 + 35, s.endMinute)   // 第2节 09:35
        assertTrue(s.isRecurring)
        assertTrue(s.note!!.contains("第1-16周")) // 备注含周次
    }

    @Test
    fun `学期开始非周一时对齐到对应星期`() {
        val json = """{"courses":[{"name":"高数","weekday":3,"start_section":6,"end_section":6}]}"""
        val (courses, _) = parseCoursesJson(json)
        val list = coursesToSchedules(courses, LocalDate.of(2025, 9, 1))
        val s = list.first()
        assertEquals(LocalDate.of(2025, 9, 3), DateUtils.fromEpochDay(s.repeatStartEpochDay!!)) // 首个周三
        assertEquals(13 * 60 + 20, s.startMinute) // 第6节 13:20
        assertEquals(14 * 60 + 5, s.endMinute)    // 14:05
    }
}