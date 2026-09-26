package com.shiguang.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 小组件刷新调度：
 * - 每日周期任务保证天数在跨天时更新（播种到次日 00:10，每次 App 启动重置）；
 * - 一次性任务用于数据变化/系统事件后的即时刷新。
 */
object WidgetRefreshHelper {

    private const val ONESHOT_WORK = "countdown-widget-refresh"
    private const val PERIODIC_WORK = "countdown-widget-daily"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<CountdownWidgetWorker>()
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    /** 播种每日周期任务：对齐到下一个本地 00:10，首次就发生在跨天之后的几分钟内。 */
    fun scheduleDaily(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(0, 10)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMs = (next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            System.currentTimeMillis())
            .coerceAtLeast(TimeUnit.MINUTES.toMillis(15))
        val request = PeriodicWorkRequestBuilder<CountdownWidgetWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

/** 刷新小组件的 Worker（被周期任务与一次性任务共用）。 */
class CountdownWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        CountdownWidget().updateAll(applicationContext)
        Result.success()
    }.getOrElse { Result.retry() }
}

/**
 * 系统事件接收器：开机、日期变化、时间设置、时区变化后刷新小组件。
 * 仅处理声明的系统动作，忽略其它广播。
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                WidgetRefreshHelper.scheduleDaily(context)
                WidgetRefreshHelper.enqueue(context)
            }
            else -> Unit
        }
    }
}