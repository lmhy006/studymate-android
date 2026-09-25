package com.shiguang.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 课程配色测试：同名同色、跨平台稳定、不同名尽量分散。
 */
class CourseColorTest {

    @Test
    fun `同名课程颜色一致`() {
        assertEquals(
            CourseColor.indexForTitle("线性代数"),
            CourseColor.indexForTitle("线性代数"),
        )
        assertEquals(
            CourseColor.indexForTitle(" 大学英语 "),
            CourseColor.indexForTitle("大学英语"), // 忽略首尾空格
        )
    }

    @Test
    fun `哈希稳定且在色盘范围内`() {
        val names = listOf("线性代数", "大学英语", "高等数学", "数据结构", "操作系统", "计算机网络", "数据库", "体育")
        names.forEach { name ->
            val idx = CourseColor.indexForTitle(name)
            assertTrue(idx in 0 until CourseColor.PALETTE_SIZE)
        }
        // 同一进程内两次调用结果一致（实现基于 String.hashCode，跨运行也稳定）
        assertEquals(CourseColor.indexForTitle("高等数学"), CourseColor.indexForTitle("高等数学"))
    }

    @Test
    fun `常见课程名尽量分散到不同颜色`() {
        val names = listOf("线性代数", "大学英语", "高等数学", "数据结构", "操作系统", "计算机网络", "数据库", "体育", "马原", "毛概")
        val indexes = names.map { CourseColor.indexForTitle(it) }
        // 12 色盘中 10 个不同课名，期望至少在 6 种以上不同颜色（避免大面积撞色）
        assertTrue("分散度不足: $indexes", indexes.distinct().size >= 6)
        // 至少存在不同课名落在不同颜色（非全同）
        assertNotEquals(indexes[0], indexes[1])
    }
}