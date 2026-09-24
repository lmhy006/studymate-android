package com.shiguang.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `compare 基础比较`() {
        assertEquals(0, AppVersion.compare("1.0.0", "1.0.0"))
        assertEquals(-1, AppVersion.compare("1.0.0", "1.0.1"))
        assertEquals(1, AppVersion.compare("1.0.1", "1.0.0"))
        assertEquals(-1, AppVersion.compare("1.9.9", "2.0.0"))
        assertEquals(1, AppVersion.compare("2", "1.9.9"))
    }

    @Test
    fun `compare 兼容 v 前缀与缺位段`() {
        assertEquals(0, AppVersion.compare("v1.0.0", "1.0.0"))
        assertEquals(-1, AppVersion.compare("v1.0", "1.0.1"))
        assertEquals(0, AppVersion.compare("1.0", "1.0.0"))
        assertEquals(1, AppVersion.compare("v1.0.1", "1.0"))
    }

    @Test
    fun `hasUpdate 判定`() {
        assertFalse(AppVersion.hasUpdate("1.0.0", "1.0.0"))
        assertTrue(AppVersion.hasUpdate("1.0.0", "1.0.1"))
        assertTrue(AppVersion.hasUpdate("1.0.0", "v1.1.0"))
        assertFalse(AppVersion.hasUpdate("1.0.1", "1.0.0"))
    }
}