package com.acuranza.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Paise

/**
 * The standard content card: solid card surface, 8dp corners and a 1dp hairline outline.
 * No shadow, no translucency, no glass edge — flat and legible on every theme.
 */
@Composable
fun SumvaloCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(SumvaloTheme.spacing.md),
    shape: Shape = RoundedCornerShape(SumvaloTheme.spacing.cardRadius),
    color: Color = SumvaloTheme.colors.cardSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.dp, SumvaloTheme.colors.hairline, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * An emphasis card with a flat dark face and white content — the app's hero treatment. The face
 * follows the theme: the brand ink (deep navy) in Light mode, the elevated card surface in Dark
 * and AMOLED modes, so the card never reads as an off-brand black slab. Use sparingly: one per
 * screen at most. Solid color, no gradient.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = SolidColor(heroFace()),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(SumvaloTheme.spacing.md),
    shape: Shape = RoundedCornerShape(SumvaloTheme.spacing.cardRadius),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** The flat ink used for emphasis cards and card faces. */
val InkFace = Color(0xFF111111)

/** The theme-aware emphasis face: brand ink on light, the elevated card surface on dark/AMOLED. */
@Composable
private fun heroFace(): Color =
    if (SumvaloTheme.colors.isDark) SumvaloTheme.colors.cardSurface else SumvaloTheme.colors.ink

/**
 * The standard surface treatment, centralized so every screen shares one implementation: a
 * solid fill derived from the theme's card surface token and a fine hairline border. No
 * translucency, no gradient, no real-time blur — cheap to render, safe on every API level,
 * derived entirely from theme tokens so Dark, Light, System, AMOLED and every palette adapt
 * without hardcoding.
 */
@Composable
fun Modifier.glassSurface(shape: Shape, colorOverride: Color? = null): Modifier {
    val c = SumvaloTheme.colors
    return this
        .clip(shape)
        .background(colorOverride ?: c.cardSurface)
        .border(1.dp, c.hairline, shape)
}

/** True window-level blur behind dialogs and bottom sheets on Android 12+, no-op below. */
fun android.view.View.dialogBlurBehind(radiusPx: Int = 80) {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
    var w: Any? = parent
    while (w is android.view.View) {
        if (w is androidx.compose.ui.window.DialogWindowProvider) {
            w.window.setBackgroundBlurRadius(radiusPx)
            return
        }
        w = w.parent
    }
}

/**
 * Call INSIDE a dialog/sheet's content: blurs the window behind it on Android 12+ (GPU-composited,
 * one call — no per-frame cost). No-op below API 31, where the scrim glass is the treatment.
 */
@Composable
fun DialogBlurBehind(radiusPx: Int = 80) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect { view.dialogBlurBehind(radiusPx) }
}

/** A plain outlined surface (the old frosted glass, now flat). */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    contentPadding: PaddingValues = PaddingValues(SumvaloTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * The dashboard headline: a small label, a large amount and an optional change line.
 * Deliberately not a card: it sits directly on the page.
 *
 * @param deltaPaise change over the comparison period; `null` hides it.
 * @param trend recent values for a small sparkline (oldest first); fewer than two hides it.
 * @param onToggleHidden shows an eye button that toggles privacy mode.
 */
@Composable
fun BalanceHeroCard(
    label: String,
    amount: Paise,
    modifier: Modifier = Modifier,
    deltaPaise: Paise? = null,
    deltaLabel: String = "this month",
    trend: List<Float> = emptyList(),
    hidden: Boolean = false,
    onToggleHidden: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = SumvaloTheme.colors.subtleText,
            )
            if (onToggleHidden != null) {
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                        tint = SumvaloTheme.colors.tertiaryText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            AmountText(
                paise = amount,
                style = AmountStyles.hero,
                color = SumvaloTheme.colors.ink,
                hidden = hidden,
                animate = true,
                modifier = Modifier.weight(1f),
            )
            if (trend.size >= 2) {
                Sparkline(
                    values = trend,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(width = 72.dp, height = 28.dp),
                    color = SumvaloTheme.colors.accent,
                    fill = false,
                )
            }
        }
        if (deltaPaise != null) {
            Spacer(Modifier.height(6.dp))
            DeltaChip(deltaPaise = deltaPaise, label = deltaLabel, hidden = hidden)
        }
        if (footer != null) {
            Spacer(Modifier.height(16.dp))
            footer()
        }
    }
}

/**
 * A change shown as text: "+₹12.5K this month". Positive is emerald, negative is ink
 * (or red when [positiveIsGood] is false and the value rises).
 * @param onDark render on an ink surface (white text).
 */
@Composable
fun DeltaChip(
    deltaPaise: Paise,
    modifier: Modifier = Modifier,
    label: String? = null,
    hidden: Boolean = false,
    onDark: Boolean = false,
    positiveIsGood: Boolean = true,
) {
    val up = deltaPaise >= 0
    val good = up == positiveIsGood
    val tint = when {
        onDark -> Color.White
        good -> SumvaloTheme.colors.income
        else -> SumvaloTheme.colors.alert
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (hidden) "••••" else (if (up) "+" else "−") + formatAmount(kotlin.math.abs(deltaPaise), compact = true),
            style = AmountStyles.small,
            color = tint,
        )
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (onDark) Color.White.copy(alpha = 0.7f) else SumvaloTheme.colors.subtleText,
            )
        }
    }
}

/** A tinted rounded-square tile behind an icon (10% of [color]). */
@Composable
fun IconBubble(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    alpha: Float = 0.10f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center,
    ) { content() }
}
