package com.unibud.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colours of the neon-on-ink look. Every component reads from here, so light and dark stay in step. */
@Immutable
class NeoColors(
    val dark: Boolean,
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val panel: Color,
    val panelStrong: Color,
    val text: Color,
    val textDim: Color,
    val violet: Color,
    val blue: Color,
    val cyan: Color,
    val green: Color,
    val amber: Color,
    val red: Color,
) {
    val accent: Brush get() = Brush.horizontalGradient(listOf(blue, violet))
    val background: Brush get() = Brush.linearGradient(listOf(bgTop, bgMid, bgBottom))
}

val NeoDark = NeoColors(
    dark = true,
    bgTop = Color(0xFF05060F), bgMid = Color(0xFF110B33), bgBottom = Color(0xFF060A1E),
    panel = Color(0x14FFFFFF), panelStrong = Color(0x26FFFFFF),
    text = Color(0xFFF1EFFF), textDim = Color(0xFFA9A6D8),
    violet = Color(0xFF8B5CFF), blue = Color(0xFF4C6FFF), cyan = Color(0xFF38C8FF),
    green = Color(0xFF2ED47A), amber = Color(0xFFFFB84D), red = Color(0xFFFF5C73),
)

val NeoLight = NeoColors(
    dark = false,
    bgTop = Color(0xFFF6F4FF), bgMid = Color(0xFFE9E5FF), bgBottom = Color(0xFFE2F3FA),
    panel = Color(0xB3FFFFFF), panelStrong = Color(0xFFE3DEFF),
    text = Color(0xFF191536), textDim = Color(0xFF5A5585),
    violet = Color(0xFF6A3DF0), blue = Color(0xFF3556E8), cyan = Color(0xFF0A8FC0),
    green = Color(0xFF14A85C), amber = Color(0xFFB36B00), red = Color(0xFFD1344B),
)

val LocalNeo = staticCompositionLocalOf { NeoDark }

object Neo {
    val colors: NeoColors
        @Composable @ReadOnlyComposable get() = LocalNeo.current
}

private fun style(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    headlineLarge = style(FontWeight.Bold, 34, 40, -0.8),
    headlineMedium = style(FontWeight.SemiBold, 26, 32, -0.4),
    titleLarge = style(FontWeight.SemiBold, 20, 26, -0.2),
    titleMedium = style(FontWeight.SemiBold, 16, 22),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 21),
    bodySmall = style(FontWeight.Normal, 12, 17),
    labelLarge = style(FontWeight.SemiBold, 14, 20),
    labelMedium = style(FontWeight.Medium, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 14),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

@Composable
fun UniBudTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) NeoDark else NeoLight
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.violet, onPrimary = Color.White, secondary = c.cyan, tertiary = c.amber,
            background = c.bgTop, onBackground = c.text, surface = c.bgMid, onSurface = c.text,
            surfaceVariant = c.panelStrong, onSurfaceVariant = c.textDim, outline = c.textDim, error = c.red,
        )
    } else {
        lightColorScheme(
            primary = c.violet, onPrimary = Color.White, secondary = c.cyan, tertiary = c.amber,
            background = c.bgTop, onBackground = c.text, surface = Color.White, onSurface = c.text,
            surfaceVariant = c.panelStrong, onSurfaceVariant = c.textDim, outline = c.textDim, error = c.red,
        )
    }
    CompositionLocalProvider(LocalNeo provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
