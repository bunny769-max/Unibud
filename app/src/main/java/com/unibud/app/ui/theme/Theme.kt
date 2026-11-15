package com.unibud.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unibud.app.core.model.SupportLevel

val LocalIsDark = staticCompositionLocalOf { true }

// Palette: aubergine ink, lavender for actions, mint for "this really works", amber for "partly".
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4A7FF), onPrimary = Color(0xFF1B1245),
    primaryContainer = Color(0xFF3A2F80), onPrimaryContainer = Color(0xFFE4DEFF),
    secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00302A),
    tertiary = Color(0xFFFFC48A), onTertiary = Color(0xFF3A2000),
    background = Color(0xFF0F0B22), onBackground = Color(0xFFEDEAFF),
    surface = Color(0xFF191336), onSurface = Color(0xFFEDEAFF),
    surfaceVariant = Color(0xFF241C4A), onSurfaceVariant = Color(0xFFB9B2E0),
    outline = Color(0xFF6F68A0), error = Color(0xFFFF8A98),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B47E0), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DEFF), onPrimaryContainer = Color(0xFF2A1B8F),
    secondary = Color(0xFF0F8F7E), onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9A5B00), onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F4FF), onBackground = Color(0xFF1A1636),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1A1636),
    surfaceVariant = Color(0xFFEAE5FF), onSurfaceVariant = Color(0xFF4B4673),
    outline = Color(0xFF8A85B0), error = Color(0xFFC0334A),
)

private fun style(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    headlineLarge = style(FontWeight.SemiBold, 34, 40, -0.8),
    headlineMedium = style(FontWeight.SemiBold, 26, 32, -0.4),
    titleLarge = style(FontWeight.SemiBold, 20, 26, -0.2),
    titleMedium = style(FontWeight.Medium, 16, 22),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 21),
    bodySmall = style(FontWeight.Normal, 12, 18),
    labelLarge = style(FontWeight.Medium, 14, 20),
    labelMedium = style(FontWeight.Medium, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 14),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

fun appBackground(dark: Boolean): Brush = if (dark) {
    Brush.linearGradient(listOf(Color(0xFF0F0B22), Color(0xFF1B1350), Color(0xFF0B1A2E)))
} else {
    Brush.linearGradient(listOf(Color(0xFFF7F4FF), Color(0xFFEAE6FF), Color(0xFFE3F5F2)))
}

fun supportColor(level: SupportLevel, dark: Boolean): Color = when (level) {
    SupportLevel.SUPPORTED -> if (dark) Color(0xFF5EEAD4) else Color(0xFF0F8F7E)
    SupportLevel.PARTIAL -> if (dark) Color(0xFFFFC48A) else Color(0xFF9A5B00)
    SupportLevel.UNAVAILABLE -> if (dark) Color(0xFF8C86B8) else Color(0xFF6B668F)
}

@Composable
fun UniBudTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
