package com.shiguang.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用设置（SharedPreferences 持久化 + StateFlow 响应式）。
 * 目前包含：主题模式（跟随系统/亮色/暗色）。
 */
object AppSettings {

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    private const val PREFS_NAME = "settings"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_SHOW_SATURDAY = "schedule_show_saturday"
    private const val KEY_SHOW_SUNDAY = "schedule_show_sunday"
    private const val KEY_HIGHLIGHT_TODAY = "schedule_highlight_today"
    private const val KEY_SHOW_BORDER = "schedule_show_border"
    private const val KEY_SHOW_DIVIDER = "schedule_show_divider"
    private const val KEY_DDL_RELEASE_DAYS = "ddl_release_days"

    private lateinit var prefs: android.content.SharedPreferences

    private val _themeMode = MutableStateFlow(THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // 日程显示设置（默认值参考 BIT101）
    private val _showSaturday = MutableStateFlow(true)
    val showSaturday: StateFlow<Boolean> = _showSaturday.asStateFlow()

    private val _showSunday = MutableStateFlow(true)
    val showSunday: StateFlow<Boolean> = _showSunday.asStateFlow()

    private val _highlightToday = MutableStateFlow(true)
    val highlightToday: StateFlow<Boolean> = _highlightToday.asStateFlow()

    private val _showBorder = MutableStateFlow(true)
    val showBorder: StateFlow<Boolean> = _showBorder.asStateFlow()

    private val _showDivider = MutableStateFlow(true)
    val showDivider: StateFlow<Boolean> = _showDivider.asStateFlow()

    // DDL：完成后自动释放的天数（默认 3）
    private val _ddlReleaseDays = MutableStateFlow(3)
    val ddlReleaseDays: StateFlow<Int> = _ddlReleaseDays.asStateFlow()

    /** 在 Application.onCreate 中初始化。 */
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        _showSaturday.value = prefs.getBoolean(KEY_SHOW_SATURDAY, true)
        _showSunday.value = prefs.getBoolean(KEY_SHOW_SUNDAY, true)
        _highlightToday.value = prefs.getBoolean(KEY_HIGHLIGHT_TODAY, true)
        _showBorder.value = prefs.getBoolean(KEY_SHOW_BORDER, true)
        _showDivider.value = prefs.getBoolean(KEY_SHOW_DIVIDER, true)
        _ddlReleaseDays.value = prefs.getInt(KEY_DDL_RELEASE_DAYS, 3).coerceIn(1, 30)
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit { putString(KEY_THEME, mode) }
    }

    fun setShowSaturday(value: Boolean) {
        _showSaturday.value = value
        prefs.edit { putBoolean(KEY_SHOW_SATURDAY, value) }
    }

    fun setShowSunday(value: Boolean) {
        _showSunday.value = value
        prefs.edit { putBoolean(KEY_SHOW_SUNDAY, value) }
    }

    fun setHighlightToday(value: Boolean) {
        _highlightToday.value = value
        prefs.edit { putBoolean(KEY_HIGHLIGHT_TODAY, value) }
    }

    fun setShowBorder(value: Boolean) {
        _showBorder.value = value
        prefs.edit { putBoolean(KEY_SHOW_BORDER, value) }
    }

    fun setShowDivider(value: Boolean) {
        _showDivider.value = value
        prefs.edit { putBoolean(KEY_SHOW_DIVIDER, value) }
    }

    fun setDdlReleaseDays(days: Int) {
        val v = days.coerceIn(1, 30)
        _ddlReleaseDays.value = v
        prefs.edit { putInt(KEY_DDL_RELEASE_DAYS, v) }
    }
}