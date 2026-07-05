package com.fossisawesome.ventus.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// Typed color tokens exposed to every composable via CompositionLocal — no MaterialTheme.
data class AppColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val error: Color,
) {
    val border: Color get() = surface2.copy(alpha = 0.4f)
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No VentusTheme provided")
}

val LocalAppIsDark = staticCompositionLocalOf { true }

val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

fun AppTheme.toAppColors() = AppColors(
    bg = bg, surface = surface, surface2 = surface2,
    text = text, muted = muted, accent = accent, error = error,
)
