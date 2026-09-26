package com.shiguang.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiguang.app.BuildConfig
import com.shiguang.app.StudyMateApp
import com.shiguang.app.core.DateUtils
import com.shiguang.app.data.AppSettings
import com.shiguang.app.ui.schedule.ScheduleViewModel

/**
 * 设置页：
 * - 外观：亮色 / 暗色 / 跟随系统；
 * - 日程设置：显示周六/周日、高亮今日、节次分界线、边框，导入 BIT101 课表；
 * - 关于：版本信息、检查应用更新（含拉取安装）、数据与更新说明。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scheduleViewModel: ScheduleViewModel = viewModel {
        ScheduleViewModel(
            (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as StudyMateApp).container
        )
    },
) {
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val showSaturday by AppSettings.showSaturday.collectAsStateWithLifecycle()
    val showSunday by AppSettings.showSunday.collectAsStateWithLifecycle()
    val highlightToday by AppSettings.highlightToday.collectAsStateWithLifecycle()
    val showBorder by AppSettings.showBorder.collectAsStateWithLifecycle()
    val showDivider by AppSettings.showDivider.collectAsStateWithLifecycle()
    val highlightCountdown by AppSettings.highlightCountdown.collectAsStateWithLifecycle()
    val timeTable by AppSettings.timeTable.collectAsStateWithLifecycle()

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showRapidDialog by remember { mutableStateOf(false) }
    var showTermPicker by remember { mutableStateOf(false) }
    var showTimeTableEditor by remember { mutableStateOf(false) }
    val toastContext = LocalContext.current

    if (showUpdateDialog) {
        CheckUpdateDialog(onDismiss = { showUpdateDialog = false })
    }
    if (showImportDialog) {
        ImportCourseDialog(
            periods = timeTable,
            onDismiss = { showImportDialog = false },
            onImport = { entities ->
                entities.forEach { scheduleViewModel.save(it) }
                Toast.makeText(toastContext, "已导入 ${entities.size} 条课程", Toast.LENGTH_SHORT).show()
                showImportDialog = false
            },
        )
    }
    if (showRapidDialog) {
        RapidEntryDialog(
            periods = timeTable,
            onDismiss = { showRapidDialog = false },
            onImport = { entities ->
                entities.forEach { scheduleViewModel.save(it) }
                Toast.makeText(toastContext, "已导入 ${entities.size} 门课", Toast.LENGTH_SHORT).show()
                showRapidDialog = false
            },
        )
    }
    if (showTimeTableEditor) {
        TimeTableEditorDialog(
            initial = timeTable,
            onSave = {
                AppSettings.setTimeTable(it)
                Toast.makeText(toastContext, "时间表已保存", Toast.LENGTH_SHORT).show()
                showTimeTableEditor = false
            },
            onDismiss = { showTimeTableEditor = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            SettingsCard(title = "外观") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeChip("跟随系统", AppSettings.THEME_SYSTEM, themeMode) {
                        AppSettings.setThemeMode(AppSettings.THEME_SYSTEM)
                    }
                    ThemeChip("亮色", AppSettings.THEME_LIGHT, themeMode) {
                        AppSettings.setThemeMode(AppSettings.THEME_LIGHT)
                    }
                    ThemeChip("暗色", AppSettings.THEME_DARK, themeMode) {
                        AppSettings.setThemeMode(AppSettings.THEME_DARK)
                    }
                }
            }
        }

        item {
            SettingsCard(title = "日程设置") {
                SettingSwitch("显示周六", showSaturday, AppSettings::setShowSaturday)
                SettingSwitch("显示周日", showSunday, AppSettings::setShowSunday)
                SettingSwitch("高亮今日", highlightToday, AppSettings::setHighlightToday)
                SettingSwitch("高亮倒数日当天", highlightCountdown, AppSettings::setHighlightCountdown)
                SettingSwitch("显示节次分界线", showDivider, AppSettings::setShowDivider)
                SettingSwitch("显示边框", showBorder, AppSettings::setShowBorder)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                // DDL 完成后自动释放天数
                val ddlReleaseDays by AppSettings.ddlReleaseDays.collectAsStateWithLifecycle()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DDL 完成后自动释放天数",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(
                        onClick = { AppSettings.setDdlReleaseDays(ddlReleaseDays - 1) },
                        enabled = ddlReleaseDays > 1,
                    ) {
                        Text("−")
                    }
                    Text(text = "$ddlReleaseDays 天", style = MaterialTheme.typography.bodyLarge)
                    TextButton(
                        onClick = { AppSettings.setDdlReleaseDays(ddlReleaseDays + 1) },
                        enabled = ddlReleaseDays < 30,
                    ) {
                        Text("+")
                    }
                }

                // 教学周起点（本学期第 1 周）
                val termStart by AppSettings.termStartEpochDay.collectAsStateWithLifecycle()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "本学期教学周起点（第1周）",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = termStart?.let { DateUtils.formatFull(DateUtils.fromEpochDay(it)) }
                                ?: "未设置（导入时按今天兜底）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showTermPicker = true }) { Text("设置") }
                    if (termStart != null) {
                        TextButton(onClick = { AppSettings.setTermStart(null) }) { Text("清除") }
                    }
                }

                // 自定义时间表
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "时间表（每节课上课/下课时间）",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "默认 08:00 起 ${timeTable.size} 节；影响节次周视图与课表导入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showTimeTableEditor = true }) { Text("编辑") }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showImportDialog = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "导入课表（BIT101 JSON）",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "把 BIT101 课表按 JSON 粘贴进来，批量转为周期日程",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showImportDialog = true }) { Text("导入") }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRapidDialog = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "逐课速录",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "表单式录入课程/星期/节次/教学周，可连续添加多门",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { showRapidDialog = true }) { Text("录入") }
                }
            }
        }

        item {
            SettingsCard(title = "关于") {
                AboutRow(
                    key = "版本",
                    value = "v${BuildConfig.VERSION_NAME}",
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUpdateDialog = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "检查更新",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TextButton(onClick = { showUpdateDialog = true }) { Text("检查") }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    text = "说明：\n" +
                        "· 倒计时/打卡/日程数据全部保存在本机（Room），关闭 APP 不丢失；\n" +
                        "· 应用仅在“检查更新”时访问网络（GitHub 最新发布版），其余功能完全离线；\n" +
                        "· 桌面小组件：长按桌面 → 添加工具/小部件 → 拾光倒数日。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showTermPicker) {
        val initial = AppSettings.termStartEpochDay.value?.let { DateUtils.fromEpochDay(it) }
            ?: DateUtils.today()
        val picker = rememberDatePickerState(initialSelectedDateMillis = DateUtils.toUtcMillis(initial))
        DatePickerDialog(
            onDismissRequest = { showTermPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let {
                            AppSettings.setTermStart(DateUtils.fromUtcMillis(it).toEpochDay())
                        }
                        showTermPicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTermPicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = picker)
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ThemeChip(label: String, mode: String, current: String, onClick: () -> Unit) {
    FilterChip(
        selected = current == mode,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AboutRow(key: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = key,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}