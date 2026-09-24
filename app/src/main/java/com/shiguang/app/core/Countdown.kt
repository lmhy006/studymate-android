package com.shiguang.app.core

import java.time.LocalDate

/**
 * 倒数日展示逻辑（纯函数，可单测）。
 */
sealed interface CountdownDisplay {
    val days: Long

    /** 目标日在未来：还有 days 天。 */
    data class Upcoming(override val days: Long) : CountdownDisplay

    /** 目标日就是今天。 */
    data object Today : CountdownDisplay {
        override val days: Long = 0
    }

    /** 目标日已过：已过 days 天（days > 0）。 */
    data class Passed(override val days: Long) : CountdownDisplay
}

object Countdown {

    fun display(target: LocalDate, today: LocalDate): CountdownDisplay {
        val diff = target.toEpochDay() - today.toEpochDay()
        return when {
            diff > 0 -> CountdownDisplay.Upcoming(diff)
            diff == 0L -> CountdownDisplay.Today
            else -> CountdownDisplay.Passed(-diff)
        }
    }

    fun summary(display: CountdownDisplay): String = when (display) {
        is CountdownDisplay.Upcoming -> "还有 ${display.days} 天"
        is CountdownDisplay.Today -> "就是今天"
        is CountdownDisplay.Passed -> "已过 ${display.days} 天"
    }
}