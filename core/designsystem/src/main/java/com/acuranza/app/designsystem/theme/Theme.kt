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
    primary = BrandPalette.BlueBright,
    onPrimary = BrandPalette.Night,
    primaryContainer = BrandPalette.BlueTintDark,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = BrandPalette.EmeraldBright,
    onSecondary = BrandPalette.Night,
    secondaryContainer = BrandPalette.EmeraldTintDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = BrandPalette.BlueBright,
    onTertiary = BrandPalette.Night,
    tertiaryContainer = BrandPalette.BlueTintDark,
    onTertiaryContainer = Color(0xFFBFDBFE),
    error = Color(0xFFF87171),
    onError = BrandPalette.Night,
    errorContainer = Color(0xFF2A1414),
    onErrorContainer = Color(0xFFFECACA),
    background = BrandPalette.Night,
    onBackground = BrandPalette.Snow,
    surface = BrandPalette.Night,
    onSurface = BrandPalette.Snow,
    surfaceVariant = BrandPalette.NightFill,
    onSurfaceVariant = BrandPalette.SnowSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = BrandPalette.Night,
    surfaceContainerLow = BrandPalette.NightCard,
    surfaceContainer = BrandPalette.NightCard,
    surfaceContainerHigh = BrandPalette.NightCard,
    surfaceContainerHighest = BrandPalette.NightFill,
    outline = BrandPalette.NightHairline,
    outlineVariant = BrandPalette.NightHairline,
    inverseSurface = BrandPalette.Snow,
    inverseOnSurface = BrandPalette.Night,
    inversePrimary = BrandPalette.BlueDeep,
    scrim = Color.Black,
)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = BrandPalette.Blue,
    onPrimary = Color.White,
    primaryContainer = BrandPalette.BlueTint,
    onPrimaryContainer = BrandPalette.BlueDeep,
    secondary = BrandPalette.Emerald,
    onSecondary = Color.White,
    secondaryContainer = BrandPalette.EmeraldTint,
    onSecondaryContainer = BrandPalette.EmeraldDeep,
    tertiary = BrandPalette.Blue,
    onTertiary = Color.White,
    tertiaryContainer = BrandPalette.BlueTint,
    onTertiaryContainer = BrandPalette.BlueDeep,
    error = BrandPalette.Red,
    onError = Color.White,
    errorContainer = BrandPalette.RedTint,
    onErrorContainer = Color(0xFF7F1D1D),
    background = BrandPalette.Paper,
    onBackground = BrandPalette.Ink,
    surface = BrandPalette.Paper,
    onSurface = BrandPalette.Ink,
    surfaceVariant = BrandPalette.Fill,
    onSurfaceVariant = BrandPalette.InkSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = BrandPalette.Fill,
    outline = BrandPalette.Hairline,
    outlineVariant = BrandPalette.Divider,
    inverseSurface = BrandPalette.Ink,
    inverseOnSurface = Color.White,
    inversePrimary = BrandPalette.BlueDeep,
    scrim = Color.Black,
)

internal val AcuranzaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
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
    /** Corner radius of standard cards — 20dp everywhere, large surfaces read expressive, not fussy. */
    val cardRadius: Dp = 20.dp,
    /** Vertical gap between sections. */
    val section: Dp = 24.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/**
 * The Acuranza theme. Wrap all app content in it:
 *
 * ```
 * AcuranzaTheme { AppNavHost() }
 * ```
 *
 * Material slots come from [MaterialTheme]; extra semantic colours from [AcuranzaTheme.colors]
 * and spacing from [AcuranzaTheme.spacing].
 *
 * @param darkTheme dark by default (Acuranza's first-launch appearance); pass `false` for light.
 * @param dynamicColor use Material You wallpaper colours on Android 12+ (brand colours otherwise).
 */
@Composable
fun AcuranzaTheme(
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
                dynamicAcuranzaColors(dyn, darkTheme).copy(
                    accent = accent.accentColor(darkTheme),
                    accentTint = accent.accentTint(darkTheme),
                ),
                dyn.copy(primary = accent.accentColor(darkTheme)),
                dyn.background,
            )
        }
        colorTheme != ColorTheme.DEFAULT -> builtInPalette(colorTheme, darkTheme)
        else -> {
            val tokens = (if (darkTheme) DarkAcuranzaColors else LightAcuranzaColors)
                .copy(accent = accent.accentColor(darkTheme), accentTint = accent.accentTint(darkTheme))
            ResolvedPalette(
                tokens,
                (if (darkTheme) DarkScheme else LightScheme).copy(primary = tokens.accent),
                if (darkTheme) BrandPalette.Night else BrandPalette.Paper,
            )
        }
    }
    // AMOLED black: pure-black canvas with near-black layers, dark mode only. In light mode the
    // preference is preserved but inactive (the UI says so) — no washed-out pseudo-dark. Works
    // over every palette including Dynamic (it re-tints surfaces, accents stay).
    val applyAmoled = amoled && darkTheme
    val acuranza = if (applyAmoled) resolved.tokens.amoled() else resolved.tokens
    val scheme = if (applyAmoled) resolved.scheme.amoled() else resolved.scheme
    CompositionLocalProvider(
        LocalAcuranzaColors provides acuranza,
        LocalSpacing provides Spacing(),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AcuranzaTypography,
            shapes = AcuranzaShapes,
        ) {
            // Text outside a Surface would otherwise default to black, which vanishes on the dark theme.
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

/** Accessors for the Acuranza-specific theme values. */
object AcuranzaTheme {
    val colors: AcuranzaColors
        @Composable @ReadOnlyComposable get() = LocalAcuranzaColors.current

    val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
}
