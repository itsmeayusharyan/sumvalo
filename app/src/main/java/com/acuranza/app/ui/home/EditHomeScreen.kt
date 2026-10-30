package com.acuranza.app.ui.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * Settings → Appearance → Edit home: picks which dashboard sections appear and in what order.
 *
 * The editor holds a WORKING COPY: every change applies immediately to the editor (rows move
 * between the two groups as you add or remove them) but nothing is written to DataStore until
 * Save. Cancel — or back — with unsaved changes asks first, so nothing is lost to a stray tap.
 * Reset restores the stock dashboard and saves that immediately.
 */
@Composable
fun EditHomeScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)

    // Seeded exactly once, when the persisted settings first arrive; after that the working
    // copy is the source of truth, so flow emissions never stomp in-progress edits.
    var layout by remember { mutableStateOf<List<Pair<HomeSection, Boolean>>?>(null) }
    LaunchedEffect(settings) {
        if (layout == null && settings != null) {
            layout = HomeLayout.decode(settings!!.homeLayout)
        }
    }
    val savedLayout = settings?.homeLayout
    val working = layout ?: HomeLayout.decode(null)
    val dirty = savedLayout != null && HomeLayout.encode(working) != savedLayout
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    fun save() {
        scope.launch { c.settings.setHomeLayout(HomeLayout.encode(working)) }
    }
    val attemptLeave: () -> Unit = {
        if (dirty) confirmDiscard = true else nav.popBackSafely()
    }

    SumvaloScreen(
        title = "Edit home",
        subtitle = "Reorder, add and remove dashboard sections",
        onBack = attemptLeave,
        actions = {
            TextButton(onClick = attemptLeave) { Text("Cancel") }
            TextButton(onClick = { save(); nav.popBackSafely() }, enabled = dirty) { Text("Save") }
        },
    ) { padding ->
        EditHomeContent(
            layout = working,
            modifier = Modifier.padding(padding),
            onChange = { layout = it },
            onResetClick = { confirmReset = true },
        )
    }

    if (confirmDiscard) {
        ConfirmDialog(
            "Discard changes?",
            "Your section order and visibility changes haven't been saved.",
            "Discard",
            destructive = true,
            onConfirm = {
                confirmDiscard = false
                nav.popBackSafely()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            "Reset to the default dashboard?",
            "Your order and visibility choices are replaced by the stock layout. Your data is untouched.",
            "Reset",
            destructive = true,
            onConfirm = {
                confirmReset = false
                layout = HomeLayout.decode(null)
                scope.launch { c.settings.resetHomeLayout() }
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/**
 * Stateless editor body, so tests render it without the container: enabled sections first (drag
 * the handle, or the arrow buttons, to reorder; ✕ removes), then the available ones (+ adds).
 * Rows animate between the groups the moment they change. No per-section settings exist yet,
 * so there is deliberately no "customize" menu — no nonfunctional controls.
 */
@Composable
fun EditHomeContent(
    layout: List<Pair<HomeSection, Boolean>>,
    modifier: Modifier = Modifier,
    onChange: (List<Pair<HomeSection, Boolean>>) -> Unit = {},
    onResetClick: () -> Unit = {},
) {
    val visible = layout.filter { it.second }
    val hidden = layout.filter { !it.second }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 64.dp.toPx() }
    var dragging by remember { mutableStateOf<HomeSection?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "enabled-label") {
            SectionLabel("ON YOUR HOMEPAGE", top = 8.dp)
        }
        if (visible.isEmpty()) {
            item(key = "enabled-empty") {
                Text(
                    "Nothing here yet — add sections below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        items(visible.size, key = { "e-" + visible[it].first.id }) { index ->
            val (section, _) = visible[index]
            val isDragging = dragging == section
            val offsetY by animateDpAsState(
                if (isDragging) with(density) { dragOffset.toDp() } else 0.dp,
                label = "drag-offset",
            )
            SumvaloCard(
                Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .offset { IntOffset(0, offsetY.roundToPx()) }
                    .pointerInput(section) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragging = section; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                // Crossing a neighbour's midline swaps the two visible rows.
                                val steps = (dragOffset / rowHeightPx).toInt()
                                if (steps != 0) {
                                    val target = (index + steps).coerceIn(0, visible.lastIndex)
                                    if (target != index) {
                                        val next = HomeLayout.moveVisibleTo(layout, index, target)
                                        if (next != layout) {
                                            onChange(next)
                                            dragOffset -= steps * rowHeightPx
                                        }
                                    }
                                }
                            },
                            onDragEnd = { dragging = null; dragOffset = 0f },
                            onDragCancel = { dragging = null; dragOffset = 0f },
                        )
                    },
            ) {
                EditHomeRow(
                    section = section,
                    visible = true,
                    onMove = { up ->
                        val target = if (up) index - 1 else index + 1
                        onChange(HomeLayout.moveVisibleTo(layout, index, target))
                    },
                    onUpEnabled = index > 0,
                    onDownEnabled = index < visible.lastIndex,
                    onToggle = { onChange(HomeLayout.setVisibility(layout, section, false)) },
                )
            }
        }
        item(key = "available-label") {
            SectionLabel("AVAILABLE SECTIONS", top = 14.dp)
        }
        if (hidden.isEmpty()) {
            item(key = "available-empty") {
                Text(
                    "Everything is on your homepage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SumvaloTheme.colors.subtleText,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        items(hidden.size, key = { "h-" + hidden[it].first.id }) { index ->
            val (section, _) = hidden[index]
            SumvaloCard(
                Modifier
                    .fillMaxWidth()
                    .animateItem(),
            ) {
                EditHomeRow(
                    section = section,
                    visible = false,
                    onMove = {},
                    onUpEnabled = false,
                    onDownEnabled = false,
                    onToggle = { onChange(HomeLayout.setVisibility(layout, section, true)) },
                )
            }
        }
        item(key = "reset") {
            SumvaloCard {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Reset to default", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Restores the stock dashboard — your data is untouched",
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onResetClick) { Text("Reset") }
                }
            }
        }
        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SectionLabel(text: String, top: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        style = SumvaloText.sectionLabel,
        color = SumvaloTheme.colors.subtleText,
        modifier = Modifier.padding(start = 4.dp, top = top, bottom = 2.dp),
    )
}

@Composable
private fun EditHomeRow(
    section: HomeSection,
    visible: Boolean,
    onMove: (Boolean) -> Unit,
    onUpEnabled: Boolean,
    onDownEnabled: Boolean,
    onToggle: () -> Unit,
) {
    val colors = SumvaloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (visible) {
            // The drag affordance; the arrow buttons remain the accessible alternative.
            Icon(
                Icons.Outlined.DragIndicator,
                contentDescription = "Drag to reorder ${section.label}",
                tint = colors.subtleText,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Spacer(Modifier.width(24.dp))
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            section.icon,
            contentDescription = null,
            tint = if (visible) colors.ink else colors.tertiaryText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                section.label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (visible) MaterialTheme.colorScheme.onSurface else colors.subtleText,
            )
            Text(
                section.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (visible) {
            IconButton(enabled = onUpEnabled, onClick = { onMove(true) }) {
                Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move ${section.label} up")
            }
            IconButton(enabled = onDownEnabled, onClick = { onMove(false) }) {
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move ${section.label} down")
            }
            IconButton(onClick = onToggle) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove ${section.label} from the homepage", tint = colors.subtleText)
            }
        } else {
            IconButton(onClick = onToggle) {
                Icon(Icons.Outlined.Add, contentDescription = "Add ${section.label} to the homepage", tint = colors.accent)
            }
        }
    }
}
