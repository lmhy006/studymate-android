package com.shiguang.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.shiguang.app.MainActivity
import com.shiguang.app.core.Countdown
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.AppDatabase
import com.shiguang.app.data.entity.CountdownEntity

/**
 * 倒数日桌面小组件（Glance / AppWidget，按 1.1.1 实际 API 编写）。
 *
 * - 2x2（小方块）与 4x1（横条）两种响应式布局；
 * - 显示“置顶”的倒数日；无置顶时显示目标日最近的；
 * - 点按小组件打开 App；
 * - 由 WorkManager 每日刷新 + 系统日期/时间/时区变化时刷新。
 */
class CountdownWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp), // 约 2x2
            DpSize(250.dp, 110.dp), // 约 4x1
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entity = runCatching {
            AppDatabase.get(context).countdownDao().widgetCandidate()
        }.getOrNull()
        provideContent { WidgetContent(entity) }
    }
}

private val BACKGROUND = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF20222A))
private val TEXT_MAIN = ColorProvider(day = Color(0xFF17181D), night = Color(0xFFF2F3F5))
private val TEXT_MUTED = ColorProvider(day = Color(0xFF7A8090), night = Color(0xFFA9AFBC))
private val ACCENT = ColorProvider(day = Color(0xFF4460F0), night = Color(0xFF94A2FF))

@Composable
private fun WidgetContent(entity: CountdownEntity?) {
    val size = LocalSize.current
    val wide = size.width >= 220.dp
    val context = LocalContext.current
    val openApp = actionStartActivity(intent = Intent(context, MainActivity::class.java))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BACKGROUND)
            .padding(if (wide) 14.dp else 10.dp)
            .clickable(openApp),
        contentAlignment = Alignment.Center,
    ) {
        if (entity == null) {
            Text(
                text = "添加倒数日",
                style = TextStyle(color = TEXT_MUTED, fontSize = 12.sp),
            )
        } else if (wide) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        text = entity.title,
                        style = TextStyle(
                            color = TEXT_MAIN,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = DateUtils.formatFull(DateUtils.fromEpochDay(entity.targetEpochDay)),
                        style = TextStyle(color = TEXT_MUTED, fontSize = 11.sp),
                    )
                }
                Spacer(GlanceModifier.width(12.dp))
                DaysNumber(entity, alignEnd = true)
            }
        } else {
            Column(
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                DaysNumber(entity, alignEnd = false)
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = entity.title,
                    style = TextStyle(
                        color = TEXT_MAIN,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DaysNumber(entity: CountdownEntity, alignEnd: Boolean) {
    val display = Countdown.display(
        target = DateUtils.fromEpochDay(entity.targetEpochDay),
        today = DateUtils.today(),
    )
    Column(
        horizontalAlignment = if (alignEnd) {
            Alignment.Horizontal.End
        } else {
            Alignment.Horizontal.CenterHorizontally
        }
    ) {
        Text(
            text = display.days.toString(),
            style = TextStyle(
                color = ACCENT,
                fontSize = if (alignEnd) 28.sp else 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = Countdown.summary(display),
            style = TextStyle(color = TEXT_MUTED, fontSize = 10.sp, textAlign = TextAlign.Center),
        )
    }
}