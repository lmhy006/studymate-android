package com.shiguang.app.data.import

import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.WeekParity
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
    fun `教学周起点为周中时第1周不后移`() {
        // 教学周起点=2025-09-03（周三）：第1周 = 该周周一 09-01 起的教学周。
        // 周一的课第1周应落在 09-01，而不是顺延到下周 09-08（旧实现的 bug）。
        val json = """{"courses":[{"name":"周一课","weekday":1,"start_section":1,"end_section":1}]}"""
        val s = coursesToSchedules(parseCoursesJson(json).first, LocalDate.of(2025, 9, 3)).first()
        assertEquals(LocalDate.of(2025, 9, 1), DateUtils.fromEpochDay(s.repeatStartEpochDay!!))
        // 第2周 → 09-08
        val json2 = """{"courses":[{"name":"周一课","weekday":1,"start_section":1,"end_section":1,"weeks":[2,2]}]}"""
        val s2 = coursesToSchedules(parseCoursesJson(json2).first, LocalDate.of(2025, 9, 3)).first()
        assertEquals(LocalDate.of(2025, 9, 8), DateUtils.fromEpochDay(s2.repeatStartEpochDay!!))
    }

    @Test
    fun `超出当前时间表节数的课程在转换时被跳过`() {
        // 默认 13 节：第 14 节的课应被跳过，其余保留
        val json = """{"courses":[
            {"name":"正常课","weekday":1,"start_section":1,"end_section":2},
            {"name":"超节课","weekday":2,"start_section":14,"end_section":14}
        ]}"""
        val (courses, errors) = parseCoursesJson(json)
        assertEquals(2, courses.size) // 解析阶段 1..20 均视为结构合法
        assertTrue(errors.isEmpty())
        val list = coursesToSchedules(courses, LocalDate.of(2025, 9, 1))
        assertEquals(1, list.size)
        assertEquals("正常课", list.first().title)
    }

    @Test
    fun `week_parity 单双周解析并写入日程`() {
        val json = """{"courses":[
            {"name":"单周课","weekday":1,"start_section":1,"end_section":1,"week_parity":"odd"},
            {"name":"双周课","weekday":2,"start_section":1,"end_section":1,"week_parity":"双周"}
        ]}"""
        val (courses, errors) = parseCoursesJson(json)
        assertTrue(errors.isEmpty())
        assertEquals(2, courses.size)
        assertEquals(WeekParity.ODD, courses[0].weekParity)
        assertEquals(WeekParity.EVEN, courses[1].weekParity)
        val list = coursesToSchedules(courses, LocalDate.of(2025, 9, 1))
        assertEquals(WeekParity.ODD, list[0].weekParity)
        assertEquals(WeekParity.EVEN, list[1].weekParity)
        assertTrue(list[0].note!!.contains("单周"))
        assertTrue(list[1].note!!.contains("双周"))
        // 缺省为每周
        val plain = parseCoursesJson("""{"courses":[{"name":"常课","weekday":3,"start_section":1,"end_section":1}]}""").first
        assertEquals(WeekParity.ALL, plain[0].weekParity)
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