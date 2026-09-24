package com.shiguang.app

import android.app.Application
import androidx.work.Configuration
import com.shiguang.app.data.AppContainer
import com.shiguang.app.data.AppSettings
import com.shiguang.app.widget.WidgetRefreshHelper

/**
 * 应用入口：持有手写依赖容器，初始化设置存储，并注册小组件每日刷新。
 * 实现 Configuration.Provider：在 WorkManager 默认初始化器不可用（如 Robolectric 测试、
 * 或其它进程裁剪场景）时自动完成初始化。
 */
class StudyMateApp : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        WidgetRefreshHelper.scheduleDaily(this)
        WidgetRefreshHelper.enqueue(this)
    }
}