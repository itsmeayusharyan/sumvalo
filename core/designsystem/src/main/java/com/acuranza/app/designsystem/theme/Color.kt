package com.acuranza.app.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.ColorScheme
import com.acuranza.app.model.AccentColor

/**
 * The Sumvalo palette.
 *
 * Dark (the default) is a deep slate-navy with clearly layered surfaces: background, card and
 * fill step apart in lightness while hue stays constant. Text uses one ink ramp; colour is spent
 * only where it carries meaning — emerald for money in, red for attention, the brand blue
 * for actions. Light mode mirrors the same structure. There are deliberately no gradients.
 */
object BrandPalette {
    // Neutrals (light)
    val SumvaloPaper = Color(0xFFF5F6F8)
    val SumvaloCard = Color(0xFFFFFFFF)
    val SumvaloHairline = Color(0xFFE6E8ED)
    val SumvaloDivider = Color(0xFFEEF0F3)
    val SumvaloInk = Color(0xFF0F1728)
    val SumvaloInkSecondary = Color(0xFF4B5565)
    val SumvaloInkTertiary = Color(0xFF8A94A6)
    val SumvaloFill = Color(0xFFEFF1F4)

    // Neutrals (dark) — deep slate-navy, not black
    val SumvaloNight = Color(0xFF0B1220)
    val SumvaloNightCard = Color(0xFF151E2E)
    val SumvaloNightHairline = Color(0xFF2A3850)
    val SumvaloNightFill = Color(0xFF1D2737)
    val SumvaloSnow = Color(0xFFF3F6FB)
    val SumvaloSnowSecondary = Color(0xFF9FADC4)
    val SumvaloSnowTertiary = Color(0xFF6C7B96)

    // Accent — the brand blue
    val SumvaloBlue = Color(0xFF2563EB)
    val SumvaloBlueDeep = Color(0xFF1D4ED8)
    val SumvaloBlueBright = Color(0xFF66A3FF)
    val SumvaloBlueTint = Color(0xFFEFF6FF)
    val SumvaloBlueTintDark = Color(0xFF16233C)

    // Money in — the only green
    val SumvaloEmerald = Color(0xFF047857)
    val SumvaloEmeraldDeep = Color(0xFF047857)
    val SumvaloEmeraldBright = Color(0xFF34D399)
    val SumvaloEmeraldTint = Color(0xFFECFDF5)
    val SumvaloEmeraldTintDark = Color(0xFF0E2E24)

    // Status
    val SumvaloRed = Color(0xFFDC2626)
    val SumvaloRedTint = Color(0xFFFEE2E2)
    val SumvaloAmber = Color(0xFFB45309)
    val SumvaloAmberTint = Color(0xFFFFFBEB)

    /** Kept for compatibility; Sumvalo uses no gradients, so this is flat colour. */
    val brandGradient: Brush get() = SolidColor(SumvaloInk)

    /** Kept for compatibility; flat brand blue. */
    val goldGradient: Brush get() = SolidColor(SumvaloBlue)
}

/**
 * Semantic colours that Material3's [ColorScheme] has no slot for.
 * Read them through [SumvaloTheme.colors].
 */
