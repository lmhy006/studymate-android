package com.shiguang.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.entity.ScheduleEntity
import com.shiguang.app.data.import.ImportedCourse
import com.shiguang.app.data.import.coursesToSchedules

/**
 * 逐课速录课表：表单式录入（课程名 / 星期 / 起止节次 / 教学周 / 教师 / 教室），
 * 可连续添加多门后一次性生成周期日程。与 JSON 导入产出相同的日程结构。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RapidEntryDialog(
    onDismiss: () -> Unit,
    onImport: (List<ScheduleEntity>) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var weekday by remember { mutableIntStateOf(1) }
    var startSection by remember { mutableFloatStateOf(1f) }
    var endSection by remember { mutableFloatStateOf(2f) }
    var weeksMinText by remember { mutableStateOf("1") }
    var weeksMaxText by remember { mutableStateOf("16") }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<ImportedCourse>>(emptyList()) }
    var termStart by remember { mutableStateOf(DateUtils.today()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("逐课速录课表") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "一门课一条，录入后点“添加”，可连续添加多门；\n完成后“生成导入”。节次时间按默认节次表（08:00 起 13 节）换算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名，如：线性代数") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "星期",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..7).forEach { w ->
                        FilterChip(
                            selected = weekday == w,
                            onClick = { weekday = w },
                            label = { Text(DateUtils.WEEKDAY_NAMES[w - 1].removePrefix("周")) },
                        )
                    }
                }

                Text(
                    text = "节次：第${startSection.toInt()}节 – 第${endSection.toInt()}节",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = startSection,
                    onValueChange = {
                        startSection = it
                        if (endSection < it) endSection = it
                    },
                    valueRange = 1f..13f,
                    steps = 11,
                )
                Slider(
                    value = endSection,
                    onValueChange = {
                        endSection = it
                        if (it < startSection) startSection = it
                    },
                    valueRange = 1f..13f,
                    steps = 11,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = weeksMinText,
                        onValueChange = { input -> if (input.length <= 2 && input.all { it.isDigit() }) weeksMinText = input },
                        label = { Text("起始周") },
                        modifier = Modifier.weight(1f),
                    )
                    Text("～", style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = weeksMaxText,
                        onValueChange = { input -> if (input.length <= 2 && input.all { it.isDigit() }) weeksMaxText = input },
                        label = { Text("结束周") },
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("教师（可选）") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("教室（可选）") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = {
                        val s = startSection.toInt()
                        val e = endSection.toInt()
                        if (name.isBlank()) {
                            error = "请填写课程名"
                            return@Button
                        }
                        if (e < s) {
                            error = "结束节次不能小于开始节次"
                            return@Button
                        }
                        val wMin = weeksMinText.toIntOrNull()?.coerceIn(1, 52) ?: 1
                        val wMax = weeksMaxText.toIntOrNull()?.coerceIn(1, 52) ?: 16
                        rows = rows + ImportedCourse(
                            name = name.trim(),
                            teacher = teacher.trim(),
                            classroom = classroom.trim(),
                            weekday = weekday,
                            startSection = s,
                            endSection = e,
                            weeksMin = wMin,
                            weeksMax = wMax,
                        )
                        error = null
                        name = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("添加本课")
                }

                if (rows.isNotEmpty()) {
                    Text(
                        text = "已添加 ${rows.size} 门课：",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    rows.forEachIndexed { index, course ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}. ${course.name}（周${DateUtils.WEEKDAY_NAMES[course.weekday - 1]} " +
                                    "第${course.startSection}-${course.endSection}节 第${course.weeksMin}-${course.weeksMax}周）",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(
                                onClick = {
                                    rows = rows.filterIndexed { idx, _ -> idx != index }
                                }
                            ) {
                                Text("删除")
                            }
                        }
                    }

                    Surface(
                        onClick = { showDatePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "学期开始日期",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = DateUtils.formatFull(termStart),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = rows.isNotEmpty(),
                onClick = {
                    val entities = coursesToSchedules(rows, termStart)
                    onImport(entities)
                },
            ) {
                Text("生成导入（${rows.size} 门）")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.toUtcMillis(termStart)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { termStart = DateUtils.fromUtcMillis(it) }
                        showDatePicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}