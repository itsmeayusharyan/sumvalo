package com.acuranza.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.NavTabs
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * Edit Navigation Tabs: the visual customizer for the bottom bar. The preview at the top is the
 * REAL bar composable, rendered from the working copy — it changes as you change things, and Save
 * writes exactly what the preview shows. Labels and icons are display-only (routes and
 * destinations never move); the landing-tab guard keeps a hidden tab from being the default
 * landing tab, moving the landing to the first visible tab. At least two tabs stay visible.
 */
@Composable
fun NavTabsEditorScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()

    var layout by remember { mutableStateOf(NavTabs.decodeLayout(s.navTabLayout)) }
    var labels by remember { mutableStateOf(NavTabs.decodeLabels(s.navTabLabels)) }
    var icons by remember { mutableStateOf(NavTabs.decodeIcons(s.navTabIcons)) }
    var showLabels by remember { mutableStateOf(s.navShowLabels) }
    var floating by remember { mutableStateOf(s.navFloatingBar) }

    val dirty = NavTabs.encodeLayout(layout) != (s.navTabLayout ?: NavTabs.encodeLayout(NavTabs.decodeLayout(null))) ||
        NavTabs.encodeLabels(labels) != (s.navTabLabels ?: "") ||
        NavTabs.encodeIcons(icons) != (s.navTabIcons ?: "") ||
        showLabels != s.navShowLabels || floating != s.navFloatingBar
    var editing by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    fun save() {
        scope.launch {
            val landing = NavTabs.landingAfterHide(s.landingTab, layout)
            if (landing != s.landingTab) c.settings.setLandingTab(landing)
            c.settings.setNavTabLayout(NavTabs.encodeLayout(layout).takeIf { it != NavTabs.encodeLayout(NavTabs.decodeLayout(null)) })
            c.settings.setNavTabLabels(NavTabs.encodeLabels(labels).ifEmpty { null })
            c.settings.setNavTabIcons(NavTabs.encodeIcons(icons).ifEmpty { null })
            c.settings.setNavShowLabels(showLabels)
            c.settings.setNavFloatingBar(floating)
        }
    }
    val attemptLeave: () -> Unit = {
        if (dirty) confirmDiscard = true else nav.popBackSafely()
    }

    AcuranzaScreen(
        title = "Edit navigation tabs",
        subtitle = "Tap a tab to customize its appearance and behavior",
        onBack = attemptLeave,
        actions = {
            TextButton(onClick = attemptLeave) { Text("Cancel") }
            TextButton(enabled = dirty, onClick = { save(); nav.popBackSafely() }) { Text("Save") }
        },
    ) { padding ->
        NavTabsEditorContent(
            layout = layout,
            labels = labels,
            icons = icons,
            showLabels = showLabels,
            floating = floating,
            modifier = Modifier.padding(padding),
            onLayout = { layout = it },
            onLabel = { labels = it },
            onIcon = { icons = it },
            onShowLabels = { showLabels = it },
            onFloating = { floating = it },
            onEditTab = { editing = it },
            onResetClick = { confirmReset = true },
        )
    }

    editing?.let { route ->
        TabCustomizeDialog(
            route = route,
            label = labels[route] ?: com.acuranza.app.ui.nav.Tabs.first { it.route == route }.label,
            iconId = icons[route],
            onLabel = { next -> labels = next },
            onIcon = { next -> icons = next },
            onDismiss = { editing = null },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            "Discard changes?",
            "Your navigation customizations haven't been saved.",
            "Discard",
            destructive = true,
            onConfirm = { confirmDiscard = false; nav.popBackSafely() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            "Reset navigation to the default bar?",
            "Order, visibility, labels and icons return to the original four tabs. Your landing-tab choice and data are untouched.",
            "Reset",
            destructive = true,
            onConfirm = {
                confirmReset = false
                layout = NavTabs.decodeLayout(null)
                labels = emptyMap()
                icons = emptyMap()
                showLabels = true
                floating = true
            },
            onDismiss = { confirmReset = false },
        )
    }
}

