package com.shiguang.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.core.SchedulePeriod
import com.shiguang.app.core.SchedulePeriods

/**
 * 自定义时间表编辑器（仿 BIT101 设置-课程表设置-时间表）：
 * 1. 自定义每日节数（默认 13，1..20）；
 * 2. 可选“每节固定时长”：开启后输入每节的开始时间，结束时间自动计算（= 开始 + 时长）；
 * 3. 也可逐节手动指定 上课/下课时间。
 * 校验：同节结束晚于开始、相邻节次不重叠；保存后周视图与课表导入即时生效。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTableEditorDialog(
    initial: List<SchedulePeriod>,
    onSave: (List<SchedulePeriod>) -> Unit,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf< List<SchedulePeriod>>(initial.toList()) }
    var durationEnabled by remember { mutableStateOf(false) }
    var durationText by remember { mutableStateOf("45") }
    var error by remember { mutableStateOf<String?>(null) }
    // 正在编辑的 (行下标, 是否开始时间)
    var editTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val duration = durationText.toIntOrNull()?.coerceIn(1, 300) ?: 45

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义时间表") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "可自定义每日节数与每节时长；开启“固定时长”后，修改开始时间会自动算出下课时间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 校验错误置顶展示，避免被时间列表遮挡
                error?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "⚠ $msg",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                // 每日节数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "每日节数",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(
                        enabled = rows.size > 1,
                        onClick = {
                            rows = rows.dropLast(1).toMutableList()
                            error = null
                        },
                    ) {
                        Text("−")
                    }
                    Text(text = "${rows.size} 节", style = MaterialTheme.typography.bodyLarge)
                    TextButton(
                        enabled = rows.size < 20,
                        onClick = {
                            rows = SchedulePeriods.appendPeriod(rows, duration).toMutableList()
                            error = null
                        },
                    ) {
                        Text("+")
                    }
                }

                // 固定时长
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "每节时长（固定，自动算下课）",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = durationEnabled,
                        onCheckedChange = { enabled ->
                            durationEnabled = enabled
                            if (enabled) {
                                rows = rows.map { it.copy(endMinute = SchedulePeriods.endWithDuration(it.startMinute, duration)) }
                                    .toMutableList()
                            }
                        },
                    )
                }
                if (durationEnabled) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { input ->
                            if (input.length <= 3 && input.all { it.isDigit() }) {
                                durationText = input
                                rows = rows.map {
                                    it.copy(endMinute = SchedulePeriods.endWithDuration(it.startMinute, duration))
                                }.toMutableList()
                            }
                        },
                        label = { Text("每节时长（分钟）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                rows.forEachIndexed { index, period ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "第${period.number}节",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TimeChip(DateUtils.timeText(period.startMinute), enabled = true) {
                            editTarget = index to true
                        }
                        Text(text = " ~ ", style = MaterialTheme.typography.bodyMedium)
                        TimeChip(DateUtils.timeText(period.endMinute), enabled = !durationEnabled) {
                            editTarget = index to false
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (durationEnabled && durationText.toIntOrNull() == null) {
                        error = "请填写每节时长（分钟）"
                        return@TextButton
                    }
                    for (i in rows.indices) {
                        if (rows[i].endMinute <= rows[i].startMinute) {
                            error = "第${rows[i].number}节结束时间必须晚于开始时间"
                            return@TextButton
                        }
                        if (i > 0 && rows[i].startMinute < rows[i - 1].endMinute) {
                            error = "第${rows[i].number}节与前一节时间重叠"
                            return@TextButton
                        }
                    }
                    error = null
                    onSave(rows)
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        rows = SchedulePeriods.DEFAULT.toMutableList()
                        error = null
                    },
                ) {
                    Text("恢复默认")
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )

    editTarget?.let { (index, isStart) ->
        val current = rows.getOrNull(index) ?: return@let
        val minute = if (isStart) current.startMinute else current.endMinute
        val state = rememberTimePickerState(initialHour = minute / 60, initialMinute = minute % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("选择第${current.number}节${if (isStart) "上课" else "下课"}时间") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newMinute = state.hour * 60 + state.minute
                        val updated = rows.toMutableList()
                        updated[index] = if (isStart) {
                            current.copy(
                                startMinute = newMinute,
                                endMinute = if (durationEnabled) {
                                    SchedulePeriods.endWithDuration(newMinute, duration)
                                } else {
                                    current.endMinute
                                },
                            )
                        } else {
                            current.copy(endMinute = newMinute)
                        }
                        rows = updated
                        editTarget = null
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TimeChip(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}