package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// cockpit dynamic theme control
object ThemeMode {
    var isDark by mutableStateOf(true)
}

// cockpit dark theme palette
val CockpitBackground: Color
    get() = if (ThemeMode.isDark) Color(0xFF0F1013) else Color(0xFFF9FAFB)

val CockpitSurface: Color
    get() = if (ThemeMode.isDark) Color(0xFF16181D) else Color(0xFFFFFFFF)

val CockpitCard: Color
    get() = if (ThemeMode.isDark) Color(0xFF22252D) else Color(0xFFECEFF1)

val CockpitAccentRed = Color(0xFFFF3344) // Sport Red Indicator
val CockpitAccentAmber = Color(0xFFFFB300) // Warming warning indication
val CockpitAccentGreen = Color(0xFF00E676) // Fuel Efficiency Eco green
val CockpitAccentBlue = Color(0xFF29B6F6) // Diagnostic cooling blue

val CockpitTextPrimary: Color
    get() = if (ThemeMode.isDark) Color(0xFFF3F4F6) else Color(0xFF111827)

val CockpitTextSecondary: Color
    get() = if (ThemeMode.isDark) Color(0xFF9CA3AF) else Color(0xFF5A606A)

val CockpitGridDividers: Color
    get() = if (ThemeMode.isDark) Color(0xFF2E323C) else Color(0xFFE0E0E0)

