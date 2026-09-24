package com.shiguang.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shiguang.app.data.AppSettings
import com.shiguang.app.ui.countdown.CountdownScreen
import com.shiguang.app.ui.habit.HabitScreen
import com.shiguang.app.ui.schedule.ScheduleScreen
import com.shiguang.app.ui.settings.SettingsScreen
import com.shiguang.app.ui.theme.StudyMateTheme

/**
 * 根界面：底部四 Tab（日程 / 倒数 / 打卡 / 设置），无导航库，保持极简。
 * 主题模式来自设置（跟随系统 / 亮色 / 暗色）。
 */
@Composable
fun AppRoot() {
    val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    StudyMateTheme(darkTheme = darkTheme) {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
                        label = { Text("日程") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.Star, contentDescription = null) },
                        label = { Text("倒数") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                        label = { Text("打卡") },
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("设置") },
                    )
                }
            },
        ) { innerPadding ->
            Box(Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    0 -> ScheduleScreen()
                    1 -> CountdownScreen()
                    2 -> HabitScreen()
                    else -> SettingsScreen()
                }
            }
        }
    }
}