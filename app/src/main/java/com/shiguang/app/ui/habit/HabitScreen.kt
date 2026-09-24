package com.shiguang.app.ui.habit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.app.StudyMateApp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.Recurrence
import com.shiguang.app.core.Streak
import com.shiguang.app.data.entity.HabitEntity
import com.shiguang.app.ui.components.EmptyHint
import com.shiguang.app.ui.components.MonthCalendar
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HabitScreen(
    viewModel: HabitViewModel = viewModel {
        HabitViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    val monthOffsets = remember { mutableStateMapOf<Long, Int>() }
    var deleteTarget by remember { mutableStateOf<HabitEntity?>(null) }

    if (showAdd) {
        HabitEditSheet(
            onDismiss = { showAdd = false },
            onSave = { name, emoji, interval ->
                viewModel.addHabit(name, emoji, interval)
                showAdd = false
            },
        )
    }

    deleteTarget?.let { habit ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除习惯") },
            text = { Text("将删除「${habit.name}」及其全部打卡记录，确定吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHabit(habit)
                        deleteTarget = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "打卡",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = DateUtils.formatFull(state.today),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val dueHabits = state.habits.filter { habit ->
                val checked = state.recordsByHabit[habit.id].orEmpty()
                Recurrence.isTodayOccurrence(
                    DateUtils.fromEpochDay(habit.startEpochDay),
                    habit.intervalDays,
                    state.today,
                ) && state.today.toEpochDay() !in checked
            }
            if (dueHabits.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "今日待打卡：",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                            dueHabits.forEach { habit ->
                                Text(
                                    text = "${habit.emoji}${habit.name}  ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            if (state.habits.isEmpty()) {
                item { EmptyHint("还没有习惯\n点击右下角 + 添加第一个习惯，开始打卡") }
            }

            items(state.habits, key = { it.id }) { habit ->
                HabitCard(
                    habit = habit,
                    today = state.today,
                    checked = state.recordsByHabit[habit.id].orEmpty(),
                    expanded = expanded[habit.id] == true,
                    monthOffset = monthOffsets[habit.id] ?: 0,
                    onToggleExpand = {
                        expanded[habit.id] = !(expanded[habit.id] ?: false)
                    },
                    onCheckToggle = {
                        viewModel.toggleCheckIn(habit, state.today.toEpochDay(), it)
                    },
                    onCheckDate = { date, currently ->
                        viewModel.toggleCheckIn(habit, date.toEpochDay(), currently)
                    },
                    onShiftMonth = { delta ->
                        monthOffsets[habit.id] = (monthOffsets[habit.id] ?: 0) + delta
                    },
                    onRequestDelete = { deleteTarget = habit },
                )
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加习惯")
        }
    }
}

private sealed interface DayStatus {
    data object Checked : DayStatus
    data object Missed : DayStatus
    data object Due : DayStatus
    data object Upcoming : DayStatus
}

/** 某天在习惯日历上的状态；非周期日返回 null。 */
private fun dayStatus(
    habit: HabitEntity,
    checked: Set<Long>,
    today: LocalDate,
    date: LocalDate,
): DayStatus? {
    val start = DateUtils.fromEpochDay(habit.startEpochDay)
    if (!Recurrence.isHabitOccurrence(start, habit.intervalDays, date)) return null
    val day = date.toEpochDay()
    return when {
        day in checked -> DayStatus.Checked
        date.isBefore(today) -> DayStatus.Missed
        date == today -> DayStatus.Due
        else -> DayStatus.Upcoming
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    today: LocalDate,
    checked: Set<Long>,
    expanded: Boolean,
    monthOffset: Int,
    onToggleExpand: () -> Unit,
    onCheckToggle: (currentlyChecked: Boolean) -> Unit,
    onCheckDate: (date: LocalDate, currentlyChecked: Boolean) -> Unit,
    onShiftMonth: (Int) -> Unit,
    onRequestDelete: () -> Unit,
) {
    val start = DateUtils.fromEpochDay(habit.startEpochDay)
    val currentStreak = Streak.current(start, habit.intervalDays, checked, today)
    val longest = Streak.longest(start, habit.intervalDays, checked, today)
    val total = Streak.total(start, habit.intervalDays, checked, today)
    val missed = Streak.missed(start, habit.intervalDays, checked, today)
    val dueToday = Recurrence.isTodayOccurrence(start, habit.intervalDays, today)
    val checkedToday = today.toEpochDay() in checked
    val month = YearMonth.now().plusMonths(monthOffset.toLong())

    Surface(
        onClick = onToggleExpand,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = habit.emoji, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = intervalLabel(habit.intervalDays),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (missed > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "已断签 $missed 次",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                CheckButton(
                    dueToday = dueToday,
                    checkedToday = checkedToday,
                    onToggle = { onCheckToggle(checkedToday) },
                )
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("连续 $currentStreak 天")
                    StatChip("最长 $longest 天")
                    StatChip("累计 $total 次")
                }
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onShiftMonth(-1) }) { Text("‹ 上月") }
                    Text(
                        text = month.format(DateUtils.YEAR_MONTH),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = { onShiftMonth(1) }) { Text("下月 ›") }
                }

                MonthCalendar(
                    month = month,
                    today = today,
                    modifier = Modifier.fillMaxWidth(),
                    mark = { date ->
                        val status = dayStatus(habit, checked, today, date)
                        if (status == DayStatus.Due) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                            )
                        } else if (status != null) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            DayStatus.Checked -> MaterialTheme.colorScheme.primary
                                            DayStatus.Missed -> MaterialTheme.colorScheme.error
                                            DayStatus.Upcoming -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            DayStatus.Due -> Color.Transparent
                                        }
                                    )
                            )
                        }
                    },
                    onDayClick = { date ->
                        val status = dayStatus(habit, checked, today, date)
                        when (status) {
                            DayStatus.Checked -> onCheckDate(date, true)
                            // 今天到期或已错过的周期日可以打卡（补签）；未来周期日不可提前打卡
                            DayStatus.Due, DayStatus.Missed -> onCheckDate(date, false)
                            DayStatus.Upcoming, null -> Unit
                        }
                    },
                )

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除习惯")
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CheckButton(
    dueToday: Boolean,
    checkedToday: Boolean,
    onToggle: () -> Unit,
) {
    when {
        !dueToday -> {
            Text(
                text = "未到期",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        checkedToday -> {
            FilledTonalButton(onClick = onToggle) {
                Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("已打卡")
            }
        }
        else -> {
            Button(onClick = onToggle) {
                Text("打卡")
            }
        }
    }
}

private fun intervalLabel(intervalDays: Int): String = when (intervalDays) {
    1 -> "每日"
    7 -> "每周"
    else -> "每 $intervalDays 天一次"
}