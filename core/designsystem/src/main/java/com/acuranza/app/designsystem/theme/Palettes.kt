package com.acuranza.app.designsystem.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The app color themes offered in Appearance → Color theme. DEFAULT is the brand palette the app
 * has always had; the others are full, hand-tuned light+dark token sets; DYNAMIC derives from the
 * Android wallpaper on API 31+ and is unavailable below it (the UI explains that).
 */
enum class ColorTheme {
    DEFAULT,
    DYNAMIC,
    CATPPUCCIN,
    CLOUD,
    OCEAN,
    FOREST,
    ROSE,
    ;

    /** DYNAMIC needs Material You (Android 12+); every built-in palette works everywhere. */
    val supported: Boolean get() = this != DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val label: String
        get() = when (this) {
            DEFAULT -> "Default"
            DYNAMIC -> "Dynamic"
            CATPPUCCIN -> "Catppuccin"
            CLOUD -> "Cloud / Amber"
            OCEAN -> "Ocean"
            FOREST -> "Forest"
            ROSE -> "Rose"
        }
}

/** [ColorTheme.supported] with an explicit SDK argument, for settings logic outside composition. */
fun ColorTheme.isSupported(sdkInt: Int): Boolean = this != ColorTheme.DYNAMIC || sdkInt >= Build.VERSION_CODES.S

/**
 * Wallpaper-derived tokens (API 31+). Surfaces, text and chrome come from the system scheme; the
 * financial semantic colours (income/expense/warning/alert) stay anchored to the Default palette
 * so money keeps the same meaning in every theme — and direction is never colour-only (rows carry
 * arrows and signs).
 */
internal fun dynamicSumvaloColors(scheme: ColorScheme, dark: Boolean): SumvaloColors {
    val semantic = if (dark) DarkSumvaloColors else LightSumvaloColors
    return SumvaloColors(
        income = semantic.income,
        expense = semantic.expense,
        transfer = semantic.transfer,
        warning = semantic.warning,
        alert = semantic.alert,
        good = semantic.good,
        tip = semantic.tip,
        gold = semantic.gold,
        cardSurface = scheme.surfaceContainerHigh,
        cardSurfaceHigh = scheme.surfaceContainerHighest,
        glass = scheme.surfaceContainerHigh,
        glassBorder = scheme.outlineVariant,
        subtleText = scheme.onSurfaceVariant,
        divider = scheme.outlineVariant,
        shimmerBase = scheme.surfaceContainer,
        shimmerHighlight = scheme.surfaceContainerHigh,
        isDark = dark,
        ink = scheme.inverseSurface,
        onInk = scheme.inverseOnSurface,
        tertiaryText = scheme.onSurfaceVariant,
        hairline = scheme.outlineVariant,
        accent = scheme.primary,
        accentTint = scheme.primaryContainer,
        alertTint = semantic.alertTint,
        warningTint = semantic.warningTint,
        tipTint = scheme.secondaryContainer,
        fill = scheme.surfaceContainer,
    )
}

/** Build a Material scheme from a palette's tokens plus its base background colour. */
private fun tokensToScheme(tokens: SumvaloColors, background: Color): ColorScheme {
    val base = if (tokens.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = tokens.accent,
        onPrimary = tokens.onInk,
        primaryContainer = tokens.accentTint,
        onPrimaryContainer = tokens.ink,
        secondary = tokens.tip,
        onSecondary = tokens.onInk,
        secondaryContainer = tokens.tipTint,
        onSecondaryContainer = tokens.ink,
        tertiary = tokens.tip,
        onTertiary = tokens.onInk,
        tertiaryContainer = tokens.tipTint,
        onTertiaryContainer = tokens.ink,
        error = tokens.alert,
        onError = if (tokens.isDark) Color(0xFF1A0A0C) else Color.White,
        errorContainer = tokens.alertTint,
        onErrorContainer = tokens.alert,
        background = background,
        onBackground = tokens.ink,
        surface = background,
        onSurface = tokens.ink,
        surfaceVariant = tokens.fill,
        onSurfaceVariant = tokens.subtleText,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = background,
        surfaceContainerLow = tokens.cardSurface,
        surfaceContainer = tokens.cardSurface,
        surfaceContainerHigh = tokens.cardSurface,
        surfaceContainerHighest = tokens.cardSurfaceHigh,
        outline = tokens.hairline,
        outlineVariant = tokens.divider,
        inverseSurface = tokens.ink,
        inverseOnSurface = tokens.onInk,
        inversePrimary = tokens.accent,
        scrim = Color.Black,
    )
}

/** Resolved theme for a palette: the token set, its Material scheme and its base background. */
data class ResolvedPalette(val tokens: SumvaloColors, val scheme: ColorScheme, val background: Color)