@Immutable
data class SumvaloColors(
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

internal val LightSumvaloColors = SumvaloColors(
    income = BrandPalette.SumvaloEmerald,
    expense = BrandPalette.SumvaloInk,
    transfer = BrandPalette.SumvaloInkSecondary,
    warning = BrandPalette.SumvaloAmber,
    alert = BrandPalette.SumvaloRed,
    good = BrandPalette.SumvaloEmerald,
    tip = BrandPalette.SumvaloBlue,
    gold = BrandPalette.SumvaloBlue,
    cardSurface = BrandPalette.SumvaloCard,
    cardSurfaceHigh = BrandPalette.SumvaloFill,
    glass = BrandPalette.SumvaloCard,
    glassBorder = BrandPalette.SumvaloHairline,
    subtleText = BrandPalette.SumvaloInkSecondary,
    divider = BrandPalette.SumvaloDivider,
    shimmerBase = Color(0xFFE9EBEF),
    shimmerHighlight = BrandPalette.SumvaloPaper,
    isDark = false,
    ink = BrandPalette.SumvaloInk,
    onInk = Color.White,
    tertiaryText = BrandPalette.SumvaloInkTertiary,
    hairline = BrandPalette.SumvaloHairline,
    accent = BrandPalette.SumvaloBlue,
    accentTint = BrandPalette.SumvaloBlueTint,
    alertTint = BrandPalette.SumvaloRedTint,
    warningTint = BrandPalette.SumvaloAmberTint,
    tipTint = BrandPalette.SumvaloBlueTint,
    fill = BrandPalette.SumvaloFill,
)

internal val DarkSumvaloColors = SumvaloColors(
    income = BrandPalette.SumvaloEmeraldBright,
    expense = BrandPalette.SumvaloSnow,
    transfer = BrandPalette.SumvaloSnowSecondary,
    warning = Color(0xFFFBBF24),
    alert = Color(0xFFF87171),
    good = BrandPalette.SumvaloEmeraldBright,
    tip = BrandPalette.SumvaloBlueBright,
    gold = BrandPalette.SumvaloBlueBright,
    cardSurface = BrandPalette.SumvaloNightCard,
    cardSurfaceHigh = BrandPalette.SumvaloNightFill,
    glass = BrandPalette.SumvaloNightCard,
    glassBorder = BrandPalette.SumvaloNightHairline,
    subtleText = BrandPalette.SumvaloSnowSecondary,
    divider = BrandPalette.SumvaloNightHairline,
    shimmerBase = BrandPalette.SumvaloNightCard,
    shimmerHighlight = BrandPalette.SumvaloNightFill,
    isDark = true,
    ink = BrandPalette.SumvaloSnow,
    onInk = BrandPalette.SumvaloNight,
    tertiaryText = BrandPalette.SumvaloSnowTertiary,
    hairline = BrandPalette.SumvaloNightHairline,
    accent = BrandPalette.SumvaloBlueBright,
    accentTint = BrandPalette.SumvaloBlueTintDark,
    alertTint = Color(0xFF2C1518),
    warningTint = Color(0xFF2B2312),
    tipTint = BrandPalette.SumvaloBlueTintDark,
    fill = BrandPalette.SumvaloNightFill,
)

val LocalSumvaloColors = staticCompositionLocalOf { LightSumvaloColors }

/**
 * Resolves a user-chosen [com.acuranza.app.model.AccentColor] onto the theme's lightness scale:
 * the bright variant for dark surfaces, the deep one for light, and an 8% wash for tints, so every
 * accent keeps the same contrast relationships the brand blue has.
 */
fun AccentColor.accentColor(dark: Boolean): Color = when (this) {
    AccentColor.BLUE -> if (dark) BrandPalette.SumvaloBlueBright else BrandPalette.SumvaloBlue
    AccentColor.TEAL -> if (dark) Color(0xFF2DD4BF) else Color(0xFF0D9488)
    AccentColor.VIOLET -> if (dark) Color(0xFFA78BFA) else Color(0xFF7C3AED)
    AccentColor.ROSE -> if (dark) Color(0xFFFB7185) else Color(0xFFE11D48)
}

/** The low-opacity wash of the accent used behind icons, chips, active fills and the nav dot. */
fun AccentColor.accentTint(dark: Boolean): Color = when (this) {
    AccentColor.BLUE -> if (dark) BrandPalette.SumvaloBlueTintDark else BrandPalette.SumvaloBlueTint
    AccentColor.TEAL -> if (dark) Color(0xFF0E2E2A) else Color(0xFFEBF8F6)
    AccentColor.VIOLET -> if (dark) Color(0xFF241E42) else Color(0xFFF4F0FE)
    AccentColor.ROSE -> if (dark) Color(0xFF2C181F) else Color(0xFFFFEFF2)
}

/** Pure-black surfaces for AMOLED, dark mode only — layered off true black for depth. */
internal fun SumvaloColors.amoled(): SumvaloColors = copy(
    cardSurface = Color(0xFF0B0B0F),
    cardSurfaceHigh = Color(0xFF141419),
    glass = Color(0xFF0B0B0F),
    fill = Color(0xFF141419),
    hairline = Color(0xFF232330),
    divider = Color(0xFF232330),
    shimmerBase = Color(0xFF0F0F14),
    shimmerHighlight = Color(0xFF17171D),
)

/** The Material scheme for AMOLED: true-black canvas, near-black containers. */
internal fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0F),
    surfaceContainer = Color(0xFF0B0B0F),
    surfaceContainerHigh = Color(0xFF0B0B0F),
    surfaceContainerHighest = Color(0xFF141419),
)
