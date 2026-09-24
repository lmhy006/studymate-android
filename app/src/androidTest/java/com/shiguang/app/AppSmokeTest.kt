package com.shiguang.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 应用启动冒烟测试（androidTest，真机/模拟器运行）。
 * 验证 MainActivity 能正常启动并进入 RESUMED 状态（同屏覆盖：主题初始化、设置存储、WorkManager 按需初始化）。
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @Test
    fun appLaunchesToResumedState() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}