/** Stateless body: the live preview (the real bar) pinned above the editor list. */
@Composable
fun NavTabsEditorContent(
    layout: List<Pair<String, Boolean>>,
    labels: Map<String, String>,
    icons: Map<String, String>,
    showLabels: Boolean,
    floating: Boolean,
    modifier: Modifier = Modifier,
    onLayout: (List<Pair<String, Boolean>>) -> Unit = {},
    onLabel: (Map<String, String>) -> Unit = {},
    onIcon: (Map<String, String>) -> Unit = {},
    onShowLabels: (Boolean) -> Unit = {},
    onFloating: (Boolean) -> Unit = {},
    onEditTab: (String) -> Unit = {},
    onResetClick: () -> Unit = {},
) {
    val visibleCount = layout.count { it.second }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "preview") {
            AcuranzaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Live preview", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (floating) "Floating bar — content scrolls underneath" else "Docked bar — hugging the screen edge",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcuranzaTheme.colors.subtleText,
                    )
                    Spacer(Modifier.height(12.dp))
                    // The real bar, fed the working copy — exactly what Save will show.
                    com.acuranza.app.ui.AcuranzaBottomBar(
                        current = layout.first { it.second }.first,
                        onTab = onEditTab,
                        tabs = NavTabs.resolve(layout, labels, icons),
                        showLabels = showLabels,
                        floating = floating,
                    )
                }
            }
        }
        item(key = "appearance") {
            Text("APPEARANCE", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            Spacer(Modifier.height(2.dp))
            AcuranzaCard(contentPadding = PaddingValues(0.dp)) {
                SettingsToggle(
                    "Labels under icons",
                    "Hide them for an icons-only bar",
                    showLabels,
                    onShowLabels,
                    null,
                    divider = true,
                )
                SettingsToggle(
                    "Floating bar",
                    "The rounded pill that hovers above the gesture area",
                    floating,
                    onFloating,
                    null,
                    divider = false,
                )
            }
        }
        item(key = "tabs-label") {
            Text("TABS", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            Text(
                "Tap a tab to change its icon or name. At least two tabs stay visible.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        items(layout.size, key = { "t-" + layout[it].first }) { index ->
            val (route, visible) = layout[index]
            val tab = com.acuranza.app.ui.nav.Tabs.first { it.route == route }
            val resolved = NavTabs.iconOf(route, icons[route])
            AcuranzaCard(Modifier.fillMaxWidth().padding(vertical = 0.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.DragIndicator, contentDescription = null, tint = AcuranzaTheme.colors.subtleText, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Icon(resolved.icon, contentDescription = null, tint = if (visible) AcuranzaTheme.colors.ink else AcuranzaTheme.colors.tertiaryText, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            labels[route] ?: tab.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (visible) MaterialTheme.colorScheme.onSurface else AcuranzaTheme.colors.subtleText,
                        )
                        Text(
                            if (visible) "Visible" else "Hidden — features stay reachable from Settings search",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcuranzaTheme.colors.subtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(enabled = index > 0, onClick = { onLayout(NavTabs.move(layout, index, index - 1)) }) {
                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move ${tab.label} up")
                    }
                    IconButton(enabled = index < layout.lastIndex, onClick = { onLayout(NavTabs.move(layout, index, index + 1)) }) {
                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move ${tab.label} down")
                    }
                    IconButton(
                        enabled = visible || visibleCount < com.acuranza.app.ui.nav.Tabs.size,
                        onClick = {
                            if (visible) {
                                if (visibleCount > 2) onLayout(NavTabs.setVisibility(layout, route, false))
                            } else {
                                onLayout(NavTabs.setVisibility(layout, route, true))
                            }
                        },
                    ) {
                        Icon(
                            if (visible) Icons.Outlined.Close else Icons.Outlined.Visibility,
                            contentDescription = if (visible) "Hide ${tab.label}" else "Show ${tab.label}",
                            tint = AcuranzaTheme.colors.subtleText,
                        )
                    }
                }
            }
        }
        item(key = "hint") {
            Text(
                "Changing a tab's name or icon only changes what you see — its destination, history and shortcuts are untouched.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        item(key = "reset") {
            AcuranzaCard {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reset to default", style = MaterialTheme.typography.bodyLarge)
                        Text("The original four tabs, names and icons", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                    }
                    TextButton(onClick = onResetClick) { Text("Reset") }
                }
            }
        }
    }
}

@Composable
private fun TabCustomizeDialog(
    route: String,
    label: String,
    iconId: String?,
    onLabel: (Map<String, String>) -> Unit,
    onIcon: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(label) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val clean = name.trim()
                onLabel(if (clean.isEmpty()) emptyMap() else mapOf(route to clean))
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Customize ${com.acuranza.app.ui.nav.Tabs.first { it.route == route }.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(12) },
                    singleLine = true,
                    label = { Text("Tab name") },
                    supportingText = { Text("Display-only — the destination never changes.") },
                )
                Text("Icon", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavTabs.ICON_CHOICES[route]?.forEach { choice ->
                        val selected = (iconId ?: NavTabs.ICON_CHOICES[route]!!.first().id) == choice.id
                        IconButton(onClick = {
                            onIcon(mapOf(route to choice.id))
                            onDismiss()
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    choice.icon,
                                    contentDescription = choice.label,
                                    tint = if (selected) AcuranzaTheme.colors.accent else AcuranzaTheme.colors.subtleText,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    choice.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) AcuranzaTheme.colors.accent else AcuranzaTheme.colors.subtleText,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}
