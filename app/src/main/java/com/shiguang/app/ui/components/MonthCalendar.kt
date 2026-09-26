package com.shiguang.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import java.time.LocalDate
import java.time.YearMonth

/**
 * 通用月历（周一开头，6 行 42 格）。
 * @param highlight 返回 true 的日期用第三强调色圈出（如倒数日当天，与今日的主色区分）。
 * mark：在每个日期下方绘制的标记（打卡圆点 / 日程计数等），高度 6dp。
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    today: LocalDate,
    modifier: Modifier = Modifier,
    highlight: ((LocalDate) -> Boolean)? = null,
    mark: (@Composable (date: LocalDate) -> Unit)? = null,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            DateUtils.WEEKDAY_NAMES.forEach { name ->
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        DateUtils.monthGrid(month).chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val inMonth = date.year == month.year && date.monthValue == month.monthValue
                    val isToday = date == today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .then(if (onDayClick != null) Modifier.clickable { onDayClick(date) } else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday -> MaterialTheme.colorScheme.primary
                                            highlight?.invoke(date) == true ->
                                                MaterialTheme.colorScheme.tertiary
                                            else -> Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isToday -> MaterialTheme.colorScheme.onPrimary
                                        highlight?.invoke(date) == true ->
                                            MaterialTheme.colorScheme.onTertiary
                                        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                            Box(Modifier.height(6.dp)) {
                                if (mark != null) {
                                    mark(date)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}