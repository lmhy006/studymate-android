package com.shiguang.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.WeekdayMask
import com.shiguang.app.data.entity.ScheduleEntity
import com.shiguang.app.ui.theme.SchedulePalette
import java.time.LocalDate

private enum class DateField { NONE, SINGLE, RANGE_START, RANGE_END }
private enum class TimeField { NONE, START, END }

/**
 * 日程 添加/编辑 底部弹层。
 *
 * 支持两种类型：
 * - 单次日程：某一天 + 起止时间；
 * - 批量周期日程：例如“3月1日 ~ 6月30日之间每个周一/周三上午 08:00 ~ 09:50”，
 *   由 [生效起始日期, 生效结束日期] + 星期多选 + 时间段 组成，避免一条条手动添加。
 *
 * 校验错误以“表单内联 + 最新一条覆盖”方式展示（不依赖底部 Snackbar，避免被弹层遮挡/排队）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditSheet(
    editing: ScheduleEntity?,
    prefill: PrefillData?,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntity) -> Unit,
    onDelete: (ScheduleEntity) -> Unit,
) {
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var location by remember { mutableStateOf(editing?.location ?: "") }
    var colorIndex by remember { mutableIntStateOf(editing?.colorIndex ?: 0) }

    var recurring by remember { mutableStateOf(editing?.isRecurring ?: false) }

    var singleDate by remember {
        mutableStateOf(
            editing?.singleDate
                ?: prefill?.date
                ?: DateUtils.today()
        )
    }
    var rangeStart by remember {
        mutableStateOf(
            editing?.repeatStartEpochDay?.let { DateUtils.fromEpochDay(it) }
                ?: DateUtils.today()
        )
    }
    var rangeEnd by remember {
        mutableStateOf(
            editing?.repeatEndEpochDay?.let { DateUtils.fromEpochDay(it) }
                ?: DateUtils.today().plusDays(90)
        )
    }
    var mask by remember { mutableIntStateOf(editing?.weekdaysMask ?: 0) }

    var startMin by remember {
        mutableIntStateOf(editing?.startMinute ?: (prefill?.minute ?: 8 * 60))
    }
    var endMin by remember {
        mutableIntStateOf(editing?.endMinute ?: ((prefill?.minute ?: 8 * 60) + 50))
    }

    var dateField by remember { mutableStateOf(DateField.NONE) }
    var timeField by remember { mutableStateOf(TimeField.NONE) }
    // 校验错误：最新一条覆盖上一条（挤占式，不排队）
    var error by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (editing == null) "添加日程" else "编辑日程",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题，如：线性代数") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("地点（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 颜色
            Text(
                text = "颜色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SchedulePalette.forEachIndexed { index, color ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (index == colorIndex) 2.5.dp else 0.dp,
                                color = if (index == colorIndex) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    Color.Transparent
                                },
                                shape = CircleShape,
                            )
                            .clickable { colorIndex = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (index == colorIndex) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            }

            // 类型
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !recurring,
                    onClick = { recurring = false },
                    label = { Text("单次日程") },
                )
                FilterChip(
                    selected = recurring,
                    onClick = { recurring = true },
                    label = { Text("批量周期日程") },
                )
            }

            HorizontalDivider()

            if (recurring) {
                // 生效日期范围
                DateFieldRow("生效日期", "${DateUtils.formatMonthDay(rangeStart)} ~ ${DateUtils.formatMonthDay(rangeEnd)}", icon = Icons.Outlined.DateRange) {
                    dateField = DateField.RANGE_START
                }
                // 星期多选
                Text(
                    text = "每周重复（可多选）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (0..6).forEach { index ->
                        FilterChip(
                            selected = (mask and (1 shl index)) != 0,
                            onClick = { mask = mask xor (1 shl index) },
                            label = { Text(DateUtils.WEEKDAY_NAMES[index].removePrefix("周")) },
                        )
                    }
                }
                Text(
                    text = "示例：从 ${DateUtils.formatMonthDay(rangeStart)} 到 ${DateUtils.formatMonthDay(rangeEnd)} 之间的所有" +
                        "${WeekdayMask.names(mask)} ${DateUtils.timeText(startMin)}~${DateUtils.timeText(endMin)} 都会自动生成日程。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                DateFieldRow("日期", DateUtils.formatFull(singleDate), icon = Icons.Outlined.DateRange) {
                    dateField = DateField.SINGLE
                }
            }

            // 起止时间
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DateFieldRow(
                    label = "开始时间",
                    value = DateUtils.timeText(startMin),
                    modifier = Modifier.weight(1f),
                ) {
                    timeField = TimeField.START
                }
                DateFieldRow(
                    label = "结束时间",
                    value = DateUtils.timeText(endMin),
                    modifier = Modifier.weight(1f),
                ) {
                    timeField = TimeField.END
                }
            }

            // 表单内联错误提示（最新一条覆盖上一条）
            error?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        error = "请填写日程标题"
                        return@Button
                    }
                    if (endMin <= startMin) {
                        error = "结束时间需晚于开始时间"
                        return@Button
                    }
                    if (recurring) {
                        if (rangeEnd.isBefore(rangeStart)) {
                            error = "结束日期不能早于开始日期"
                            return@Button
                        }
                        if (mask == 0) {
                            error = "请至少选择一个星期"
                            return@Button
                        }
                        error = null
                        onSave(
                            ScheduleEntity(
                                id = editing?.id ?: 0,
                                title = trimmed,
                                location = location.trim().ifBlank { null },
                                colorIndex = colorIndex,
                                singleEpochDay = null,
                                repeatStartEpochDay = rangeStart.toEpochDay(),
                                repeatEndEpochDay = rangeEnd.toEpochDay(),
                                weekdaysMask = mask,
                                startMinute = startMin,
                                endMinute = endMin,
                            )
                        )
                    } else {
                        error = null
                        onSave(
                            ScheduleEntity(
                                id = editing?.id ?: 0,
                                title = trimmed,
                                location = location.trim().ifBlank { null },
                                colorIndex = colorIndex,
                                singleEpochDay = singleDate.toEpochDay(),
                                repeatStartEpochDay = null,
                                repeatEndEpochDay = null,
                                weekdaysMask = 0,
                                startMinute = startMin,
                                endMinute = endMin,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }

            if (editing != null) {
                OutlinedButton(
                    onClick = { onDelete(editing) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除此日程", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    // ---- 日期选择 ----
    if (dateField != DateField.NONE) {
        val initial = when (dateField) {
            DateField.SINGLE -> singleDate
            DateField.RANGE_START -> rangeStart
            DateField.RANGE_END -> rangeEnd
            DateField.NONE -> DateUtils.today()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = DateUtils.toUtcMillis(initial))
        DatePickerDialog(
            onDismissRequest = { dateField = DateField.NONE },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = pickerState.selectedDateMillis?.let { DateUtils.fromUtcMillis(it) }
                        if (picked != null) {
                            when (dateField) {
                                DateField.SINGLE -> singleDate = picked
                                DateField.RANGE_START -> rangeStart = picked
                                DateField.RANGE_END -> rangeEnd = picked
                                DateField.NONE -> Unit
                            }
                        }
                        dateField = DateField.NONE
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateField = DateField.NONE }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    // ---- 时间选择（material3 1.3.2 无 TimePickerDialog，用 AlertDialog 包裹 TimePicker）----
    if (timeField != TimeField.NONE) {
        val initialMinute = if (timeField == TimeField.START) startMin else endMin
        val timeState = rememberTimePickerState(
            initialHour = initialMinute / 60,
            initialMinute = initialMinute % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timeField = TimeField.NONE },
            title = {
                Text(if (timeField == TimeField.START) "选择开始时间" else "选择结束时间")
            },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minute = timeState.hour * 60 + timeState.minute
                        if (timeField == TimeField.START) startMin = minute else endMin = minute
                        timeField = TimeField.NONE
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { timeField = TimeField.NONE }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DateFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}