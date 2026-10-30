package com.acuranza.app.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Paise

/** The standard content card: solid card surface, 14dp corners and a 1dp hairline outline.
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
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier
            .then(clickableModifier)
            .clip(shape)
            .background(color)
            .border(1.dp, SumvaloTheme.colors.hairline, shape)
            .padding(contentPadding),
        content = content,
    )
}

/** Deprecated: tinted icon tiles were removed in v3.1 de-slop. Use plain Icon instead. */
@Deprecated("Use plain Icon instead — tinted bubble containers are AI-generated slop")
@Composable
fun IconBubble(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    alpha: Float = 0.10f,
    content: @Composable () -> Unit,
) {
    content()
}

/** Deprecated: frosted-glass surface removed in v3.1 de-slop. Use SumvaloCard instead. */
@Deprecated("Use SumvaloCard instead — glass surfaces are AI-generated slop")
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

/** A change shown as text: "+₹12.5K this month". Positive is emerald, negative is red. */
@Composable
fun DeltaChip(
    deltaPaise: Paise,
    modifier: Modifier = Modifier,
    label: String? = null,
    hidden: Boolean = false,
) {
    val up = deltaPaise >= 0
    val tint = if (up) SumvaloTheme.colors.income else SumvaloTheme.colors.alert
    val abs = kotlin.math.abs(deltaPaise)
    val formatted = "${abs / 100}.${String.format("%02d", abs % 100)}"
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (hidden) "••••" else (if (up) "+" else "−") + formatted,
            style = AmountStyles.small,
            color = tint,
        )
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
        }
    }
}

/** A primary action button: flat text button with a leading icon. No fill, no shadow —
 * the action is communicated by the brand-coloured label and icon, not by a container.
 */
@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
) {
    val textColor = if (enabled) SumvaloTheme.colors.accent else SumvaloTheme.colors.subtleText
    Row(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

/** The dashboard headline: a label, large amount and optional change line. Simple, flat, no sparkline —
 * sparklines are decorative and add no decision-relevant information.
 */
@Composable
fun BalanceHeroCard(
    label: String,
    amount: Paise,
    modifier: Modifier = Modifier,
    deltaPaise: Paise? = null,
    deltaLabel: String? = null,
    hidden: Boolean = false,
    onToggleHidden: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (onToggleHidden != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SumvaloTheme.colors.subtleText,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleHidden, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Show amounts" else "Hide amounts",
                        tint = SumvaloTheme.colors.subtleText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = SumvaloTheme.colors.subtleText,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            AmountText(
                paise = amount,
                style = AmountStyles.hero,
                color = SumvaloTheme.colors.ink,
                hidden = hidden,
                modifier = Modifier.weight(1f),
            )
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

/** Deprecated: Modifier.glassSurface() was removed in v3.1 de-slop. Use .padding() + .border() instead. */
@Deprecated("Use SumvaloCard instead")
@Composable
fun Modifier.glassSurface(shape: Shape, colorOverride: Color? = null): Modifier {
    val c = SumvaloTheme.colors
    return this
        .clip(shape)
        .background(colorOverride ?: c.cardSurface)
        .border(1.dp, c.hairline, shape)
}

/** Deprecated: GradientCard removed in v3.1 de-slop. Flat card with no gradient. */
@Deprecated("Use SumvaloCard instead — gradient cards are AI-generated slop")
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    brush: androidx.compose.ui.graphics.Brush = androidx.compose.ui.graphics.SolidColor(SumvaloTheme.colors.cardSurface),
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(SumvaloTheme.spacing.md),
    shape: Shape = RoundedCornerShape(SumvaloTheme.spacing.cardRadius),
    content: @Composable ColumnScope.() -> Unit,
) {
    SumvaloCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = contentPadding,
        shape = shape,
        color = SumvaloTheme.colors.cardSurface,
        content = content,
    )
}

/** Deprecated: DialogBlurBehind removed in v3.1 de-slop. No-op. */
@Deprecated("No longer used — dialogs use scrim, not blur")
@Composable
fun DialogBlurBehind(radiusPx: Int = 80) {
    // No-op: removed in v3.1 de-slop
}
