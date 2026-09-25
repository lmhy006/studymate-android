package com.shiguang.app.data.import

import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.SchedulePeriods
import com.shiguang.app.core.WeekParity
import com.shiguang.app.data.entity.ScheduleEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * 待导入的一门课（与 BIT101 课表对象同构）。
 */
data class ImportedCourse(
    val name: String,
    val teacher: String,
    val classroom: String,
    /** 1=周一 … 7=周日 */
    val weekday: Int,
    val startSection: Int,
    val endSection: Int,
    /** 单双周：0=每周（默认）、1=单周、2=双周 */
    val weekParity: Int = WeekParity.ALL,
    /** 教学周范围：形如 weeks=[1..16] 取首尾；缺省时按整学期处理 */
    val weeksMin: Int?,
    val weeksMax: Int?,
) {
    /** 节次上限：兼容自定义时间表（默认 13 节，可最多 20 节）。 */
    companion object {
        const val MAX_SECTION = 20
    }

    val isValid: Boolean
        get() = name.isNotBlank() &&
            weekday in 1..7 &&
            startSection in 1..MAX_SECTION &&
            endSection in startSection..MAX_SECTION
}

/**
 * 解析「BIT101 课表 JSON」文本。
 * 示例：
 * {"courses":[{"name":"线性代数","teacher":"张老师","classroom":"良乡1-101",
 *   "weekday":1,"start_section":1,"end_section":2,"weeks":[1,2,3,4,5,6]}]}
 *
 * 返回 (有效课程列表, 错误信息列表)，item 级错误不会导致整体失败。
 */
fun parseCoursesJson(text: String): Pair<List<ImportedCourse>, List<String>> {
    val errors = mutableListOf<String>()
    if (text.isBlank()) return emptyList<ImportedCourse>() to listOf("内容为空")
    val courses = mutableListOf<ImportedCourse>()
    try {
        val root = JSONObject(text)
        val array = root.optJSONArray("courses") ?: JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            val teacher = obj.optString("teacher", "").trim()
            val classroom = obj.optString("classroom", "").trim()
            val weekday = obj.optInt("weekday", 0)
            val startSection = obj.optInt("start_section", 0)
            val endSection = obj.optInt("end_section", 0)
            val weekParity = when (obj.optString("week_parity", "").trim().lowercase()) {
                "odd", "single", "单周" -> WeekParity.ODD
                "even", "double", "双周" -> WeekParity.EVEN
                else -> WeekParity.ALL
            }
            val weeksMin: Int?
            val weeksMax: Int?
            val weeks = obj.optJSONArray("weeks")
            if (weeks != null && weeks.length() > 0) {
                var min = Int.MAX_VALUE
                var max = Int.MIN_VALUE
                for (w in 0 until weeks.length()) {
                    val v = weeks.optInt(w, -1)
                    if (v > 0) {
                        if (v < min) min = v
                        if (v > max) max = v
                    }
                }
                weeksMin = if (min == Int.MAX_VALUE) null else min
                weeksMax = if (max == Int.MIN_VALUE) null else max
            } else {
                weeksMin = null
                weeksMax = null
            }
            val course = ImportedCourse(
                name = name,
                teacher = teacher,
                classroom = classroom,
                weekday = weekday,
                startSection = startSection,
                endSection = endSection,
                weekParity = weekParity,
                weeksMin = weeksMin,
                weeksMax = weeksMax,
            )
            if (course.isValid) {
                courses.add(course)
            } else {
                errors.add("第 ${i + 1} 条无效：名称/星期/节次范围不合法")
            }
        }
    } catch (e: JSONException) {
        return emptyList<ImportedCourse>() to listOf("JSON 格式错误：${e.message}")
    }
    if (courses.isEmpty() && errors.isEmpty()) {
        errors.add("未找到任何课程（缺少 courses 数组？）")
    }
    return courses to errors
}

/**
 * 把导入课程转换为本应用的周期日程：
 * - 日期范围 = 学期开始日对齐到该星期几后，按教学周首尾扩展（缺省按 16 周）；
 * - 时间 = 默认节次表中 起止节次 的时间；
 * - 备注 = 教师 · 周次。
 */
fun coursesToSchedules(
    courses: List<ImportedCourse>,
    termStart: LocalDate,
    periods: List<com.shiguang.app.core.SchedulePeriod> = SchedulePeriods.DEFAULT,
): List<ScheduleEntity> = courses.mapNotNull { course ->
    // 超出当前时间表节数的课程跳过（避免生成错误时间）
    if (course.startSection > periods.size || course.endSection > periods.size) {
        return@mapNotNull null
    }
    // 教学周映射：以“学期/教学周起点”所在周的周一为第 1 周的基准，
    // 第 k 周、星期 w 的日期 = 起点周周一 + (k-1)*7 + (w-1)。
    // 这样“第 1 周的周一/周三…”都落在同一周内，不会因为起点是周中而顺延到下周。
    val target = course.weekday // ISO 1=周一
    val week1Monday = DateUtils.startOfWeek(termStart)
    val first = week1Monday.plusDays((target - 1).toLong()) // 第 1 周该星期几

    val wMin = course.weeksMin ?: 1
    val wMax = course.weeksMax ?: 16
    val rangeStart = first.plusDays((wMin - 1) * 7L)
    val rangeEnd = first.plusDays((wMax - 1) * 7L)

    val startMinute = periods.getOrNull(course.startSection - 1)?.startMinute ?: 0
    val endMinute = periods.getOrNull(course.endSection - 1)?.endMinute ?: startMinute

    val note = buildString {
        if (course.teacher.isNotBlank()) append(course.teacher)
        if (course.weeksMin != null || course.weeksMax != null) {
            if (isNotEmpty()) append(" · ")
            append("第${course.weeksMin ?: 1}-${course.weeksMax ?: 16}周")
        }
        if (course.weekParity != WeekParity.ALL) {
            if (isNotEmpty()) append(" · ")
            append(WeekParity.label(course.weekParity))
        }
    }.ifBlank { null }

    ScheduleEntity(
        title = course.name,
        location = course.classroom.ifBlank { null },
        colorIndex = (course.weekday - 1) % 8,
        singleEpochDay = null,
        repeatStartEpochDay = rangeStart.toEpochDay(),
        repeatEndEpochDay = rangeEnd.toEpochDay(),
        weekdaysMask = 1 shl (course.weekday - 1),
        weekParity = course.weekParity,
        startMinute = startMinute,
        endMinute = endMinute,
        note = note,
    )
}