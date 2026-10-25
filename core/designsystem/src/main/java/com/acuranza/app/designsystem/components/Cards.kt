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
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.Paise

/**
 * The standard content card: white (or dark card) surface, 16dp corners and a 1dp hairline
 * outline. No shadow.
 */
@Composable
fun AcuranzaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AcuranzaTheme.spacing.lg),
    shape: Shape = RoundedCornerShape(AcuranzaTheme.spacing.cardRadius),
    color: Color = AcuranzaTheme.colors.cardSurface,
    content: @Composable ColumnScope.() -> Unit,
) {
    // An explicit non-default color is a semantic tint (warning/primary washes): keep it solid.
    val tinted = color != AcuranzaTheme.colors.cardSurface
    Column(
        modifier = modifier
            .glassSurface(shape, colorOverride = if (tinted) color else null)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * An emphasis card with a flat dark face and white content — the app's hero treatment. The face
 * follows the theme: the brand ink (deep navy) in Light mode, the elevated card surface in Dark
 * and AMOLED modes, so the card never reads as an off-brand black slab. Use sparingly: one per
 * screen at most. The [brush] parameter is kept for compatibility; pass a [SolidColor] to
 * override the face.
 */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: Brush = SolidColor(heroFace()),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(AcuranzaTheme.spacing.xl),
    shape: Shape = RoundedCornerShape(20.dp),
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
    if (AcuranzaTheme.colors.isDark) AcuranzaTheme.colors.cardSurface else AcuranzaTheme.colors.ink

/**
 * The glass surface treatment, centralized so every screen shares one implementation: a
 * translucent fill derived from the theme's glass token, a fine glass border, and — in dark
 * modes — a restrained top-edge highlight that suggests a lit glass edge. No real-time blur:
 * Compose has no public in-app backdrop blur, so this scrim-glass is the primary treatment and
 * the deliberate fallback everywhere (dialogs and sheets get true window blur separately on
 * API 31+). Cheap to render (one fill, one border, one line), safe on every API level, and
 * derived entirely from theme tokens so Dark, Light, System, AMOLED and every palette adapt
 * without hardcoding.
 */
@Composable
fun Modifier.glassSurface(shape: Shape, colorOverride: Color? = null): Modifier {
    val c = AcuranzaTheme.colors
    return this
        .clip(shape)
        .background(colorOverride ?: c.glass.copy(alpha = if (c.isDark) 0.80f else 0.92f))
        .border(1.dp, c.glassBorder, shape)
        .drawWithContent {
            drawContent()
            if (c.isDark) {
                // Restrained lit edge: a 1dp gradient at the top, white at 10% fading out.
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        startY = 0f,
                        endY = 6.dp.toPx(),
                    ),
                    size = Size(size.width, 6.dp.toPx()),
                )
            }
        }
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
    contentPadding: PaddingValues = PaddingValues(AcuranzaTheme.spacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(AcuranzaTheme.colors.cardSurface)
            .border(1.dp, AcuranzaTheme.colors.hairline, shape)
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
                color = AcuranzaTheme.colors.subtleText,
            )
            if (onToggleHidden != null) {
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                        tint = AcuranzaTheme.colors.tertiaryText,
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
                color = AcuranzaTheme.colors.ink,
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
                    color = AcuranzaTheme.colors.accent,
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
        good -> AcuranzaTheme.colors.income
        else -> AcuranzaTheme.colors.alert
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
                color = if (onDark) Color.White.copy(alpha = 0.7f) else AcuranzaTheme.colors.subtleText,
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
