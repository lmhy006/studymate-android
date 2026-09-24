package com.shiguang.app.ui.schedule

import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.Recurrence
import com.shiguang.app.data.entity.ScheduleEntity
import java.time.LocalDate

/**
 * 已物化的日程“发生事件”（单次日程 + 周期日程展开后的具体某一天）。
 */
data class ScheduleOccurrence(
    val scheduleId: Long,
    val date: LocalDate,
    val title: String,
    val location: String?,
    val colorIndex: Int,
    val startMinute: Int,
    val endMinute: Int,
)

/** 把全部日程在 [rangeStart, rangeEnd]（含）区间内物化为发生事件列表。 */
fun materializeOccurrences(
    schedules: List<ScheduleEntity>,
    rangeStart: LocalDate,
    rangeEnd: LocalDate,
): List<ScheduleOccurrence> =
    schedules.flatMap { schedule ->
        val start = schedule.startMinute ?: 0
        val end = (schedule.endMinute ?: start).coerceAtLeast(start + 1)
        if (schedule.singleEpochDay != null) {
            val day = DateUtils.fromEpochDay(schedule.singleEpochDay)
            if (!day.isBefore(rangeStart) && !day.isAfter(rangeEnd)) {
                listOf(
                    ScheduleOccurrence(
                        scheduleId = schedule.id,
                        date = day,
                        title = schedule.title,
                        location = schedule.location,
                        colorIndex = schedule.colorIndex,
                        startMinute = start,
                        endMinute = end,
                    )
                )
            } else {
                emptyList()
            }
        } else if (schedule.repeatStartEpochDay != null && schedule.repeatEndEpochDay != null) {
            Recurrence.occurrencesBetween(
                ruleStart = DateUtils.fromEpochDay(schedule.repeatStartEpochDay),
                ruleEnd = DateUtils.fromEpochDay(schedule.repeatEndEpochDay),
                weekdaysMask = schedule.weekdaysMask,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
            ).map { day ->
                ScheduleOccurrence(
                    scheduleId = schedule.id,
                    date = day,
                    title = schedule.title,
                    location = schedule.location,
                    colorIndex = schedule.colorIndex,
                    startMinute = start,
                    endMinute = end,
                )
            }
        } else {
            emptyList()
        }
    }