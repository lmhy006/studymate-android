package com.shiguang.app.ui.ddl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.entity.DdlEntity
import java.time.LocalDate
import java.time.LocalDateTime

private enum class DateField { NONE, DUE_DATE }
private enum class TimeField { NONE, DUE_TIME }

/**
 * DDL 添加/编辑弹层：作业内容、关联课程（可空）、截止时间（日期+时刻）、提交方式（可选）、备注（可选）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DdlEditSheet(
    editing: DdlEntity?,
    prefillCourseName: String?,
    courseNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (DdlEntity) -> Unit,
    onDelete: (DdlEntity) -> Unit,
) {
    val initial = editing?.dueAt?.let { DateUtils.fromEpochMillis(it) } ?: LocalDateTime.now().plusDays(1).withHour(23).withMinute(59)

    var title by remember { mutableStateOf(editing?.title ?: "") }
    var courseName by remember { mutableStateOf(editing?.courseName ?: prefillCourseName) }
    var dueDate by remember { mutableStateOf(initial.toLocalDate()) }
    var dueHour by remember { mutableIntStateOf(initial.hour) }
    var dueMinute by remember { mutableIntStateOf(initial.minute) }
    var submitMethod by remember { mutableStateOf(editing?.submitMethod ?: "") }
    var note by remember { mutableStateOf(editing?.note ?: "") }
    var dateField by remember { mutableStateOf(DateField.NONE) }
    var timeField by remember { mutableStateOf(TimeField.NONE) }
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
                text = if (editing == null) "添加 DDL" else "编辑 DDL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("作业内容，如：第三章习题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 关联课程
            Text(
                text = "关联课程（可跳过）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (courseNames.isEmpty()) {
                Text(
                    text = "日程里还没有课程；可在「日程」先添加课程后再关联。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(
                    selected = courseName == null,
                    onClick = { courseName = null },
                    label = { Text("不关联") },
                )
                courseNames.forEach { name ->
                    FilterChip(
                        selected = courseName == name,
                        onClick = { courseName = name },
                        label = { Text(name) },
                    )
                }
            }

            // 截止时间
            Surface(
                onClick = { dateField = DateField.DUE_DATE },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = "截止日期",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = DateUtils.formatMonthDay(dueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Surface(
                onClick = { timeField = TimeField.DUE_TIME },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "截止时刻",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = DateUtils.timeText(dueHour * 60 + dueMinute),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            OutlinedTextField(
                value = submitMethod,
                onValueChange = { submitMethod = it },
                label = { Text("提交方式（可选，如：学习通/邮箱）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        error = "请填写作业内容"
                        return@Button
                    }
                    error = null
                    onSave(
                        DdlEntity(
                            id = editing?.id ?: 0,
                            title = title.trim(),
                            courseName = courseName?.trim()?.ifBlank { null },
                            dueAt = DateUtils.toEpochMillis(dueDate.atTime(dueHour, dueMinute)),
                            submitMethod = submitMethod.trim().ifBlank { null },
                            note = note.trim().ifBlank { null },
                            completed = editing?.completed ?: false,
                            completedAt = editing?.completedAt,
                        )
                    )
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
                    Text("删除此 DDL", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (dateField != DateField.NONE) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toUtcMillis(dueDate)
        )
        DatePickerDialog(
            onDismissRequest = { dateField = DateField.NONE },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { dueDate = DateUtils.fromUtcMillis(it) }
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

    if (timeField != TimeField.NONE) {
        val timeState = rememberTimePickerState(
            initialHour = dueHour,
            initialMinute = dueMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timeField = TimeField.NONE },
            title = { Text("选择截止时刻") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueHour = timeState.hour
                        dueMinute = timeState.minute
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