// ── Catppuccin: coordinated pastels — Mocha for dark, Latte for light ──────────────────────────
private fun catppuccinTokens(dark: Boolean) = if (dark) SumvaloColors(
    income = Color(0xFFA6E3A1), expense = Color(0xFFCDD6F4), transfer = Color(0xFF9399B2),
    warning = Color(0xFFF9E2AF), alert = Color(0xFFF38BA8), good = Color(0xFFA6E3A1),
    tip = Color(0xFF89B4FA), gold = Color(0xFF89B4FA),
    cardSurface = Color(0xFF262637), cardSurfaceHigh = Color(0xFF313244), glass = Color(0xFF262637),
    glassBorder = Color(0xFF45475A), subtleText = Color(0xFFBAC2DE), divider = Color(0xFF45475A),
    shimmerBase = Color(0xFF262637), shimmerHighlight = Color(0xFF313244), isDark = true,
    ink = Color(0xFFCDD6F4), onInk = Color(0xFF1E1E2E), tertiaryText = Color(0xFF9399B2),
    hairline = Color(0xFF45475A), accent = Color(0xFF89B4FA), accentTint = Color(0xFF27364F),
    alertTint = Color(0xFF3B2530), warningTint = Color(0xFF3A3327), tipTint = Color(0xFF27364F),
    fill = Color(0xFF313244),
) else SumvaloColors(
    income = Color(0xFF40A02B), expense = Color(0xFF4C4F69), transfer = Color(0xFF6C6F85),
    warning = Color(0xFFDF8E1D), alert = Color(0xFFD20F39), good = Color(0xFF40A02B),
    tip = Color(0xFF1E66F5), gold = Color(0xFF1E66F5),
    cardSurface = Color.White, cardSurfaceHigh = Color(0xFFE6E9EF), glass = Color.White,
    glassBorder = Color(0xFFCCD0DA), subtleText = Color(0xFF6C6F85), divider = Color(0xFFCCD0DA),
    shimmerBase = Color(0xFFE6E9EF), shimmerHighlight = Color(0xFFEFF1F5), isDark = false,
    ink = Color(0xFF4C4F69), onInk = Color.White, tertiaryText = Color(0xFF8C8FA1),
    hairline = Color(0xFFCCD0DA), accent = Color(0xFF1E66F5), accentTint = Color(0xFFE5EEFC),
    alertTint = Color(0xFFFCE4E9), warningTint = Color(0xFFFBF0DC), tipTint = Color(0xFFE5EEFC),
    fill = Color(0xFFE6E9EF),
)

// ── Cloud / Amber: warm, muted neutrals with an amber accent ───────────────────────────────────
private fun cloudTokens(dark: Boolean) = if (dark) SumvaloColors(
    income = Color(0xFF8FD694), expense = Color(0xFFE8E0D4), transfer = Color(0xFFA99F8F),
    warning = Color(0xFFFBBF24), alert = Color(0xFFF87171), good = Color(0xFF8FD694),
    tip = Color(0xFFFBBF24), gold = Color(0xFFFBBF24),
    cardSurface = Color(0xFF241E18), cardSurfaceHigh = Color(0xFF2F2820), glass = Color(0xFF241E18),
    glassBorder = Color(0xFF453A2D), subtleText = Color(0xFFC9BFAF), divider = Color(0xFF453A2D),
    shimmerBase = Color(0xFF241E18), shimmerHighlight = Color(0xFF2F2820), isDark = true,
    ink = Color(0xFFE8E0D4), onInk = Color(0xFF171310), tertiaryText = Color(0xFFA99F8F),
    hairline = Color(0xFF453A2D), accent = Color(0xFFFBBF24), accentTint = Color(0xFF3D341F),
    alertTint = Color(0xFF3B2530), warningTint = Color(0xFF3A3327), tipTint = Color(0xFF3D341F),
    fill = Color(0xFF2F2820),
) else SumvaloColors(
    income = Color(0xFF2F9E44), expense = Color(0xFF292524), transfer = Color(0xFF78716C),
    warning = Color(0xFFB45309), alert = Color(0xFFDC2626), good = Color(0xFF2F9E44),
    tip = Color(0xFFB45309), gold = Color(0xFFB45309),
    cardSurface = Color.White, cardSurfaceHigh = Color(0xFFEFEAE3), glass = Color.White,
    glassBorder = Color(0xFFE2DACC), subtleText = Color(0xFF78716C), divider = Color(0xFFE2DACC),
    shimmerBase = Color(0xFFEFEAE3), shimmerHighlight = Color(0xFFF7F5F1), isDark = false,
    ink = Color(0xFF292524), onInk = Color.White, tertiaryText = Color(0xFF8A847B),
    hairline = Color(0xFFE2DACC), accent = Color(0xFFB45309), accentTint = Color(0xFFFBF0DC),
    alertTint = Color(0xFFFEE2E2), warningTint = Color(0xFFFBF0DC), tipTint = Color(0xFFFBF0DC),
    fill = Color(0xFFEFEAE3),
)

