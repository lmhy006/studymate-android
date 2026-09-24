package com.shiguang.app.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 纯日期工具（不依赖 Android，可单测）。
 * 约定：一周从周一开始（ISO-8601），时间用“当日分钟数”表示。
 */
object DateUtils {

    val MONTH_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
    val FULL_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    val YEAR_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月")

    val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun today(): LocalDate = LocalDate.now()

    fun toEpochDay(d: LocalDate): Long = d.toEpochDay()

    fun fromEpochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    /** 所在周（周一为首日）的第一天。 */
    fun startOfWeek(anchor: LocalDate): LocalDate {
        var d = anchor
        while (d.dayOfWeek != DayOfWeek.MONDAY) d = d.minusDays(1)
        return d
    }

    /** 所在周的 7 天（周一到周日）。 */
    fun weekOf(anchor: LocalDate): List<LocalDate> {
        val start = startOfWeek(anchor)
        return (0..6).map { start.plusDays(it.toLong()) }
    }

    /** 当月日历的 42 个格子（6 行 7 列，含前后月补位，以周一开头）。 */
    fun monthGrid(year: Int, month: Int): List<LocalDate> {
        val first = LocalDate.of(year, month, 1)
        val start = startOfWeek(first)
        return (0 until 42).map { start.plusDays(it.toLong()) }
    }

    fun monthGrid(month: YearMonth): List<LocalDate> = monthGrid(month.year, month.monthValue)

    fun weekdayName(d: LocalDate): String = WEEKDAY_NAMES[d.dayOfWeek.value - 1]

    /** "周一"/"周二"… 的索引（0..6），与 [WeekdayMask] 的位对应。 */
    fun weekdayIndex(d: LocalDate): Int = d.dayOfWeek.value - 1

    /** LocalDate -> UTC 零点的 epochMillis（Material3 DatePicker 使用 UTC 毫秒）。 */
    fun toUtcMillis(d: LocalDate): Long = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** UTC 零点的 epochMillis -> LocalDate。 */
    fun fromUtcMillis(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

    fun formatMonthDay(d: LocalDate): String = d.format(MONTH_DAY)

    fun formatFull(d: LocalDate): String = d.format(FULL_DATE)

    /** 当日分钟数 -> "HH:mm"。 */
    fun timeText(minuteOfDay: Int): String {
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}