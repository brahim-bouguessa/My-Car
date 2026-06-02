package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit,
) {
    ThemeMode.isDark = darkTheme
    
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = CockpitAccentRed,
            secondary = CockpitAccentAmber,
            tertiary = CockpitAccentGreen,
            background = CockpitBackground,
            surface = CockpitSurface,
            onBackground = CockpitTextPrimary,
            onSurface = CockpitTextPrimary,
            onPrimary = CockpitTextPrimary,
            onSecondary = CockpitBackground,
            onTertiary = CockpitBackground,
            surfaceVariant = CockpitCard,
            onSurfaceVariant = CockpitTextSecondary,
            outline = CockpitGridDividers
        )
    } else {
        lightColorScheme(
            primary = CockpitAccentRed,
            secondary = CockpitAccentAmber,
            tertiary = CockpitAccentGreen,
            background = CockpitBackground,
            surface = CockpitSurface,
            onBackground = CockpitTextPrimary,
            onSurface = CockpitTextPrimary,
            onPrimary = Color.White,
            onSecondary = CockpitBackground,
            onTertiary = CockpitBackground,
            surfaceVariant = CockpitCard,
            onSurfaceVariant = CockpitTextSecondary,
            outline = CockpitGridDividers
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