// ── Ocean: deep teal-blues ──────────────────────────────────────────────────────────────────────
private fun oceanTokens(dark: Boolean) = if (dark) SumvaloColors(
    income = Color(0xFF7DD3FC), expense = Color(0xFFE0F2FE), transfer = Color(0xFF94A9C4),
    warning = Color(0xFFFBBF24), alert = Color(0xFFF87171), good = Color(0xFF6EE7B7),
    tip = Color(0xFF22D3EE), gold = Color(0xFF22D3EE),
    cardSurface = Color(0xFF10243D), cardSurfaceHigh = Color(0xFF16304E), glass = Color(0xFF10243D),
    glassBorder = Color(0xFF24466B), subtleText = Color(0xFFB7C9DE), divider = Color(0xFF24466B),
    shimmerBase = Color(0xFF10243D), shimmerHighlight = Color(0xFF16304E), isDark = true,
    ink = Color(0xFFE0F2FE), onInk = Color(0xFF0A1628), tertiaryText = Color(0xFF94A9C4),
    hairline = Color(0xFF24466B), accent = Color(0xFF22D3EE), accentTint = Color(0xFF0F3242),
    alertTint = Color(0xFF3B2530), warningTint = Color(0xFF3A3327), tipTint = Color(0xFF0F3242),
    fill = Color(0xFF16304E),
) else SumvaloColors(
    income = Color(0xFF0E9F6E), expense = Color(0xFF0F1728), transfer = Color(0xFF5B7A99),
    warning = Color(0xFFB45309), alert = Color(0xFFDC2626), good = Color(0xFF0E9F6E),
    tip = Color(0xFF0E7490), gold = Color(0xFF0E7490),
    cardSurface = Color.White, cardSurfaceHigh = Color(0xFFE3EEF5), glass = Color.White,
    glassBorder = Color(0xFFD3E2EC), subtleText = Color(0xFF4E6B87), divider = Color(0xFFD3E2EC),
    shimmerBase = Color(0xFFE3EEF5), shimmerHighlight = Color(0xFFF0F7FA), isDark = false,
    ink = Color(0xFF0F1728), onInk = Color.White, tertiaryText = Color(0xFF6B87A3),
    hairline = Color(0xFFD3E2EC), accent = Color(0xFF0E7490), accentTint = Color(0xFFDCF3F8),
    alertTint = Color(0xFFFEE2E2), warningTint = Color(0xFFFBF0DC), tipTint = Color(0xFFDCF3F8),
    fill = Color(0xFFE3EEF5),
)

// ── Forest: deep greens ─────────────────────────────────────────────────────────────────────────
private fun forestTokens(dark: Boolean) = if (dark) SumvaloColors(
    income = Color(0xFF86EFAC), expense = Color(0xFFE5F3EA), transfer = Color(0xFF93AD9E),
    warning = Color(0xFFFBBF24), alert = Color(0xFFF87171), good = Color(0xFF86EFAC),
    tip = Color(0xFF4ADE80), gold = Color(0xFF4ADE80),
    cardSurface = Color(0xFF14241B), cardSurfaceHigh = Color(0xFF1C3225), glass = Color(0xFF14241B),
    glassBorder = Color(0xFF2A4736), subtleText = Color(0xFFB4CCBF), divider = Color(0xFF2A4736),
    shimmerBase = Color(0xFF14241B), shimmerHighlight = Color(0xFF1C3225), isDark = true,
    ink = Color(0xFFE5F3EA), onInk = Color(0xFF0C1510), tertiaryText = Color(0xFF93AD9E),
    hairline = Color(0xFF2A4736), accent = Color(0xFF4ADE80), accentTint = Color(0xFF153827),
    alertTint = Color(0xFF3B2530), warningTint = Color(0xFF3A3327), tipTint = Color(0xFF153827),
    fill = Color(0xFF1C3225),
) else SumvaloColors(
    income = Color(0xFF15803D), expense = Color(0xFF14201A), transfer = Color(0xFF68826F),
    warning = Color(0xFFB45309), alert = Color(0xFFDC2626), good = Color(0xFF15803D),
    tip = Color(0xFF15803D), gold = Color(0xFF15803D),
    cardSurface = Color.White, cardSurfaceHigh = Color(0xFFE6F0E8), glass = Color.White,
    glassBorder = Color(0xFFD5E4D9), subtleText = Color(0xFF5A7361), divider = Color(0xFFD5E4D9),
    shimmerBase = Color(0xFFE6F0E8), shimmerHighlight = Color(0xFFF2F7F2), isDark = false,
    ink = Color(0xFF14201A), onInk = Color.White, tertiaryText = Color(0xFF74907E),
    hairline = Color(0xFFD5E4D9), accent = Color(0xFF15803D), accentTint = Color(0xFFDDF3E3),
    alertTint = Color(0xFFFEE2E2), warningTint = Color(0xFFFBF0DC), tipTint = Color(0xFFDDF3E3),
    fill = Color(0xFFE6F0E8),
)

