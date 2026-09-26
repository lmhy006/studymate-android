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
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.app.StudyMateApp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.SchedulePeriod
import com.shiguang.app.core.SchedulePeriods
import com.shiguang.app.core.WeekParity
import com.shiguang.app.core.WeekdayMask
import com.shiguang.app.data.AppSettings
import com.shiguang.app.data.entity.DdlEntity
import com.shiguang.app.data.entity.ScheduleEntity
import com.shiguang.app.ui.components.EmptyHint
import com.shiguang.app.ui.components.MonthCalendar
import com.shiguang.app.ui.countdown.CountdownViewModel
import com.shiguang.app.ui.ddl.DdlEditSheet
import com.shiguang.app.ui.ddl.DdlView
import com.shiguang.app.ui.ddl.DdlViewModel
import com.shiguang.app.ui.theme.scheduleColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val VIEW_WEEK = 0
private const val VIEW_MONTH = 1
private const val VIEW_DDL = 2

/** 周视图每个节次行的高度（压缩后一屏尽量放下 13 节）。 */
private val PERIOD_ROW_HEIGHT = 40.dp

/** 周视图左侧节次轴宽度（窄化，时间拆两行）。 */
private val AXIS_WIDTH = 46.dp

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
    ddlViewModel: DdlViewModel = viewModel {
        DdlViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
    countdownViewModel: CountdownViewModel = viewModel {
        CountdownViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ddlState by ddlViewModel.uiState.collectAsStateWithLifecycle()
    val countdownItems by countdownViewModel.items.collectAsStateWithLifecycle()
    // 倒数日当天（epochDay 集合），用于日程高亮
    val countdownDays = remember(countdownItems) { countdownItems.map { it.targetEpochDay }.toSet() }

    // 日程显示设置（设置 → 日程设置）
    val showSaturday by AppSettings.showSaturday.collectAsStateWithLifecycle()
    val showSunday by AppSettings.showSunday.collectAsStateWithLifecycle()
    val highlightToday by AppSettings.highlightToday.collectAsStateWithLifecycle()
    val showBorder by AppSettings.showBorder.collectAsStateWithLifecycle()
    val showDivider by AppSettings.showDivider.collectAsStateWithLifecycle()
    val timeTable by AppSettings.timeTable.collectAsStateWithLifecycle()
    val highlightCountdown by AppSettings.highlightCountdown.collectAsStateWithLifecycle()

    var viewMode by rememberSaveable { mutableIntStateOf(VIEW_WEEK) }
    var weekAnchorStr by rememberSaveable { mutableStateOf(DateUtils.today().toString()) }
    var monthStr by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }

    // 周切换（头部箭头 + FAB 上方双箭头共用同一逻辑）
    val moveWeek: (Long) -> Unit = { delta ->
        val anchor = DateUtils.weekOf(LocalDate.parse(weekAnchorStr)).first()
        weekAnchorStr = anchor.plusDays(delta).toString()
    }

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var editing by remember { mutableStateOf<ScheduleEntity?>(null) }
    var prefill by remember { mutableStateOf<PrefillData?>(null) }
    var detail by remember { mutableStateOf<ScheduleOccurrence?>(null) }
    var ddlEditing by remember { mutableStateOf<DdlEntity?>(null) }
    var ddlAddPrefill by remember { mutableStateOf<String?>(null) } // 预选关联课程名

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

    // ---- DDL 弹层 ----
    if (ddlEditing != null || ddlAddPrefill != null) {
        DdlEditSheet(
            editing = ddlEditing,
            prefillCourseName = ddlAddPrefill,
            courseNames = ddlState.courseNames,
            onDismiss = {
                ddlEditing = null
                ddlAddPrefill = null
            },
            onSave = { ddl ->
                ddlViewModel.save(ddl)
                ddlEditing = null
                ddlAddPrefill = null
                notify("已保存")
            },
            onDelete = { ddl ->
                ddlViewModel.delete(ddl)
                ddlEditing = null
                ddlAddPrefill = null
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
                    source?.note?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (source?.isRecurring == true) {
                            "周期日程：${DateUtils.formatMonthDay(DateUtils.fromEpochDay(source.repeatStartEpochDay!!))} - " +
                                "${DateUtils.formatMonthDay(DateUtils.fromEpochDay(source.repeatEndEpochDay!!))}，每" +
                                WeekdayMask.names(source.weekdaysMask) +
                                (if (source.weekParity != WeekParity.ALL) "，${WeekParity.label(source.weekParity)}" else "")
                        } else {
                            "单次日程"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // 关联 DDL
                    if (source != null && source.title.isNotBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        val related = ddlState.all.filter { it.courseName == source.title }
                        Text(
                            text = "关联 DDL（${related.size}）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (related.isEmpty()) {
                            Text(
                                text = "该课程暂无 DDL",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            related.take(6).forEach { ddl ->
                                Text(
                                    text = buildString {
                                        if (ddl.completed) append("✓ ")
                                        append(ddl.title)
                                        append("（截止 ${DateUtils.formatDateTime(ddl.dueAt)}")
                                        if (ddl.completed) append(" 已完成") else append(" 未完成")
                                        append("）")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!ddl.completed && com.shiguang.app.core.DdlLogic.isOverdue(ddl.dueAt, System.currentTimeMillis())) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            }
                            if (related.size > 6) {
                                Text(
                                    text = "…… 共 ${related.size} 个，请在「DDL」视图查看",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                detail = null
                                ddlAddPrefill = source.title
                            }
                        ) {
                            Text("为该课程添加 DDL")
                        }
                    }
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
                        FilterChip(
                            selected = viewMode == VIEW_DDL,
                            onClick = { viewMode = VIEW_DDL },
                            label = { Text("DDL") },
                        )
                    }
                }
            }

            when (viewMode) {
                VIEW_WEEK -> {
                    val week = DateUtils.weekOf(LocalDate.parse(weekAnchorStr))
                    val occurrences = materializeOccurrences(state.all, week.first(), week.last())

                    WeekMode(
                        week = week,
                        occurrences = occurrences,
                        periods = timeTable,
                        countdownDays = countdownDays,
                        highlightCountdown = highlightCountdown,
                        showSaturday = showSaturday,
                        showSunday = showSunday,
                        highlightToday = highlightToday,
                        showBorder = showBorder,
                        showDivider = showDivider,
                        onPrev = { moveWeek(-7) },
                        onNext = { moveWeek(7) },
                        onToday = { weekAnchorStr = DateUtils.today().toString() },
                        onEventClick = { detail = it },
                        onSlotClick = { date, minute -> prefill = PrefillData(date, minute) },
                        modifier = Modifier.weight(1f),
                    )
                }
                VIEW_DDL -> {
                    DdlView(
                        state = ddlState,
                        onCardClick = { ddlEditing = it },
                        onToggleCompleted = { ddl, completed ->
                            ddlViewModel.toggleCompleted(ddl, completed)
                        },
                        onAddDdl = { ddlAddPrefill = null },
                        modifier = Modifier.weight(1f),
                    )
                }
                else -> {
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
                        countdownDays = countdownDays,
                        highlightCountdown = highlightCountdown,
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
        }

        FloatingActionButton(
            onClick = {
                if (viewMode == VIEW_DDL) {
                    ddlAddPrefill = null
                } else {
                    prefill = PrefillData(DateUtils.today(), 8 * 60)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加日程")
        }

        // 仿 BIT101：FAB 上方叠放 左箭头（上一周）/ 右箭头（下一周），快捷切换周
        if (viewMode == VIEW_WEEK) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SmallFloatingActionButton(onClick = { moveWeek(7) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "下一周")
                }
                SmallFloatingActionButton(onClick = { moveWeek(-7) }) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "上一周")
                }
            }
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
    periods: List<SchedulePeriod>,
    countdownDays: Set<Long>,
    highlightCountdown: Boolean,
    showSaturday: Boolean,
    showSunday: Boolean,
    highlightToday: Boolean,
    showBorder: Boolean,
    showDivider: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onEventClick: (ScheduleOccurrence) -> Unit,
    onSlotClick: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = DateUtils.today()
    val visibleDays = week.filter { day ->
        when (day.dayOfWeek) {
            DayOfWeek.SATURDAY -> showSaturday
            DayOfWeek.SUNDAY -> showSunday
            else -> true
        }
    }
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

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

        // 表头：仅显示可见的星期；倒数日当天用第三强调色（与今日主色区分）
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(AXIS_WIDTH))
            visibleDays.forEach { day ->
                val isToday = day == today
                val isCountdown = highlightCountdown && !isToday && day.toEpochDay() in countdownDays
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = DateUtils.weekdayName(day),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isToday -> primary
                            isCountdown -> tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (isToday || isCountdown) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isToday -> primary
                            isCountdown -> tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(if (isCountdown) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .background(tertiary),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        if (visibleDays.isEmpty()) {
            EmptyHint("周六、周日都被隐藏了\n在 设置 → 日程设置 中调整")
            return@Column
        }

        Row(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 节次轴：第N节 + 时间拆两行（上课 / -下课），窄栏
            Column(Modifier.width(AXIS_WIDTH)) {
                periods.forEach { period ->
                    Column(
                        modifier = Modifier.height(PERIOD_ROW_HEIGHT),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "第${period.number}节",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = DateUtils.timeText(period.startMinute),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "-${DateUtils.timeText(period.endMinute)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // 每天一列：节次网格
            visibleDays.forEach { day ->
                val dayEvents = occurrences.filter { it.date == day }
                val isToday = day == today
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(PERIOD_ROW_HEIGHT * periods.size)
                        .then(if (showBorder) Modifier.border(1.dp, outlineVariant) else Modifier)
                        .background(
                            if (highlightToday && isToday) primary.copy(alpha = 0.06f)
                            else Color.Transparent
                        )
                        .pointerInput(day) {
                            val rowHeightPx = PERIOD_ROW_HEIGHT.toPx()
                            detectTapGestures { offset ->
                                val row = (offset.y / rowHeightPx).toInt()
                                    .coerceIn(0, periods.lastIndex)
                                onSlotClick(day, periods[row].startMinute)
                            }
                        }
                ) {
                    // 节次分界线
                    if (showDivider) {
                        val lineColor = outlineVariant
                        Canvas(Modifier.fillMaxSize()) {
                            for (i in 1 until periods.size) {
                                val y = i * PERIOD_ROW_HEIGHT.toPx()
                                drawLine(
                                    color = lineColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                )
                            }
                        }
                    }
                    // 日程块：按起止时间吸附到节次行
                    dayEvents.forEach { event ->
                        val range = SchedulePeriods.periodRange(event.startMinute, event.endMinute, periods)
                        val rowSpan = range.last - range.first + 1
                        Surface(
                            onClick = { onEventClick(event) },
                            modifier = Modifier
                                .offset(y = PERIOD_ROW_HEIGHT * range.first)
                                .fillMaxWidth()
                                .height(PERIOD_ROW_HEIGHT * rowSpan - 2.dp)
                                .padding(horizontal = 1.dp),
                            shape = RoundedCornerShape(5.dp),
                            color = scheduleColor(event.colorIndex),
                        ) {
                            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(
                                    text = event.title,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = if (rowSpan >= 2) 3 else 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                event.location?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (rowSpan >= 2) {
                                    Text(
                                        text = "${DateUtils.timeText(event.startMinute)}-${DateUtils.timeText(event.endMinute)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                    // 当前时间横线（黑色，仿 BIT101）
                    val dayStartMinute = periods.first().startMinute
                    val dayEndMinute = periods.last().endMinute
                    val nowMinute = DateUtils.fromEpochMillis(System.currentTimeMillis()).hour * 60 +
                        DateUtils.fromEpochMillis(System.currentTimeMillis()).minute
                    if (nowMinute in dayStartMinute..dayEndMinute) {
                        val frac = (nowMinute - dayStartMinute).toFloat() /
                            (dayEndMinute - dayStartMinute).coerceAtLeast(1)
                        val lineY = PERIOD_ROW_HEIGHT * periods.size * frac
                        Canvas(Modifier.fillMaxSize()) {
                            drawLine(
                                color = Color.Black,
                                start = Offset(0f, lineY.toPx()),
                                end = Offset(size.width, lineY.toPx()),
                                strokeWidth = 2.dp.toPx(),
                            )
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
    countdownDays: Set<Long>,
    highlightCountdown: Boolean,
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
            highlight = if (highlightCountdown) { date -> date.toEpochDay() in countdownDays } else null,
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