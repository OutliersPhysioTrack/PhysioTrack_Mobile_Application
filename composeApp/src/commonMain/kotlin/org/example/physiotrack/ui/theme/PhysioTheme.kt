package org.example.physiotrack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


object PhysioTokens {
    val Primary = Color(0xFF4C4DFF)      // indigo/purple
    val PrimarySoft = Color(0xFFE9E9FF)  // pastel lavender

    // Surfaces
    val Bg = Color(0xFFFFFFFF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceTint = Color(0xFFF7F7FB)
    val Outline = Color(0xFFE8E8F2)
    val SubtleText = Color(0xFF6B7280)  // neutral gray

    // Status
    val SuccessSoft = Color(0xFFE9F8EE)
    val Success = Color(0xFF16A34A)
    val DangerSoft = Color(0xFFFFEAEA)
    val WarningSoft = Color(0xFFFFF4DB)
}

private val LightColorScheme = lightColorScheme(
    primary = PhysioTokens.Primary,
    onPrimary = Color.White,
    background = PhysioTokens.Bg,
    onBackground = Color(0xFF111827),
    surface = PhysioTokens.Surface,
    onSurface = Color(0xFF111827),
    surfaceVariant = PhysioTokens.SurfaceTint,
    onSurfaceVariant = PhysioTokens.SubtleText,
    outline = PhysioTokens.Outline,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9AA4FF),
    onPrimary = Color(0xFF0B102B),

    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EAF2),

    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE6EAF2),

    surfaceVariant = Color(0xFF121B33),
    onSurfaceVariant = Color(0xFF9AA4B2),

    outline = Color(0xFF2B3448),
)

private val PhysioTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun PhysioTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = scheme,
        typography = PhysioTypography,
        content = content
    )
}
