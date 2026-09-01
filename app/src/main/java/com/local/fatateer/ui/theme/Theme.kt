package com.local.fatateer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF0F3D3E)
private val GreenLight = Color(0xFF2A9D8F)
private val Danger = Color(0xFFC44536)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = GreenLight,
    onSecondary = Color.White,
    background = Color(0xFFF4F7F6),
    onBackground = Color(0xFF122021),
    surface = Color.White,
    onSurface = Color(0xFF122021),
    surfaceVariant = Color(0xFFE8EEED),
    onSurfaceVariant = Color(0xFF3D4F50),
    error = Danger,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6A8),
    onPrimary = Color(0xFF00332F),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF00332F),
    background = Color(0xFF0E1515),
    onBackground = Color(0xFFE2EAE9),
    surface = Color(0xFF1A2222),
    onSurface = Color(0xFFE2EAE9),
    surfaceVariant = Color(0xFF2A3333),
    onSurfaceVariant = Color(0xFFB0BEBD),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0000)
)

@Composable
fun FatateerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
