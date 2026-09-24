package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 打卡日锚定模型测试：随时打卡，每 N 天一个窗口，超期缺卡，打卡重置。
 */
class HabitTimelineTest {

    // N = 3：打卡 1 -> 截止 4（含）；4 未打 -> 缺卡
    @Test
    fun `截止日当天含在内`() {
        assertEquals(4L, HabitTimeline.deadline(1, 3))
        // 截止日当天：due 且未缺卡
        assertTrue(HabitTimeline.dueToday(1, 4, 3))
        assertFalse(HabitTimeline.isMissed(1, 4, 3))
        // 超过截止日：缺卡
        assertTrue(HabitTimeline.isMissed(1, 5, 3))
    }

    @Test
    fun `无打卡记录时不计算过期`() {
        assertFalse(HabitTimeline.dueToday(null, 100, 3))
        assertFalse(HabitTimeline.isMissed(null, 100, 3))
        assertEquals(3, HabitTimeline.daysToDeadline(null, 100, 3))
    }

    @Test
    fun `每日习惯连续三天`() {
        val checked = listOf(1L, 2L, 3L)
        assertEquals(3, HabitTimeline.currentChainLength(checked, 3, 1))
        assertEquals(3, HabitTimeline.longestChainLength(checked, 1))
        assertEquals(0, HabitTimeline.missedCount(checked, 3, 1))
    }

    @Test
    fun `每3天：按时续打为连续 截止日当天打不算缺卡`() {
        val checked = listOf(1L, 4L) // 第3天(4=1+3)打卡，gap==N 合法
        assertEquals(2, HabitTimeline.currentChainLength(checked, 4, 3))
        assertEquals(0, HabitTimeline.missedCount(checked, 4, 3))
    }

    @Test
    fun `每3天：超期一次算一次缺卡 且当前连续为0`() {
        val checked = listOf(1L, 5L, 6L) // 1->5 gap 4 > 3：缺卡；5->6 gap 1：连续
        assertEquals(2, HabitTimeline.currentChainLength(checked, 6, 3))
        assertEquals(1, HabitTimeline.missedCount(checked, 6, 3))
        assertEquals(2, HabitTimeline.longestChainLength(checked, 3))
    }

    @Test
    fun `已缺卡状态当前连续为0 直到下次打卡重置`() {
        val checked = listOf(1L)
        assertEquals(0, HabitTimeline.currentChainLength(checked, 6, 3)) // 1 已超期
        assertEquals(1, HabitTimeline.missedCount(checked, 6, 3))       // 尾部逾期 1 次
        assertEquals(-2, HabitTimeline.daysToDeadline(1, 6, 3))

        val reset = listOf(1L, 6L) // 6 打卡后重新起算
        assertEquals(1, HabitTimeline.currentChainLength(reset, 6, 3))
        assertEquals(1, HabitTimeline.missedCount(reset, 6, 3)) // 历史缺卡仍统计
        assertEquals(3, HabitTimeline.daysToDeadline(6, 6, 3))  // 截止 = 6+3 = 9，还有 3 天
    }

    @Test
    fun `打卡更频繁则连续次数更多`() {
        val checked = listOf(1L, 2L, 4L, 8L) // 间隔 1,2,4(>3 断了)；最长段 [1,2,4]
        assertEquals(1, HabitTimeline.currentChainLength(checked, 8, 3)) // 最近一次 8 与 4 间隔 4>3 → 新链只有 8
        assertEquals(3, HabitTimeline.longestChainLength(checked, 3))
        assertEquals(1, HabitTimeline.missedCount(checked, 8, 3))
    }

    @Test
    fun `日历缺卡截止日集合`() {
        // N=3：打卡1 截止4；打卡4 截止7（4当天打，4处不标）；打卡10：从4到10 gap6>3 → 4+3=7 标红；
        // 10 的截止 13 未过（today=11）不标
        val checked = listOf(1L, 4L, 10L)
        assertEquals(setOf(7L), HabitTimeline.missedDeadlineDays(checked, 11, 3))
        // 尾部逾期：打卡1，today=8 → 截止4 已过且无下一次 → {4}
        assertEquals(setOf(4L), HabitTimeline.missedDeadlineDays(listOf(1L), 8, 3))
    }
}