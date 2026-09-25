package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DDL 截止/释放计算测试。
 * 用固定“毫秒”构造时间：DAY = 24h。
 */
class DdlLogicTest {

    private val DAY = 24L * 60 * 60 * 1000

    @Test
    fun `过期判定`() {
        assertFalse(DdlLogic.isOverdue(1000, 999))
        assertFalse(DdlLogic.isOverdue(1000, 1000)) // 截止时刻前一刻不算过期？相等不算
        assertTrue(DdlLogic.isOverdue(1000, 1001))
    }

    @Test
    fun `距截止天数向上取整`() {
        assertEquals(3, DdlLogic.daysUntilDue(now + 3 * DAY, now))
        assertEquals(1, DdlLogic.daysUntilDue(now + DAY / 2, now))
        assertEquals(1, DdlLogic.daysUntilDue(now + 1000, now)) // 不足一天按 1 天（向上取整）
        assertEquals(-1, DdlLogic.daysUntilDue(now - DAY / 2, now))
        assertEquals(-2, DdlLogic.daysUntilDue(now - 2 * DAY, now))
    }

    @Test
    fun `自动释放时间与剩余天数`() {
        val done = now
        assertEquals(done + 3 * DAY, DdlLogic.releaseAt(done, 3))
        assertEquals(2, DdlLogic.daysUntilRelease(done, 3, now + DAY))
        assertEquals(0, DdlLogic.daysUntilRelease(done, 3, now + 3 * DAY))
        assertFalse(DdlLogic.shouldRelease(done, 3, now + 3 * DAY - 1))
        assertTrue(DdlLogic.shouldRelease(done, 3, now + 3 * DAY))
    }

    private val now = 1_700_000_000_000L
}