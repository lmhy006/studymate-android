package com.shiguang.app.core

/**
 * DDL 计算逻辑（纯函数，可单测）：过期判定、剩余天数、自动释放时间。
 */
object DdlLogic {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** 已过期：当前时间晚于截止时间。 */
    fun isOverdue(dueAt: Long, now: Long): Boolean = dueAt < now

    /** 距截止还剩多少整天（向上取整）；已过期为负数（向上取整的负值）。 */
    fun daysUntilDue(dueAt: Long, now: Long): Int {
        val diff = dueAt - now
        return if (diff <= 0) {
            -(((-diff) + DAY_MS - 1) / DAY_MS).toInt()
        } else {
            ((diff + DAY_MS - 1) / DAY_MS).toInt()
        }
    }

    /** 完成 N 天后自动释放的时刻。 */
    fun releaseAt(completedAt: Long, releaseDays: Int): Long =
        completedAt + releaseDays.toLong() * DAY_MS

    /** 距自动释放还有几天（向上取整；<=0 表示已可释放）。 */
    fun daysUntilRelease(completedAt: Long, releaseDays: Int, now: Long): Int {
        val remain = releaseAt(completedAt, releaseDays) - now
        return if (remain <= 0) 0 else ((remain + DAY_MS - 1) / DAY_MS).toInt()
    }

    /** 是否已到释放时刻（>0 天自动删除）。 */
    fun shouldRelease(completedAt: Long, releaseDays: Int, now: Long): Boolean =
        now >= releaseAt(completedAt, releaseDays)
}