package com.shiguang.app.ui.habit

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shiguang.app.data.entity.HabitEntity

private val EMOJI_PRESETS = listOf("📚", "🏃", "💪", "🧠", "✍️", "🎯", "🌱", "🧘", "💧", "😴")

/** 周期类型：0=每日 1=每周 2=自定义 N 天 */
private const val TYPE_DAILY = 0
private const val TYPE_WEEKLY = 1
private const val TYPE_CUSTOM = 2

/**
 * 新增习惯底部弹层。
 * mode：周期模式 / 自由模式（每个习惯可单独选择）；
 * intervalDays 由周期类型决定：每日=1，每周=7，自定义=用户输入的 N（至少 2）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditSheet(
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, intervalDays: Int, mode: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf(EMOJI_PRESETS.first()) }
    var type by remember { mutableIntStateOf(TYPE_DAILY) }
    var customDays by remember { mutableStateOf("3") }
    var mode by remember { mutableIntStateOf(HabitEntity.MODE_CYCLIC) }

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
                text = "添加习惯",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("习惯名称，如：背单词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "图标",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EMOJI_PRESETS.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (preset == emoji) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable { emoji = preset }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = preset, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Text(
                text = "打卡模式",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == HabitEntity.MODE_CYCLIC,
                    onClick = { mode = HabitEntity.MODE_CYCLIC },
                    label = { Text("周期模式") },
                )
                FilterChip(
                    selected = mode == HabitEntity.MODE_FREE,
                    onClick = { mode = HabitEntity.MODE_FREE },
                    label = { Text("自由模式") },
                )
            }
            Text(
                text = if (mode == HabitEntity.MODE_CYCLIC) {
                    "周期模式：按创建日锚定周期日，每 N 天到期一次，每周期最多打卡 1 次。"
                } else {
                    "自由模式：可随时打卡；打卡后 N 天内再打即连续，超过 N 天未打卡记一次缺卡，下次打卡重新起算。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "打卡周期",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = type == TYPE_DAILY,
                    onClick = { type = TYPE_DAILY },
                    label = { Text("每日") },
                )
                FilterChip(
                    selected = type == TYPE_WEEKLY,
                    onClick = { type = TYPE_WEEKLY },
                    label = { Text("每周") },
                )
                FilterChip(
                    selected = type == TYPE_CUSTOM,
                    onClick = { type = TYPE_CUSTOM },
                    label = { Text("每 N 天") },
                )
            }

            if (type == TYPE_CUSTOM) {
                OutlinedTextField(
                    value = customDays,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) customDays = input
                    },
                    label = { Text("间隔天数 N（至少 2）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val interval = when (type) {
                        TYPE_DAILY -> 1
                        TYPE_WEEKLY -> 7
                        else -> customDays.toIntOrNull()?.coerceAtLeast(2) ?: 3
                    }
                    onSave(name.trim(), emoji, interval, mode)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("创建习惯")
            }
        }
    }
}