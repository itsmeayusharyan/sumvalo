package com.acuranza.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import com.acuranza.app.model.AccentColor

/**
 * The Acuranza palette.
 *
 * Dark (the default) is a deep slate-navy with clearly layered surfaces: background, card and
 * fill step apart in lightness while hue stays constant. Text uses one ink ramp; colour is spent
 * only where it carries meaning — green for money in, amber/red for attention, the brand blue
 * for actions. Light mode mirrors the same structure. There are deliberately no gradients.
 */
object BrandPalette {
    // Neutrals (light)
    val Paper = Color(0xFFF5F6F8)
    val Card = Color(0xFFFFFFFF)
    val Hairline = Color(0xFFE6E8ED)
    val Divider = Color(0xFFEEF0F3)
    val Ink = Color(0xFF0F1728)
    val InkSecondary = Color(0xFF4B5565)
    val InkTertiary = Color(0xFF8A94A6)
    val Fill = Color(0xFFEFF1F4)

    // Neutrals (dark) — deep slate-navy, not black
    val Night = Color(0xFF0B1220)
    val NightCard = Color(0xFF151E2E)
    val NightHairline = Color(0xFF2A3850)
    val NightFill = Color(0xFF1D2737)
    val Snow = Color(0xFFF3F6FB)
    val SnowSecondary = Color(0xFF9FADC4)
    val SnowTertiary = Color(0xFF6C7B96)

    // Accent — the brand blue
    val Blue = Color(0xFF2563EB)
    val BlueDeep = Color(0xFF1D4ED8)
    val BlueBright = Color(0xFF66A3FF)
    val BlueTint = Color(0xFFEFF6FF)
    val BlueTintDark = Color(0xFF16233C)

    // Money in — the only green
    val Emerald = Color(0xFF047857)
    val EmeraldDeep = Color(0xFF047857)
    val EmeraldBright = Color(0xFF34D399)
    val EmeraldTint = Color(0xFFECFDF5)
    val EmeraldTintDark = Color(0xFF0E2E24)

    // Status
    val Red = Color(0xFFDC2626)
    val RedTint = Color(0xFFFEE2E2)
    val Amber = Color(0xFFB45309)
    val AmberTint = Color(0xFFFFFBEB)

    /** Kept for compatibility; Acuranza uses no gradients, so this is flat colour. */
    val brandGradient: Brush get() = SolidColor(Ink)

    /** Kept for compatibility; flat brand blue. */
    val goldGradient: Brush get() = SolidColor(Blue)
}

/**
 * Semantic colours that Material3's [androidx.compose.material3.ColorScheme] has no slot for.
 * Read them through [AcuranzaTheme.colors].
 */
@Immutable
data class AcuranzaColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warning: Color,
    val alert: Color,
    val good: Color,
    val tip: Color,
    val gold: Color,
    val cardSurface: Color,
    val cardSurfaceHigh: Color,
    val glass: Color,
    val glassBorder: Color,
    val subtleText: Color,
    val divider: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val isDark: Boolean,
    /** Primary text / ink buttons. */
    val ink: Color,
    /** Text on ink buttons. */
    val onInk: Color,
    val tertiaryText: Color,
    /** 1dp card outline. */
    val hairline: Color,
    val accent: Color,
    val accentTint: Color,
    val alertTint: Color,
    val warningTint: Color,
    val tipTint: Color,
    /** Neutral fill for icon tiles, chips and inputs. */
    val fill: Color,
) {
    /** Kept for compatibility; a flat surface instead of a gradient. */
    val heroGradient: Brush get() = SolidColor(cardSurface)
}

internal val LightAcuranzaColors = AcuranzaColors(
    income = BrandPalette.Emerald,
    expense = BrandPalette.Ink,
    transfer = BrandPalette.InkSecondary,
    warning = BrandPalette.Amber,
    alert = BrandPalette.Red,
    good = BrandPalette.Emerald,
    tip = BrandPalette.Blue,
    gold = BrandPalette.Blue,
    cardSurface = BrandPalette.Card,
    cardSurfaceHigh = BrandPalette.Fill,
    glass = BrandPalette.Card,
    glassBorder = BrandPalette.Hairline,
    subtleText = BrandPalette.InkSecondary,
    divider = BrandPalette.Divider,
    shimmerBase = Color(0xFFE9EBEF),
    shimmerHighlight = BrandPalette.Paper,
    isDark = false,
    ink = BrandPalette.Ink,
    onInk = Color.White,
    tertiaryText = BrandPalette.InkTertiary,
    hairline = BrandPalette.Hairline,
    accent = BrandPalette.Blue,
    accentTint = BrandPalette.BlueTint,
    alertTint = BrandPalette.RedTint,
    warningTint = BrandPalette.AmberTint,
    tipTint = BrandPalette.BlueTint,
    fill = BrandPalette.Fill,
)

internal val DarkAcuranzaColors = AcuranzaColors(
    income = BrandPalette.EmeraldBright,
    expense = BrandPalette.Snow,
    transfer = BrandPalette.SnowSecondary,
    warning = Color(0xFFFBBF24),
    alert = Color(0xFFF87171),
    good = BrandPalette.EmeraldBright,
    tip = BrandPalette.BlueBright,
    gold = BrandPalette.BlueBright,
    cardSurface = BrandPalette.NightCard,
    cardSurfaceHigh = BrandPalette.NightFill,
    glass = BrandPalette.NightCard,
    glassBorder = BrandPalette.NightHairline,
    subtleText = BrandPalette.SnowSecondary,
    divider = BrandPalette.NightHairline,
    shimmerBase = BrandPalette.NightCard,
    shimmerHighlight = BrandPalette.NightFill,
    isDark = true,
    ink = BrandPalette.Snow,
    onInk = BrandPalette.Night,
    tertiaryText = BrandPalette.SnowTertiary,
    hairline = BrandPalette.NightHairline,
    accent = BrandPalette.BlueBright,
    accentTint = BrandPalette.BlueTintDark,
    alertTint = Color(0xFF2C1518),
    warningTint = Color(0xFF2B2312),
    tipTint = BrandPalette.BlueTintDark,
    fill = BrandPalette.NightFill,
)

val LocalAcuranzaColors = staticCompositionLocalOf { LightAcuranzaColors }

/**
 * Resolves a user-chosen [com.acuranza.app.model.AccentColor] onto the theme's lightness scale:
 * the bright variant for dark surfaces, the deep one for light, and an 8% wash for tints, so every
 * accent keeps the same contrast relationships the brand blue has.
 */
fun AccentColor.accentColor(dark: Boolean): Color = when (this) {
    AccentColor.BLUE -> if (dark) BrandPalette.BlueBright else BrandPalette.Blue
    AccentColor.TEAL -> if (dark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
    AccentColor.VIOLET -> if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    AccentColor.ROSE -> if (dark) Color(0xFFFB7185) else Color(0xFFE11D48)
}

/** The low-opacity wash of the accent used behind icons, chips, active fills and the nav dot. */
fun AccentColor.accentTint(dark: Boolean): Color = when (this) {
    AccentColor.BLUE -> if (dark) BrandPalette.BlueTintDark else BrandPalette.BlueTint
    AccentColor.TEAL -> if (dark) Color(0xFF0E2E2A) else Color(0xFFEBF8F6)
    AccentColor.VIOLET -> if (dark) Color(0xFF241E42) else Color(0xFFF4F0FE)
    AccentColor.ROSE -> if (dark) Color(0xFF2C181F) else Color(0xFFFFEFF2)
}
