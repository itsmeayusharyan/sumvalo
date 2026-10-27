package com.acuranza.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.IconBubble
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme

/**
 * The settings row kit: labelled groups of rows on cards, each row led by an icon tile (the
 * same vocabulary as the home screen's quick actions) with a hairline between rows aligned
 * to the text. Only this screen family uses it; [Section] in MoreScreen.kt keeps the plainer
 * style the other inner screens are built on.
 */

/** How a row's leading icon tile is tinted. */
enum class RowTone {
    /** The standard neutral tile. */
    NORMAL,

    /** A dimmed tile for rows that can't be used right now. */
    MUTED,

    /** The red tile for destructive rows. */
    DESTRUCTIVE,
}

/** Hairline between rows inside a settings card, aligned with the text (past the icon tile). */
@Composable
fun SettingsDivider(startInset: Dp = 52.dp) {
    Box(
        Modifier
            .padding(start = startInset)
            .fillMaxWidth()
            .height(1.dp)
            .background(SumvaloTheme.colors.divider),
    )
}

/** A titled group of settings rows on a card; rows bring their own padding and dividers. */
@Composable
fun SettingsSection(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        // On subpages the page title already names the group, so the in-card label is dropped.
        if (title != null) {
            Text(title.uppercase(), style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText)
            Spacer(Modifier.height(8.dp))
        }
        SumvaloCard(contentPadding = PaddingValues(0.dp)) {
            content()
        }
    }
}

@Composable
private fun RowTile(icon: ImageVector, tone: RowTone, enabled: Boolean) {
    val colors = SumvaloTheme.colors
    val (tile, tint) = when (tone) {
        RowTone.NORMAL -> colors.fill to colors.ink
        RowTone.MUTED -> colors.fill to colors.tertiaryText
        RowTone.DESTRUCTIVE -> colors.alertTint to colors.alert
    }
    IconBubble(color = tile, size = 36.dp, shape = RoundedCornerShape(12.dp), alpha = 1f) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) tint else tint.copy(alpha = 0.45f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSwitch(checked: Boolean, enabled: Boolean) {
    val colors = SumvaloTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = null, // the row itself is the tap target
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = colors.fill,
            uncheckedBorderColor = colors.hairline,
        ),
    )
}

@Composable
private fun RowSkeleton(
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    tone: RowTone,
    enabled: Boolean,
    divider: Boolean,
    titleColor: Color? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = SumvaloTheme.colors
    val tick = com.acuranza.app.ui.common.rememberHaptic()
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(enabled = enabled) { tick(); onClick() }
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                RowTile(icon, tone, enabled)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        titleColor != null -> titleColor
                        !enabled -> colors.subtleText
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.subtleText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                footer?.invoke(this)
            }
            trailing?.invoke(this)
        }
        if (divider) SettingsDivider(if (icon != null) 52.dp else 16.dp)
    }
}

/** A row that navigates or acts on tap: icon tile, title, description and a chevron. */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: RowTone = RowTone.NORMAL,
    enabled: Boolean = true,
    value: String? = null,
    divider: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    RowSkeleton(
        title = title,
        subtitle = subtitle,
        icon = icon,
        tone = tone,
        enabled = enabled,
        divider = divider,
        titleColor = if (tone == RowTone.DESTRUCTIVE) SumvaloTheme.colors.alert else null,
        footer = footer,
        onClick = onClick,
        trailing = {
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                if (value != null) {
                    // Capped so a long user-generated value can never starve the weighted title
                    // column beside it; ellipsizes gracefully and TalkBack still reads it all.
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumvaloTheme.colors.subtleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = SumvaloTheme.colors.tertiaryText.copy(alpha = if (enabled) 1f else 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}

/** A row with a switch; the whole row is the tap target. */
@Composable
fun SettingsToggle(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: ImageVector? = null,
    tone: RowTone = RowTone.NORMAL,
    enabled: Boolean = true,
    divider: Boolean = true,
) {
    RowSkeleton(
        title = title,
        subtitle = subtitle,
        icon = icon,
        tone = tone,
        enabled = enabled,
        divider = divider,
        trailing = { SettingsSwitch(checked, enabled) },
        onClick = if (enabled) ({ onChecked(!checked) }) else null,
    )
}

/** A row showing the current choice; tapping opens a bottom-sheet picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SettingsPickerRow(
    title: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    icon: ImageVector? = null,
    divider: Boolean = true,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    SettingsRow(
        title = title,
        icon = icon,
        enabled = enabled,
        value = optionLabel(selected),
        divider = divider,
        onClick = if (enabled) ({ open = true }) else null,
    )
    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyColumn(Modifier.navigationBarsPadding()) {
                items(options.size) { i ->
                    val o = options[i]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(o)
                                open = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(optionLabel(o), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        if (o == selected) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
