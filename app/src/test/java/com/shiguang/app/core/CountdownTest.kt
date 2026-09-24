package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CountdownTest {

    @Test
    fun `未来目标日 显示剩余天数`() {
        val today = LocalDate.of(2025, 6, 1)
        val target = LocalDate.of(2025, 6, 10)
        val display = Countdown.display(target, today)
        assertEquals(9L, display.days)
        assertEquals(CountdownDisplay.Upcoming::class, display::class)
    }

    @Test
    fun `目标日即今天`() {
        val today = LocalDate.of(2025, 6, 1)
        val display = Countdown.display(today, today)
        assertEquals(CountdownDisplay.Today::class, display::class)
        assertEquals(0L, display.days)
    }

    @Test
    fun `目标日已过 显示已过天数`() {
        val today = LocalDate.of(2025, 6, 10)
        val target = LocalDate.of(2025, 6, 1)
        val display = Countdown.display(target, today)
        assertEquals(CountdownDisplay.Passed::class, display::class)
        assertEquals(9L, display.days)
    }

    @Test
    fun `summary 文案`() {
        val today = LocalDate.of(2025, 6, 1)
        assertEquals("还有 9 天", Countdown.summary(Countdown.display(LocalDate.of(2025, 6, 10), today)))
        assertEquals("就是今天", Countdown.summary(Countdown.display(today, today)))
        assertEquals("已过 9 天", Countdown.summary(Countdown.display(LocalDate.of(2025, 5, 23), today)))
    }
}