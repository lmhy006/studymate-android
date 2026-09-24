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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiguang.app.BuildConfig
import com.shiguang.app.data.AppSettings

/**
 * 设置页：
 * - 外观：亮色 / 暗色 / 跟随系统；
 * - 关于：版本信息、检查应用更新（含拉取安装）、数据与更新说明。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    var showUpdateDialog by remember { mutableStateOf(false) }

    if (showUpdateDialog) {
        CheckUpdateDialog(onDismiss = { showUpdateDialog = false })
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
            SettingsCard(title = "关于") {
                AboutRow(
                    key = "版本",
                    value = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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
                        "· 倒数日、打卡、日程数据全部保存在本机（Room），关闭 APP 不丢失；\n" +
                        "· 应用仅在“检查更新”时访问网络（GitHub 最新发布版），其余功能完全离线；\n" +
                        "· 桌面小组件：长按桌面 → 添加工具/小部件 → 拾光倒数日。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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