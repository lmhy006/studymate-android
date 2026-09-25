package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekParityTest {

    private val d = { s: String -> LocalDate.parse(s) }

    @Test
    fun `matches 奇偶`() {
        assertTrue(WeekParity.matches(WeekParity.ALL, 1))
        assertTrue(WeekParity.matches(WeekParity.ALL, 2))
        assertTrue(WeekParity.matches(WeekParity.ODD, 1))
        assertFalse(WeekParity.matches(WeekParity.ODD, 2))
        assertTrue(WeekParity.matches(WeekParity.EVEN, 2))
        assertFalse(WeekParity.matches(WeekParity.EVEN, 1))
    }

    @Test
    fun `weekNumber 以规则起点所在周周一起算`() {
        val ruleStart = d("2025-09-01") // 周一
        assertEquals(1, WeekParity.weekNumber(d("2025-09-01"), ruleStart))
        assertEquals(1, WeekParity.weekNumber(d("2025-09-05"), ruleStart)) // 周五仍在第1周
        assertEquals(2, WeekParity.weekNumber(d("2025-09-08"), ruleStart))
        assertEquals(3, WeekParity.weekNumber(d("2025-09-15"), ruleStart))
        // 起点为周中：以起点所在周周一为第1周
        val mid = d("2025-09-03") // 周三
        assertEquals(1, WeekParity.weekNumber(d("2025-09-01"), mid))
        assertEquals(1, WeekParity.weekNumber(d("2025-09-03"), mid))
        assertEquals(2, WeekParity.weekNumber(d("2025-09-08"), mid))
    }

    @Test
    fun `label 文案`() {
        assertEquals("", WeekParity.label(WeekParity.ALL))
        assertEquals("单周", WeekParity.label(WeekParity.ODD))
        assertEquals("双周", WeekParity.label(WeekParity.EVEN))
    }
}