// ── Rose: dusty rose ────────────────────────────────────────────────────────────────────────────
private fun roseTokens(dark: Boolean) = if (dark) SumvaloColors(
    income = Color(0xFF9BE3C1), expense = Color(0xFFF6E4EA), transfer = Color(0xFFB39AA6),
    warning = Color(0xFFFBBF24), alert = Color(0xFFF87171), good = Color(0xFF9BE3C1),
    tip = Color(0xFFFB7185), gold = Color(0xFFFB7185),
    cardSurface = Color(0xFF241920), cardSurfaceHigh = Color(0xFF31222B), glass = Color(0xFF241920),
    glassBorder = Color(0xFF48303C), subtleText = Color(0xFFD8BFC9), divider = Color(0xFF48303C),
    shimmerBase = Color(0xFF241920), shimmerHighlight = Color(0xFF31222B), isDark = true,
    ink = Color(0xFFF6E4EA), onInk = Color(0xFF171015), tertiaryText = Color(0xFFB39AA6),
    hairline = Color(0xFF48303C), accent = Color(0xFFFB7185), accentTint = Color(0xFF3C2430),
    alertTint = Color(0xFF3B2530), warningTint = Color(0xFF3A3327), tipTint = Color(0xFF3C2430),
    fill = Color(0xFF31222B),
) else SumvaloColors(
    income = Color(0xFF0F8A5F), expense = Color(0xFF221619), transfer = Color(0xFF967982),
    warning = Color(0xFFB45309), alert = Color(0xFFDC2626), good = Color(0xFF0F8A5F),
    tip = Color(0xFFE11D48), gold = Color(0xFFE11D48),
    cardSurface = Color.White, cardSurfaceHigh = Color(0xFFF5E8EC), glass = Color.White,
    glassBorder = Color(0xFFEBD9DF), subtleText = Color(0xFF836873), divider = Color(0xFFEBD9DF),
    shimmerBase = Color(0xFFF5E8EC), shimmerHighlight = Color(0xFFFAF3F5), isDark = false,
    ink = Color(0xFF221619), onInk = Color.White, tertiaryText = Color(0xFF937A85),
    hairline = Color(0xFFEBD9DF), accent = Color(0xFFE11D48), accentTint = Color(0xFFFCE0E7),
    alertTint = Color(0xFFFEE2E2), warningTint = Color(0xFFFBF0DC), tipTint = Color(0xFFFCE0E7),
    fill = Color(0xFFF5E8EC),
)

private fun catppuccinBackground(dark: Boolean) = if (dark) Color(0xFF1E1E2E) else Color(0xFFEFF1F5)
private fun cloudBackground(dark: Boolean) = if (dark) Color(0xFF171310) else Color(0xFFF7F5F1)
private fun oceanBackground(dark: Boolean) = if (dark) Color(0xFF0A1628) else Color(0xFFF0F7FA)
private fun forestBackground(dark: Boolean) = if (dark) Color(0xFF0C1510) else Color(0xFFF2F7F2)
private fun roseBackground(dark: Boolean) = if (dark) Color(0xFF171015) else Color(0xFFFAF3F5)

/**
 * Resolve a palette's tokens and Material scheme for the given mode. Handles DEFAULT too;
 * DYNAMIC is wallpaper-derived at runtime, so callers get the Default palette as fallback here.
 */
fun builtInPalette(theme: ColorTheme, dark: Boolean): ResolvedPalette {
    val tokens = when (theme) {
        ColorTheme.CATPPUCCIN -> catppuccinTokens(dark)
        ColorTheme.CLOUD -> cloudTokens(dark)
        ColorTheme.OCEAN -> oceanTokens(dark)
        ColorTheme.FOREST -> forestTokens(dark)
        ColorTheme.ROSE -> roseTokens(dark)
        else -> if (dark) DarkSumvaloColors else LightSumvaloColors
    }
    val background = when (theme) {
        ColorTheme.CATPPUCCIN -> catppuccinBackground(dark)
        ColorTheme.CLOUD -> cloudBackground(dark)
        ColorTheme.OCEAN -> oceanBackground(dark)
        ColorTheme.FOREST -> forestBackground(dark)
        ColorTheme.ROSE -> roseBackground(dark)
        else -> if (dark) BrandPalette.SumvaloNight else BrandPalette.SumvaloPaper
    }
    return ResolvedPalette(tokens, tokensToScheme(tokens, background), background)
}
