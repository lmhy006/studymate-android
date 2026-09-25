package com.shiguang.app.data

import android.content.Context
import com.shiguang.app.data.repo.CountdownRepository
import com.shiguang.app.data.repo.DdlRepository
import com.shiguang.app.data.repo.HabitRepository
import com.shiguang.app.data.repo.ScheduleRepository
import com.shiguang.app.widget.WidgetRefreshHelper

/**
 * 极简手写依赖容器（不引入 Hilt，保持工程简洁）。
 */
class AppContainer(private val appContext: Context) {

    private val database: AppDatabase = AppDatabase.get(appContext)

    val countdownRepository: CountdownRepository = CountdownRepository(database.countdownDao())
    val habitRepository: HabitRepository = HabitRepository(database.habitDao())
    val scheduleRepository: ScheduleRepository = ScheduleRepository(database.scheduleDao())
    val ddlRepository: DdlRepository = DdlRepository(database.ddlDao())

    /** 数据变化后请求刷新桌面小组件。 */
    fun refreshWidget() = WidgetRefreshHelper.enqueue(appContext)
}