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

    private lateinit var prefs: android.content.SharedPreferences

    private val _themeMode = MutableStateFlow(THEME_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    /** 在 Application.onCreate 中初始化。 */
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit { putString(KEY_THEME, mode) }
    }
}