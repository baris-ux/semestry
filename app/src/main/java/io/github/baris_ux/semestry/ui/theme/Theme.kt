package io.github.baris_ux.semestry.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = Sky40,
    onPrimary            = Color.White,
    primaryContainer     = SkyContainer,
    onPrimaryContainer   = Color(0xFF00344D),
    secondary            = SlateGrey40,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFCFD8DC),
    onSecondaryContainer = Color(0xFF0D1F26),
    tertiary             = Orange40,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFFFF3E0),
    onTertiaryContainer  = Color(0xFF3E1400),
    background           = SkySurface,
    surface              = Color.White,
    surfaceVariant       = Color(0xFFE8F4FB),
    onSurfaceVariant     = Color(0xFF3D5A6A),
)

private val DarkColorScheme = darkColorScheme(
    primary              = Sky80,
    onPrimary            = Color(0xFF00344D),
    primaryContainer     = Color(0xFF004E73),
    onPrimaryContainer   = SkyContainer,
    secondary            = SlateGrey80,
    onSecondary          = Color(0xFF1E3640),
    secondaryContainer   = Color(0xFF2E4550),
    onSecondaryContainer = Color(0xFFCFD8DC),
    tertiary             = Orange80,
    onTertiary           = Color(0xFF3E1400),
    tertiaryContainer    = Color(0xFF5C2600),
    onTertiaryContainer  = Color(0xFFFFF3E0),
    background           = Color(0xFF141C22),
    surface              = Color(0xFF1A2530),
    surfaceVariant       = Color(0xFF1E303C),
    onSurfaceVariant     = Color(0xFFB0C8D8),
)

@Composable
fun SemestryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
