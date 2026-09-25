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
import androidx.compose.material3.Surface
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
 * 自定义时间表编辑器：逐节设置“上课/下课时间”（仿 BIT101 设置-课程表设置-时间表）。
 * 校验：同节结束晚于开始；相邻节次时间不重叠；保存为活动时间表（周视图与课表导入即时生效）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTableEditorDialog(
    initial: List<SchedulePeriod>,
    onSave: (List<SchedulePeriod>) -> Unit,
    onDismiss: () -> Unit,
) {
    var rows by remember { mutableStateOf(initial.toMutableList()) }
    var error by remember { mutableStateOf<String?>(null) }
    // 正在编辑的 (行下标, 是否开始时间)
    var editTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

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
                    text = "设置每节课的上课/下课时间；影响节次周视图与课表导入的换算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                rows.forEachIndexed { index, period ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "第${period.number}节",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TimeChip(DateUtils.timeText(period.startMinute)) {
                            editTarget = index to true
                        }
                        Text(
                            text = " ~ ",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        TimeChip(DateUtils.timeText(period.endMinute)) {
                            editTarget = index to false
                        }
                    }
                }
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 校验收起
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
                TextButton(onClick = { rows = SchedulePeriods.DEFAULT.toMutableList(); error = null }) {
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
                            current.copy(startMinute = newMinute)
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
private fun TimeChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold),
        )
    }
}