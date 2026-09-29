package com.asa.note.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色"),
}

enum class Palette(val label: String) {
    Dynamic("跟随壁纸"),
    Violet("紫"),
    Blue("蓝"),
    Green("绿"),
    Orange("橙"),
}

private class Seeds(
    val lightPrimary: Long,
    val lightOnPrimary: Long,
    val lightContainer: Long,
    val lightOnContainer: Long,
    val darkPrimary: Long,
    val darkOnPrimary: Long,
    val darkContainer: Long,
    val darkOnContainer: Long,
)

private val Palette.seeds: Seeds
    get() = when (this) {
        Palette.Dynamic, Palette.Violet -> Seeds(
            lightPrimary = 0xFF6750A4,
            lightOnPrimary = 0xFFFFFFFF,
            lightContainer = 0xFFEADDFF,
            lightOnContainer = 0xFF21005D,
            darkPrimary = 0xFFD0BCFF,
            darkOnPrimary = 0xFF381E72,
            darkContainer = 0xFF4F378B,
            darkOnContainer = 0xFFEADDFF,
        )

        Palette.Blue -> Seeds(
            lightPrimary = 0xFF1B6BB5,
            lightOnPrimary = 0xFFFFFFFF,
            lightContainer = 0xFFD3E4FF,
            lightOnContainer = 0xFF001D36,
            darkPrimary = 0xFFA2C9FF,
            darkOnPrimary = 0xFF00325A,
            darkContainer = 0xFF00497E,
            darkOnContainer = 0xFFD3E4FF,
        )

        Palette.Green -> Seeds(
            lightPrimary = 0xFF2E6B4F,
            lightOnPrimary = 0xFFFFFFFF,
            lightContainer = 0xFFB4F1CE,
            lightOnContainer = 0xFF00210F,
            darkPrimary = 0xFF99D5B3,
            darkOnPrimary = 0xFF003822,
            darkContainer = 0xFF13502F,
            darkOnContainer = 0xFFB4F1CE,
        )

        Palette.Orange -> Seeds(
            lightPrimary = 0xFF8B5000,
            lightOnPrimary = 0xFFFFFFFF,
            lightContainer = 0xFFFFDDB8,
            lightOnContainer = 0xFF2C1700,
            darkPrimary = 0xFFFFB870,
            darkOnPrimary = 0xFF4A2800,
            darkContainer = 0xFF6A3C00,
            darkOnContainer = 0xFFFFDDB8,
        )
    }

/**
 * 只覆盖主色与容器色，其余角色走 Material 3 默认。
 * secondary / tertiary 系列刻意绑到同一组容器色上，否则选了色系之后
 * 筛选 chip 和底部栏指示器还会留在默认的紫色，看着像没生效。
 * 这仍不是逐档的完整色调生成，方案文档 §4 里写明了这个取舍。
 */
fun Palette.lightScheme(): ColorScheme {
    val s = seeds
    return lightColorScheme(
        primary = Color(s.lightPrimary),
        onPrimary = Color(s.lightOnPrimary),
        primaryContainer = Color(s.lightContainer),
        onPrimaryContainer = Color(s.lightOnContainer),
        secondaryContainer = Color(s.lightContainer),
        onSecondaryContainer = Color(s.lightOnContainer),
        tertiary = Color(s.lightPrimary),
        onTertiary = Color(s.lightOnPrimary),
        tertiaryContainer = Color(s.lightContainer),
        onTertiaryContainer = Color(s.lightOnContainer),
    )
}

fun Palette.darkScheme(): ColorScheme {
    val s = seeds
    return darkColorScheme(
        primary = Color(s.darkPrimary),
        onPrimary = Color(s.darkOnPrimary),
        primaryContainer = Color(s.darkContainer),
        onPrimaryContainer = Color(s.darkOnContainer),
        secondaryContainer = Color(s.darkContainer),
        onSecondaryContainer = Color(s.darkOnContainer),
        tertiary = Color(s.darkPrimary),
        onTertiary = Color(s.darkOnPrimary),
        tertiaryContainer = Color(s.darkContainer),
        onTertiaryContainer = Color(s.darkOnContainer),
    )
}
