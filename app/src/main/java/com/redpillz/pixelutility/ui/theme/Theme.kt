package com.redpillz.pixelutility.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedPillzDarkColors = darkColorScheme(
    primary = Color(0xFFFF5F5F),
    onPrimary = Color(0xFF240707),
    secondary = Color(0xFFE0A2A2),
    background = Color(0xFF09090B),
    surface = Color(0xFF121316),
    surfaceVariant = Color(0xFF1B1C20),
    onSurface = Color(0xFFF3F3F4),
    onSurfaceVariant = Color(0xFFC9CBD1),
    error = Color(0xFFFF897D),
)

@Composable
fun RedPillzTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = RedPillzDarkColors.takeIf { isSystemInDarkTheme() } ?: RedPillzDarkColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

