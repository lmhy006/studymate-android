package com.shiguang.app.core

/**
 * 习惯打卡时间线（“打卡日锚定”模型，纯函数，可单测）。
 *
 * 规则（用户定义）：
 * - 可以随时打卡（每个自然日最多一次）；
 * - 任意一次打卡后，N 天内再打卡即视为连续；
 * - 超过 N 天未打卡记一次缺卡；下一次打卡后重新起算 N 天。
 *
 * 约定：epochDay 为当日零点；截止日“含当天”（第 N 天打卡仍算连续，超过 N 天才算缺卡）。
 */
object HabitTimeline {

    /** 某次打卡的截止日（当天含）：lastCheck + interval。 */
    fun deadline(lastCheckEpochDay: Long, intervalDays: Int): Long =
        lastCheckEpochDay + intervalDays

    /** 今天是否已到截止日且尚未打卡（今天必须打卡，否则将缺卡）。无记录返回 false。 */
    fun dueToday(lastCheckEpochDay: Long?, today: Long, intervalDays: Int): Boolean =
        lastCheckEpochDay != null && today >= deadline(lastCheckEpochDay, intervalDays)

    /** 是否已经缺卡：晚于截止日仍未打卡。 */
    fun isMissed(lastCheckEpochDay: Long?, today: Long, intervalDays: Int): Boolean =
        lastCheckEpochDay != null && today > deadline(lastCheckEpochDay, intervalDays)

    /** 距截止日还有几天（>=0 表示未缺卡；<0 表示已缺卡）。 */
    fun daysToDeadline(lastCheckEpochDay: Long?, today: Long, intervalDays: Int): Int =
        if (lastCheckEpochDay == null) {
            intervalDays
        } else {
            (deadline(lastCheckEpochDay, intervalDays) - today).toInt()
        }

    /**
     * 当前连续达标次数：从最近一次打卡往前数，相邻打卡间隔 ≤ N 的一段；
     * 若已缺卡（最近一次打卡也逾期），返回 0。
     */
    fun currentChainLength(checked: List<Long>, today: Long, intervalDays: Int): Int {
        if (checked.isEmpty()) return 0
        val sorted = checked.sortedDescending()
        val last = sorted.first()
        if (isMissed(last, today, intervalDays)) return 0
        var count = 1
        var prev = last
        for (cur in sorted.drop(1)) {
            if (prev - cur <= intervalDays) {
                count++
                prev = cur
            } else {
                break
            }
        }
        return count
    }

    /** 历史最长连续达标次数（相邻打卡间隔 ≤ N）。 */
    fun longestChainLength(checked: List<Long>, intervalDays: Int): Int {
        if (checked.isEmpty()) return 0
        var best = 1
        var run = 1
        val sorted = checked.sorted()
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] <= intervalDays) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return best
    }

    /** 缺卡次数：相邻打卡间隔超过 N 的次数，加上“距今天”的逾期（未逾期不计）。 */
    fun missedCount(checked: List<Long>, today: Long, intervalDays: Int): Int {
        if (checked.isEmpty()) return 0
        val sorted = checked.sorted()
        var missed = 0
        for (i in 1 until sorted.size) {
            if (sorted[i] - sorted[i - 1] > intervalDays) missed++
        }
        val last = sorted.last()
        if (isMissed(last, today, intervalDays)) missed++
        return missed
    }

    /**
     * 日历上标记为“缺卡”的截止日集合：
     * 某次打卡的截止日当天已过、且截止前没有下一次打卡，则该截止日标红。
     */
    fun missedDeadlineDays(checked: List<Long>, today: Long, intervalDays: Int): Set<Long> {
        if (checked.isEmpty()) return emptySet()
        val sorted = checked.sorted()
        val result = mutableSetOf<Long>()
        for (i in sorted.indices) {
            val c = sorted[i]
            val deadline = deadline(c, intervalDays)
            val next = sorted.getOrNull(i + 1)
            val missed = if (next == null) {
                deadline < today
            } else {
                next > deadline
            }
            if (missed) result.add(deadline)
        }
        return result
    }
}