package com.acuranza.app.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.acuranza.app.designsystem.theme.SumvaloText
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.acuranza.app.designsystem.theme.LocalHapticsEnabled
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.acuranza.app.designsystem.theme.AmountStyles
import com.acuranza.app.designsystem.theme.Motion
import com.acuranza.app.designsystem.theme.pressSpring
import com.acuranza.app.designsystem.theme.springForCard
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.designsystem.theme.TabularFigures
import androidx.compose.animation.core.EaseOut
import com.acuranza.app.model.Paise
import com.acuranza.app.model.TxnType

/**
 * A pill-shaped segmented control with a sliding indicator, e.g. Month / Quarter / Year.
 */
/** Subtle tap tick for design-system controls: the shell-provided shared haptic path, so the
 *  selected strength and the tabs-and-buttons preference apply here too. No-op in previews. */
@Composable
internal fun rememberTick(): () -> Unit = com.acuranza.app.designsystem.theme.LocalHapticTick.current

@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberTick()
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(SumvaloTheme.colors.fill)
            .padding(3.dp),
    ) {
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val reduced = com.acuranza.app.designsystem.theme.LocalReducedMotion.current
        val offset by animateDpAsState(
            targetValue = segment * selectedIndex,
            animationSpec = if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(shape)
                .background(SumvaloTheme.colors.cardSurface)
                .border(1.dp, SumvaloTheme.colors.hairline, shape),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable { tick(); onSelect(i) }
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) SumvaloTheme.colors.ink else SumvaloTheme.colors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Compact text chips for time ranges (1D / 1W / 1M / 1Y / ALL). The active chip is a solid ink
 * pill with contrasting text; inactive ones are quiet text, like the reference chart's range row.
 */
@Composable
fun RangeChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberTick()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .heightIn(min = 32.dp)
                    .clip(shape)
                    .then(if (active) Modifier.background(SumvaloTheme.colors.ink) else Modifier)
                    .clickable { tick(); onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .semantics {
                        role = Role.Tab
                        this.selected = active
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) SumvaloTheme.colors.onInk else SumvaloTheme.colors.subtleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** ‹ Oct 2026 › — steps through periods. Disable "next" at the current period. */
@Composable
fun MonthSwitcher(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canGoNext: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(SumvaloTheme.colors.cardSurfaceHigh),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous period")
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(min = 84.dp),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext, enabled = canGoNext, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next period",
                modifier = Modifier.alpha(if (canGoNext) 1f else 0.3f),
            )
        }
    }
}

/** A horizontally scrolling row of single- or multi-select filter chips. */
@Composable
fun FilterChipsRow(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(option) },
                label = { Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = SumvaloTheme.colors.cardSurfaceHigh,
                ),
                border = null,
            )
        }
    }
}

/** An empty-state block: a large tinted icon, a title, a message and an optional action. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(SumvaloTheme.colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SumvaloTheme.colors.subtleText, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = SumvaloTheme.colors.subtleText,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.fillMaxWidth(0.7f))
        }
    }
}

/** A shimmering placeholder block for loading states. */
@Composable
fun LoadingShimmer(
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
) {
    val reduced = com.acuranza.app.designsystem.theme.LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    // Reduced motion: a frozen placeholder instead of an endless sweep.
    val x by transition.animateFloat(
        initialValue = if (reduced) 2f else -1f,
        targetValue = 2f,
        animationSpec = if (reduced) infiniteRepeatable(tween(0), RepeatMode.Restart)
        else infiniteRepeatable(tween(900, easing = EaseOut), RepeatMode.Restart),
        label = "shimmer-x",
    )
    val base = SumvaloTheme.colors.shimmerBase
    val hi = SumvaloTheme.colors.shimmerHighlight
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .semantics { contentDescription = "Loading" },
    ) {
        val w = constraints.maxWidth.toFloat()
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.linearGradient(
                        listOf(base, hi, base),
                        start = Offset(w * x, 0f),
                        end = Offset(w * x + w * 0.6f, 0f),
                    ),
                ),
        )
    }
}

/**
 * A minimal top bar: optional back arrow, title with optional subtitle, and actions.
 * Transparent so it sits on the screen background.
 */
@Composable
fun SumvaloTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SumvaloTheme.colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

/**
 * A compact stat: label, amount, optional change. For rows of "Income · Spent · Saved".
 */
@Composable
fun StatChip(
    label: String,
    value: Paise,
    modifier: Modifier = Modifier,
    type: TxnType? = null,
    deltaPaise: Paise? = null,
    positiveIsGood: Boolean = true,
    hidden: Boolean = false,
    onDark: Boolean = false,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (onDark) Color.White.copy(alpha = 0.7f) else SumvaloTheme.colors.subtleText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        AmountText(
            paise = value,
            style = AmountStyles.medium,
            compact = true,
            hidden = hidden,
            color = if (onDark) Color.White else when (type) {
                TxnType.INCOME -> SumvaloTheme.colors.income
                TxnType.EXPENSE -> SumvaloTheme.colors.ink
                else -> SumvaloTheme.colors.ink
            },
        )
        if (deltaPaise != null) {
            Spacer(Modifier.height(6.dp))
            DeltaChip(deltaPaise = deltaPaise, hidden = hidden, onDark = onDark, positiveIsGood = positiveIsGood)
        }
    }
}

/** A section title with an optional trailing "See all" style action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val tick = rememberTick()
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 2.dp).heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = SumvaloText.sectionLabel,
            color = SumvaloTheme.colors.subtleText,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = SumvaloTheme.colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { tick(); onAction() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Tactile press feedback: scales to [scaleDown] while pressed and eases back. Share the
 * [interactionSource] with the clickable so the ripple and the scale fire together. A quick
 * 150ms tween keeps the response inside the high-frequency budget: no bounce, no overshoot,
 * and disabled controls (whose clickables never press) never scale.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, scaleDown: Float = 0.96f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        // Expressive press: a fast spring that settles back with a hint of life instead of a
        // linear tween. Under reduced motion it snaps — instant feedback, no movement.
        animationSpec = if (com.acuranza.app.designsystem.theme.LocalReducedMotion.current) {
            androidx.compose.animation.core.snap()
        } else {
            pressSpring()
        },
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * The main call-to-action button: solid ink with white text, 14dp corners, 52dp tall.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .then(if (enabled) Modifier.pressScale(interaction) else Modifier)
            .background(if (enabled) SumvaloTheme.colors.ink else SumvaloTheme.colors.fill)
            .clickable(interactionSource = interaction, indication = indication, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SumvaloTheme.colors.onInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) SumvaloTheme.colors.onInk else SumvaloTheme.colors.tertiaryText,
        )
    }
}


/** A square quick-action tile: icon in a filled rounded square with a label below. */
@Composable
fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberTick()
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = indication, role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SumvaloTheme.colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SumvaloTheme.colors.subtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A compact action button for use on an ink card: [filled] renders a white face with ink text,
 * otherwise a white-outlined ghost with white text.
 */
@Composable
fun HeroActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    val interaction = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .pressScale(interaction)
            .then(
                if (filled) Modifier.background(Color.White)
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), shape),
            )
            .clickable(interactionSource = interaction, indication = indication, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (filled) Color(0xFF111111) else Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (filled) Color(0xFF111111) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A secondary button: white with a hairline border and ink text. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) SumvaloTheme.colors.ink else SumvaloTheme.colors.tertiaryText,
        )
    }
}
