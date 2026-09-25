package com.shiguang.app.core

import java.time.LocalDate

/**
 * 单双周（教学周奇偶）支持。
 * weekParity：0=每周（全部）、1=单周、2=双周。
 * 教学周序号以“规则起点所在周的周一”为第 1 周计算（与教学周起点对齐一致）。
 */
object WeekParity {

    const val ALL = 0
    const val ODD = 1
    const val EVEN = 2

    fun matches(parity: Int, weekNumber: Int): Boolean = when (parity) {
        ALL -> true
        ODD -> weekNumber % 2 == 1
        EVEN -> weekNumber % 2 == 0
        else -> true
    }

    /** 相对规则起点所在周周一的教学周序号（1 起）；早于起点按负周处理。 */
    fun weekNumber(date: LocalDate, ruleStart: LocalDate): Int {
        val weeks =
            (DateUtils.startOfWeek(date).toEpochDay() - DateUtils.startOfWeek(ruleStart).toEpochDay()) / 7
        return (weeks + 1).toInt()
    }

    fun label(parity: Int): String = when (parity) {
        ODD -> "单周"
        EVEN -> "双周"
        else -> ""
    }
}