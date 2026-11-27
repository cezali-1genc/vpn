package com.cloudflaresecure.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Spec §8: koyu lacivert + siyah + beyaz + kontrollü mavi; yeşil/kırmızı durum
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF001233),
    secondary = Color(0xFF7FB8FF),
    background = Color(0xFF05070D),
    surface = Color(0xFF0D1424),
    surfaceVariant = Color(0xFF16213A),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    error = Color(0xFFFF5252)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0057C2),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2F6FED),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EEF7),
    onBackground = Color(0xFF0A1120),
    onSurface = Color(0xFF0A1120),
    error = Color(0xFFB3261E)
)

@Composable
fun CloudflareVpnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
