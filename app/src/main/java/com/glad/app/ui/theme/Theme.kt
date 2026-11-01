package com.glad.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glad.app.data.prefs.AppSettings
import com.glad.app.data.prefs.ThemeMode

data class Accent(val name: String, val color: Color)

val Accents = listOf(
    Accent("Sunrise", Color(0xFFFFB84D)),
    Accent("Coral", Color(0xFFFF6B7A)),
    Accent("Aurora", Color(0xFF8F7CFF)),
    Accent("Lagoon", Color(0xFF19C3B1)),
    Accent("Bloom", Color(0xFFFF5FA2)),
    Accent("Sky", Color(0xFF4DA3FF)),
    Accent("Mint", Color(0xFF5BD68A)),
    Accent("Orchid", Color(0xFFC66BFF))
)

@Immutable
data class BrandColors(val start: Color, val end: Color) {
    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
    fun soft(alpha: Float = 0.18f): Brush =
        Brush.linearGradient(listOf(start.copy(alpha = alpha), end.copy(alpha = alpha * 0.6f)))
}

val LocalBrand = staticCompositionLocalOf { BrandColors(Color(0xFFFFB84D), Color(0xFFFF6B7A)) }

fun shiftHue(color: Color, degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun gladColorScheme(accent: Color, dark: Boolean, amoled: Boolean): ColorScheme {
    val onAccent = if (accent.luminance() > 0.4f) Color(0xFF1A1226) else Color.White
    return if (dark) {
        val bg = if (amoled) Color.Black else Color(0xFF0D0A14)
        val surface = if (amoled) Color(0xFF08080A) else Color(0xFF131020)
        val container = if (amoled) Color(0xFF111114) else Color(0xFF1B1727)
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = lerp(container, accent, 0.30f),
            onPrimaryContainer = lerp(accent, Color.White, 0.75f),
            secondary = lerp(accent, Color.White, 0.30f),
            onSecondary = Color(0xFF1A1226),
            secondaryContainer = lerp(container, accent, 0.16f),
            onSecondaryContainer = lerp(accent, Color.White, 0.8f),
            tertiary = shiftHue(accent, -28f),
            onTertiary = Color(0xFF1A1226),
            background = bg,
            onBackground = Color(0xFFEFEAF8),
            surface = surface,
            onSurface = Color(0xFFEFEAF8),
            surfaceVariant = container,
            onSurfaceVariant = Color(0xFFB4ACC6),
            surfaceTint = accent,
            surfaceContainerLowest = bg,
            surfaceContainerLow = surface,
            surfaceContainer = container,
            surfaceContainerHigh = lerp(container, Color.White, 0.05f),
            surfaceContainerHighest = lerp(container, Color.White, 0.09f),
            outline = Color(0xFF7A728F),
            outlineVariant = Color(0xFF2E2840),
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF2B0A0A)
        )
    } else {
        val primary = lerp(accent, Color.Black, 0.18f)
        lightColorScheme(
            primary = primary,
            onPrimary = if (primary.luminance() > 0.45f) Color(0xFF1A1226) else Color.White,
            primaryContainer = lerp(Color.White, accent, 0.28f),
            onPrimaryContainer = lerp(accent, Color.Black, 0.65f),
            secondary = lerp(accent, Color.Black, 0.35f),
            secondaryContainer = lerp(Color.White, accent, 0.14f),
            onSecondaryContainer = lerp(accent, Color.Black, 0.7f),
            tertiary = shiftHue(primary, -28f),
            background = Color(0xFFFCF9FF),
            onBackground = Color(0xFF1A1623),
            surface = Color(0xFFFCF9FF),
            onSurface = Color(0xFF1A1623),
            surfaceVariant = Color(0xFFF1ECF9),
            onSurfaceVariant = Color(0xFF5E5772),
            surfaceTint = primary,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8F4FD),
            surfaceContainer = Color(0xFFF1ECF9),
            surfaceContainerHigh = Color(0xFFEAE4F4),
            surfaceContainerHighest = Color(0xFFE3DCEF),
            outline = Color(0xFF8D86A1),
            outlineVariant = Color(0xFFDCD5E9),
            error = Color(0xFFD32F4F)
        )
    }
}

private val GladTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
        labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp)
    )
}

private val GladShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun isDarkTheme(settings: AppSettings): Boolean = when (settings.themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun GladTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = isDarkTheme(settings)
    val accent = Accents.getOrElse(settings.accentIndex) { Accents[0] }.color
    val context = LocalContext.current
    val useDynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> gladColorScheme(accent, dark, settings.amoled && dark)
    }
    val brandStart = if (useDynamic) scheme.primary else if (dark) accent else scheme.primary
    val brand = BrandColors(brandStart, shiftHue(brandStart, -28f))
    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(colorScheme = scheme, typography = GladTypography, shapes = GladShapes, content = content)
    }
}

/** Text style that paints glyphs with the brand gradient. */
@Composable
fun brandTextStyle(base: TextStyle): TextStyle = base.copy(brush = LocalBrand.current.brush)
