package com.acuranza.app.designsystem.theme

import com.acuranza.app.model.AccentColor

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = BrandPalette.SumvaloBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandPalette.SumvaloBlueTint,
    onPrimaryContainer = BrandPalette.SumvaloNight,
    secondary = BrandPalette.SumvaloEmerald,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BrandPalette.SumvaloBlueTint,
    onSecondaryContainer = BrandPalette.SumvaloNight,
    tertiary = BrandPalette.SumvaloBlue,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BrandPalette.SumvaloBlueTint,
    onTertiaryContainer = BrandPalette.SumvaloNight,
    error = Color(0xFFF87171),
    onError = BrandPalette.SumvaloNight,
    errorContainer = Color(0xFF2A1414),
    onErrorContainer = Color(0xFFFECACA),
    background = BrandPalette.SumvaloNight,
    onBackground = BrandPalette.SumvaloSnow,
    surface = BrandPalette.SumvaloNight,
    onSurface = BrandPalette.SumvaloSnow,
    surfaceVariant = BrandPalette.SumvaloNightFill,
    onSurfaceVariant = BrandPalette.SumvaloSnowSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = BrandPalette.SumvaloNight,
    surfaceContainerLow = BrandPalette.SumvaloNightCard,
    surfaceContainer = BrandPalette.SumvaloNightCard,
    surfaceContainerHigh = BrandPalette.SumvaloNightCard,
    surfaceContainerHighest = BrandPalette.SumvaloNightFill,
    outline = BrandPalette.SumvaloNightHairline,
    outlineVariant = BrandPalette.SumvaloNightHairline,
    inverseSurface = BrandPalette.SumvaloSnow,
    inverseOnSurface = BrandPalette.SumvaloNight,
    inversePrimary = BrandPalette.SumvaloBlueDeep,
    scrim = Color.Black,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = BrandPalette.SumvaloBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandPalette.SumvaloBlueTint,
    onPrimaryContainer = BrandPalette.SumvaloBlueDeep,
    secondary = BrandPalette.SumvaloEmerald,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = BrandPalette.SumvaloBlueTint,
    onSecondaryContainer = BrandPalette.SumvaloBlueDeep,
    tertiary = BrandPalette.SumvaloBlue,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = BrandPalette.SumvaloBlueTint,
    onTertiaryContainer = BrandPalette.SumvaloBlueDeep,
    error = BrandPalette.SumvaloRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = BrandPalette.SumvaloRedTint,
    onErrorContainer = Color(0xFF7F1D1D),
    background = BrandPalette.SumvaloPaper,
    onBackground = BrandPalette.SumvaloInk,
    surface = BrandPalette.SumvaloPaper,
    onSurface = BrandPalette.SumvaloInk,
    surfaceVariant = BrandPalette.SumvaloFill,
    onSurfaceVariant = BrandPalette.SumvaloInkSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = BrandPalette.SumvaloFill,
    outline = BrandPalette.SumvaloHairline,
    outlineVariant = BrandPalette.SumvaloDivider,
    inverseSurface = BrandPalette.SumvaloInk,
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = BrandPalette.SumvaloBlueDeep,
    scrim = Color.Black,
)

internal val SumvaloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Spacing scale used across the app (4dp grid). */
@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    /** Horizontal padding for screen content. */
    val screen: Dp = 20.dp,
    /** Corner radius of standard cards. */
    val cardRadius: Dp = 20.dp,
    /** Vertical gap between sections. */
    val section: Dp = 24.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * The Sumvalo theme. Wrap all app content in it:
 *
 * ```kotlin
 * SumvaloTheme { AppNavHost() }
 * ```
 *
 * @param darkTheme dark by default (Sumvalo's first-launch appearance); pass `false` for light.
 * @param dynamicColor use Material You wallpaper colours on Android 12+ (brand colours otherwise).
 * @param accent brand accent colour.
 * @param colorTheme colour theme variant.
 * @param amoled pure-black surfaces (AMOLED), dark mode only.
 */
@Composable
fun SumvaloTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accent: AccentColor = AccentColor.BLUE,
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    amoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDynamic = (colorTheme == ColorTheme.DYNAMIC || dynamicColor) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolved = when {
        useDynamic -> {
            val context = LocalContext.current
            val dyn = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            ResolvedPalette(
                dynamicSumvaloColors(dyn, darkTheme).copy(
                    accent = accent.accentColor(darkTheme),
                    accentTint = accent.accentTint(darkTheme),
                ),
                dyn.copy(primary = accent.accentColor(darkTheme)),
                dyn.background,
            )
        }
        colorTheme != ColorTheme.DEFAULT -> builtInPalette(colorTheme, darkTheme)
        else -> {
            val tokens = (if (darkTheme) DarkSumvaloColors else LightSumvaloColors)
                .copy(accent = accent.accentColor(darkTheme), accentTint = accent.accentTint(darkTheme))
            ResolvedPalette(
                tokens,
                (if (darkTheme) DarkScheme else LightScheme).copy(primary = tokens.accent),
                if (darkTheme) BrandPalette.SumvaloNight else BrandPalette.SumvaloPaper,
            )
        }
    }
    // AMOLED black: pure-black canvas with near-black layers, dark mode only.
    val applyAmoled = amoled && darkTheme
    val tokens = if (applyAmoled) resolved.tokens.amoled() else resolved.tokens
    val scheme = if (applyAmoled) resolved.scheme.amoled() else resolved.scheme
    CompositionLocalProvider(
        LocalSumvaloColors provides tokens,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SumvaloTypography,
            shapes = SumvaloShapes,
        ) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

/** Accessors for the Sumvalo theme values. */
object SumvaloTheme {
    val colors: SumvaloColors
        @Composable @ReadOnlyComposable get() = LocalSumvaloColors.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

/** Backward-compatible aliases removed: SumvaloTheme and SumvaloText are already objects. */
