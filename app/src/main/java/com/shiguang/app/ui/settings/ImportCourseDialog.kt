package com.shiguang.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.SchedulePeriod
import com.shiguang.app.core.SchedulePeriods
import com.shiguang.app.data.AppSettings
import com.shiguang.app.data.entity.ScheduleEntity
import com.shiguang.app.data.import.ImportedCourse
import com.shiguang.app.data.import.coursesToSchedules
import com.shiguang.app.data.import.parseCoursesJson

/**
 * 导入 BIT101 课表 JSON：
 * 1. 粘贴 JSON（与 BIT101 课程对象同构：name/teacher/classroom/weekday/start_section/end_section/weeks）；
 * 2. 设置学期开始日期；
 * 3. 预览解析结果（含错误提示），确认后生成周期日程。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCourseDialog(
    periods: List<SchedulePeriod> = SchedulePeriods.DEFAULT,
    onDismiss: () -> Unit,
    onImport: (List<ScheduleEntity>) -> Unit,
) {
    var json by remember { mutableStateOf("") }
    var termStart by remember {
        mutableStateOf(
            AppSettings.termStartEpochDay.value?.let { DateUtils.fromEpochDay(it) }
                ?: DateUtils.today()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Pair<List<ImportedCourse>, List<String>>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导入课表（BIT101 JSON）") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "节次时间按当前时间表（设置中可改）换算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it },
                    label = { Text("粘贴课表 JSON") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )

                Text(
                    text = "示例：\n" +
                        """{"courses":[{"name":"线性代数","teacher":"张老师","classroom":"良乡1-101","weekday":1,"start_section":1,"end_section":2,"weeks":[1,2,3,4,5,6]}]}""",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                Text(
                    text = "第1周以「教学周起点」对齐；此处修改会自动保存为默认（设置→日程设置 可改）。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextButton(
                    onClick = { preview = parseCoursesJson(json) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("预览解析结果")
                }

                preview?.let { (courses, errors) ->
                    if (errors.isNotEmpty()) {
                        errors.forEach { err ->
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Text(
                        text = "将导入 ${courses.size} 条课程（按节次周视图显示）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (courses.isEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (preview?.first?.isNotEmpty() == true),
                onClick = {
                    val courses = preview!!.first
                    val entities = coursesToSchedules(courses, termStart, periods)
                    onImport(entities)
                },
            ) {
                Text("导入")
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
                        pickerState.selectedDateMillis?.let {
                            val d = DateUtils.fromUtcMillis(it)
                            termStart = d
                            AppSettings.setTermStart(d.toEpochDay())
                        }
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