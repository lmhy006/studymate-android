package com.shiguang.app.ui.schedule

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.app.StudyMateApp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.WeekdayMask
import com.shiguang.app.data.entity.ScheduleEntity
import com.shiguang.app.ui.components.EmptyHint
import com.shiguang.app.ui.components.MonthCalendar
import com.shiguang.app.ui.theme.scheduleColor
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private const val VIEW_WEEK = 0
private const val VIEW_MONTH = 1

private const val HOUR_START = 6
private const val HOUR_END = 23
private val HOUR_HEIGHT = 52.dp

/** 点击周视图空白处预填的“新建日程”数据。 */
data class PrefillData(val date: LocalDate, val minute: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = viewModel {
        ScheduleViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var viewMode by rememberSaveable { mutableIntStateOf(VIEW_WEEK) }
    var weekAnchorStr by rememberSaveable { mutableStateOf(DateUtils.today().toString()) }
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var editing by remember { mutableStateOf<ScheduleEntity?>(null) }
    var prefill by remember { mutableStateOf<PrefillData?>(null) }
    var detail by remember { mutableStateOf<ScheduleOccurrence?>(null) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // ---- 弹层 ----
    if (editing != null || prefill != null) {
        ScheduleEditSheet(
            editing = editing,
            prefill = prefill,
            onDismiss = {
                editing = null
                prefill = null
            },
            onSave = { entity ->
                viewModel.save(entity)
                editing = null
                prefill = null
                notify("已保存")
            },
            onDelete = { entity ->
                viewModel.delete(entity)
                editing = null
                prefill = null
                notify("已删除")
            },
        )
    }

    detail?.let { occurrence ->
        val source = state.all.firstOrNull { it.id == occurrence.scheduleId }
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(occurrence.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${DateUtils.formatMonthDay(occurrence.date)} ${DateUtils.weekdayName(occurrence.date)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "${DateUtils.timeText(occurrence.startMinute)} - ${DateUtils.timeText(occurrence.endMinute)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    occurrence.location?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (source?.isRecurring == true) {
                            "周期日程：${DateUtils.formatMonthDay(DateUtils.fromEpochDay(source.repeatStartEpochDay!!))} - " +
                                "${DateUtils.formatMonthDay(DateUtils.fromEpochDay(source.repeatEndEpochDay!!))}，每" +
                                WeekdayMask.names(source.weekdaysMask)
                        } else {
                            "单次日程"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { detail = null }) { Text("关闭") }
            },
            dismissButton = {
                if (source != null) {
                    TextButton(
                        onClick = {
                            viewModel.delete(source)
                            detail = null
                            notify("已删除")
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    // ---- 主界面 ----
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "日程",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = viewMode == VIEW_WEEK,
                            onClick = { viewMode = VIEW_WEEK },
                            label = { Text("周视图") },
                        )
                        FilterChip(
                            selected = viewMode == VIEW_MONTH,
                            onClick = { viewMode = VIEW_MONTH },
                            label = { Text("月视图") },
                        )
                    }
                }
            }

            if (viewMode == VIEW_WEEK) {
                val week = DateUtils.weekOf(LocalDate.parse(weekAnchorStr))
                val occurrences = materializeOccurrences(state.all, week.first(), week.last())

                WeekMode(
                    week = week,
                    occurrences = occurrences,
                    onPrev = { weekAnchorStr = week.first().minusDays(7).toString() },
                    onNext = { weekAnchorStr = week.first().plusDays(7).toString() },
                    onToday = { weekAnchorStr = DateUtils.today().toString() },
                    onEventClick = { detail = it },
                    onSlotClick = { date, minute -> prefill = PrefillData(date, minute) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                val month = YearMonth.parse(monthStr)
                val monthStart = month.atDay(1)
                val monthEnd = month.atEndOfMonth()
                val occurrences = materializeOccurrences(state.all, monthStart, monthEnd)
                val selected = selectedDay ?: DateUtils.today()
                val dayEvents = materializeOccurrences(state.all, selected, selected)

                MonthMode(
                    month = month,
                    occurrences = occurrences,
                    selectedDay = selected,
                    dayEvents = dayEvents,
                    onPrev = { monthStr = month.minusMonths(1).toString() },
                    onNext = { monthStr = month.plusMonths(1).toString() },
                    onToday = { monthStr = YearMonth.now().toString(); selectedDay = null },
                    onSelectDay = { selectedDay = it },
                    onEventClick = { detail = it },
                    onAddForDay = { date -> prefill = PrefillData(date, 8 * 60) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FloatingActionButton(
            onClick = { prefill = PrefillData(DateUtils.today(), 8 * 60) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加日程")
        }

        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 周视图（课表式）
// ---------------------------------------------------------------------------

@Composable
private fun WeekMode(
    week: List<LocalDate>,
    occurrences: List<ScheduleOccurrence>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onEventClick: (ScheduleOccurrence) -> Unit,
    onSlotClick: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = DateUtils.today()
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一周")
            }
            Text(
                text = "${week.first().monthValue}月${week.first().dayOfMonth}日 - " +
                    "${week.last().monthValue}月${week.last().dayOfMonth}日",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一周")
            }
            TextButton(onClick = onToday) { Text("本周") }
        }

        // 表头：周一..周日
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(44.dp))
            week.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = DateUtils.weekdayName(day),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day == today) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (day == today) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 时间轴
            Column(Modifier.width(44.dp)) {
                (HOUR_START until HOUR_END).forEach { hour ->
                    Text(
                        text = DateUtils.timeText(hour * 60),
                        modifier = Modifier.height(HOUR_HEIGHT),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 每天一列
            week.forEach { day ->
                val dayEvents = occurrences.filter { it.date == day }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(HOUR_HEIGHT * (HOUR_END - HOUR_START))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .pointerInput(day) {
                            val hourHeightPx = HOUR_HEIGHT.toPx()
                            detectTapGestures { offset ->
                                val minutesPerHour = 60f
                                val tappedMinute =
                                    HOUR_START * 60 +
                                        ((offset.y / hourHeightPx) * minutesPerHour)
                                            .toInt()
                                            .coerceIn(HOUR_START * 60, (HOUR_END - 1) * 60)
                                onSlotClick(day, tappedMinute)
                            }
                        }
                ) {
                    // 小时分隔线
                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    Canvas(Modifier.fillMaxSize()) {
                        repeat(HOUR_END - HOUR_START - 1) { i ->
                            val y = (i + 1) * HOUR_HEIGHT.toPx()
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f,
                            )
                        }
                    }
                    // 日程块
                    dayEvents.forEach { event ->
                        val startClamped = event.startMinute.coerceIn(HOUR_START * 60, HOUR_END * 60)
                        val endClamped = event.endMinute.coerceIn(HOUR_START * 60, HOUR_END * 60)
                        val top = HOUR_HEIGHT * ((startClamped - HOUR_START * 60) / 60f)
                        val heightDp = HOUR_HEIGHT * ((endClamped - startClamped).coerceAtLeast(15) / 60f)
                        Surface(
                            onClick = { onEventClick(event) },
                            modifier = Modifier
                                .offset(y = top)
                                .fillMaxWidth()
                                .height(if (heightDp < 20.dp) 20.dp else heightDp)
                                .padding(horizontal = 1.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = scheduleColor(event.colorIndex),
                        ) {
                            Column(Modifier.padding(horizontal = 5.dp, vertical = 3.dp)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${DateUtils.timeText(event.startMinute)}–${DateUtils.timeText(event.endMinute)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 月视图
// ---------------------------------------------------------------------------

@Composable
private fun MonthMode(
    month: YearMonth,
    occurrences: List<ScheduleOccurrence>,
    selectedDay: LocalDate,
    dayEvents: List<ScheduleOccurrence>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onEventClick: (ScheduleOccurrence) -> Unit,
    onAddForDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一月")
            }
            Text(
                text = month.format(DateUtils.YEAR_MONTH),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一月")
            }
            TextButton(onClick = onToday) { Text("本月") }
        }

        MonthCalendar(
            month = month,
            today = DateUtils.today(),
            modifier = Modifier.fillMaxWidth(),
            mark = { date ->
                val count = occurrences.count { it.date == date }
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
            onDayClick = { onSelectDay(it) },
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${DateUtils.formatMonthDay(selectedDay)} ${DateUtils.weekdayName(selectedDay)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { onAddForDay(selectedDay) }) { Text("+ 添加") }
        }

        if (dayEvents.isEmpty()) {
            EmptyHint("这一天没有日程\n点“+ 添加”新建，或使用右下角 + 新建周期日程")
        } else {
            dayEvents.sortedBy { it.startMinute }.forEach { event ->
                DayEventRow(event = event, onClick = { onEventClick(event) })
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun DayEventRow(event: ScheduleOccurrence, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(scheduleColor(event.colorIndex))
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                event.location?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${DateUtils.timeText(event.startMinute)}–${DateUtils.timeText(event.endMinute)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}