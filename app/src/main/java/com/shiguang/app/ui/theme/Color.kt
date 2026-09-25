package com.shiguang.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---- 亮色 ----
val Primary = Color(0xFF4460F0)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFDDE3FF)
val OnPrimaryContainer = Color(0xFF12225C)
val Secondary = Color(0xFF2E9E6B)
val OnSecondary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEF0F6)
val OnLightSurface = Color(0xFF1A1C22)
val OnLightSurfaceVariant = Color(0xFF5A6070)
val LightOutline = Color(0xFFD6DAE4)
val LightOutlineVariant = Color(0xFFE7EAF1)
val LightError = Color(0xFFC7504E)

// ---- 暗色 ----
val DarkPrimary = Color(0xFF94A2FF)
val OnDarkPrimary = Color(0xFF17224F)
val DarkPrimaryContainer = Color(0xFF2C3A97)
val OnDarkPrimaryContainer = Color(0xFFDDE3FF)
val DarkBackground = Color(0xFF111318)
val DarkSurface = Color(0xFF1A1D24)
val DarkSurfaceVariant = Color(0xFF262A34)
val OnDarkSurface = Color(0xFFEDEFF4)
val OnDarkSurfaceVariant = Color(0xFFA9AFBC)
val DarkOutline = Color(0xFF3A4150)
val DarkOutlineVariant = Color(0xFF2A2F3A)
val DarkError = Color(0xFFF28B82)

/**
 * 日程颜色盘（12 色，极简低饱和）。
 * 同名课程按课程名哈希取同一颜色；12 色使不同课程的撞色概率明显低于 8 色。
 */
val SchedulePalette = listOf(
    Color(0xFF4460F0), // 靛蓝
    Color(0xFF2E9E6B), // 绿
    Color(0xFFE07B39), // 橙
    Color(0xFFC2488F), // 玫红
    Color(0xFF6B5BD0), // 紫
    Color(0xFF2A9DBB), // 青
    Color(0xFFC7504E), // 红
    Color(0xFF8A94A6), // 灰
    Color(0xFF8FA83C), // 黄绿
    Color(0xFF3E8E9E), // 深青
    Color(0xFFA06B8E), // 紫红
    Color(0xFFA87B51), // 棕
)

fun scheduleColor(index: Int): Color =
    SchedulePalette[((index % SchedulePalette.size) + SchedulePalette.size) % SchedulePalette.size]

/** 打卡“今天”的强调绿。 */
val TodayGreen = Color(0xFF2E9E6B)