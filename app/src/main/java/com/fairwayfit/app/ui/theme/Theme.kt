package com.fairwayfit.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Palette {
    val Bg = Color(0xFF0A120E)
    val Surface = Color(0xFF111C16)
    val SurfaceHigh = Color(0xFF17241D)
    val SurfaceHighest = Color(0xFF1E2E25)
    val Lime = Color(0xFFB8F26B)
    val LimeDeep = Color(0xFF1F3A27)
    val Mint = Color(0xFF5EE0B0)
    val Gold = Color(0xFFF2C26B)
    val Coral = Color(0xFFFF8A75)
    val Text = Color(0xFFE6EFE8)
    val TextMuted = Color(0xFF93A89A)
    val Outline = Color(0xFF3A4D41)
    val OutlineSoft = Color(0xFF24342B)
}

private val FairwayColors = darkColorScheme(
    primary = Palette.Lime,
    onPrimary = Color(0xFF0B2A12),
    primaryContainer = Palette.LimeDeep,
    onPrimaryContainer = Color(0xFFD7F7B0),
    secondary = Palette.Mint,
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF16382E),
    onSecondaryContainer = Color(0xFFB9F5DF),
    tertiary = Palette.Gold,
    onTertiary = Color(0xFF3A2800),
    tertiaryContainer = Color(0xFF3D3016),
    onTertiaryContainer = Color(0xFFFFE2A8),
    error = Palette.Coral,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Bg,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextMuted,
    surfaceContainerLowest = Palette.Bg,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHighest,
    outline = Palette.Outline,
    outlineVariant = Palette.OutlineSoft
)

private val Sans = FontFamily.SansSerif

private val FairwayTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-2).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.5).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp)
)

private val FairwayShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/** FairwayFit is dark-only by design: deep green surfaces with a lime accent. */
@Composable
fun FairwayFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FairwayColors,
        typography = FairwayTypography,
        shapes = FairwayShapes,
        content = content
    )
}
