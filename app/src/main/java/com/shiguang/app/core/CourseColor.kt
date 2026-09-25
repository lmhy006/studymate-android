package com.shiguang.app.core

/**
 * 课程配色：按课程名稳定哈希分配，保证“同名课程颜色一致”，
 * 同时尽量让不同课程分散到不同颜色（色盘 12 色，撞色概率明显低于 8 色）。
 */
object CourseColor {

    /** 与 ui.theme.SchedulePalette 的色数保持一致。 */
    const val PALETTE_SIZE = 12

    fun indexForTitle(title: String, paletteSize: Int = PALETTE_SIZE): Int {
        val normalized = title.trim()
        if (normalized.isEmpty()) return 0
        val h = mix(normalized.hashCode())
        return (h and Int.MAX_VALUE) % paletteSize
    }

    /** Thomas Wang 整数雪崩最终化：让相邻/相似字符串的哈希在小取模空间下尽量分散。 */
    private fun mix(input: Int): Int {
        var h = input
        h = h xor (h ushr 16)
        h *= 0x7feb352d
        h = h xor (h ushr 15)
        h *= -2071394676 // 0x846ca68b 的等值有符号 32 位表示
        h = h xor (h ushr 16)
        return h
    }
}