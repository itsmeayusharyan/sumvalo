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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings

/**
 * Customize settings: the edit mode for the Settings hub. Everything here edits a working copy of
 * the layout; Save persists it, Cancel drops it, Reset restores the default layout. Renaming
 * changes only the displayed heading — never a feature's identity, route or preference key.
 */
@Composable
fun SettingsEditScreen(nav: NavController) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val saved = LocalSettings.current.settingsLayout
    var working by remember { mutableStateOf(SettingsHubLayout.decodeWithHidden(saved)) }

    AcuranzaScreen(
        title = "Customize settings",
        subtitle = "Order, hide and rename — nothing is deleted",
        onBack = { nav.popBackSafely() },
        actions = {
            TextButton(onClick = { nav.popBackSafely() }) { Text("Cancel") }
            TextButton(
                onClick = {
                    scope.launch {
                        c.settings.setSettingsLayout(SettingsHubLayout.encode(working).takeIf { it != SettingsHubLayout.encode(SettingsHubLayout.DEFAULT) })
                    }
                    nav.popBackSafely()
                },
            ) { Text("Save") }
        },
    ) { padding ->
        SettingsEditContent(
            layout = working,
            modifier = Modifier.padding(padding),
            onLayout = { working = it },
            onReset = { working = SettingsHubLayout.DEFAULT },
        )
    }
}

/** Stateless body so screenshot fixtures render every state without persistence or navigation. */
@Composable
fun SettingsEditContent(
    layout: SettingsHubLayout,
    modifier: Modifier = Modifier,
    onLayout: (SettingsHubLayout) -> Unit = {},
    onReset: () -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "hint") {
            Text(
                "Move, hide or rename sections and shortcuts. Hidden shortcuts stay reachable " +
                    "from the Settings search, and renaming only changes the label you see.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
            )
        }
        layout.sections.forEachIndexed { index, section ->
            item(key = "edit-${section.key}") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(section.title, style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                        }
                        RenameButton(section) { title -> onLayout(SettingsHubLayout.renameSection(layout, section.key, title)) }
                        MoveButton(
                            upEnabled = index > 0,
                            downEnabled = index < layout.sections.lastIndex,
                            onUp = { onLayout(SettingsHubLayout.moveSection(layout, index, up = true)) },
                            onDown = { onLayout(SettingsHubLayout.moveSection(layout, index, up = false)) },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    AcuranzaCard(contentPadding = PaddingValues(0.dp)) {
                        section.entries.forEachIndexed { entryIndex, key ->
                            val entry = SettingsEntryDisplay[key] ?: return@forEachIndexed
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(entry.icon, contentDescription = null, tint = AcuranzaTheme.colors.subtleText, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(entry.description, style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                                }
                                MoveButton(
                                    upEnabled = entryIndex > 0,
                                    downEnabled = entryIndex < section.entries.lastIndex,
                                    onUp = { onLayout(SettingsHubLayout.moveEntry(layout, section.key, entryIndex, up = true)) },
                                    onDown = { onLayout(SettingsHubLayout.moveEntry(layout, section.key, entryIndex, up = false)) },
                                )
                                IconButton(
                                    onClick = { onLayout(SettingsHubLayout.hideEntry(layout, key)) },
                                    modifier = Modifier.size(48.dp),
                                ) { Icon(Icons.Outlined.Close, contentDescription = "Hide ${entry.title}") }
                            }
                        }
                    }
                }
            }
        }
        if (layout.hidden.isNotEmpty()) {
            item(key = "hidden") {
                Column {
                    Text("Hidden from the hub", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                    Spacer(Modifier.height(6.dp))
                    AcuranzaCard(contentPadding = PaddingValues(0.dp)) {
                        layout.hidden.forEach { key ->
                            val entry = SettingsEntryDisplay[key] ?: return@forEach
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(entry.icon, contentDescription = null, tint = AcuranzaTheme.colors.subtleText, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                                    Text("Reachable from Settings search while hidden", style = MaterialTheme.typography.bodySmall, color = AcuranzaTheme.colors.subtleText)
                                }
                                IconButton(
                                    onClick = { onLayout(SettingsHubLayout.showEntry(layout, key)) },
                                    modifier = Modifier.size(48.dp),
                                ) { Icon(Icons.Outlined.Visibility, contentDescription = "Show ${entry.title} again") }
                            }
                        }
                    }
                }
            }
        }
        item(key = "reset") {
            AcuranzaCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Reset to default", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Restores the original sections, order and names. Your data and every other setting are untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AcuranzaTheme.colors.subtleText,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onReset) { Text("Reset") }
                }
            }
        }
        item(key = "tail") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MoveButton(upEnabled: Boolean, downEnabled: Boolean, onUp: () -> Unit, onDown: () -> Unit) {
    Row {
        IconButton(onClick = onUp, enabled = upEnabled, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.ArrowUpward, contentDescription = "Move up")
        }
        IconButton(onClick = onDown, enabled = downEnabled, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Outlined.ArrowDownward, contentDescription = "Move down")
        }
    }
}

@Composable
private fun RenameButton(section: SettingsSection, onRename: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var text by remember(section) { mutableStateOf(section.title) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
        Icon(Icons.Outlined.Edit, contentDescription = "Rename ${section.title}")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        open = false
                        onRename(text)
                    },
                    enabled = text.isNotBlank(),
                ) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
            title = { Text("Rename “${section.title}”") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(40) },
                    singleLine = true,
                    label = { Text("Section name") },
                    supportingText = { Text("Changes only the label shown on the hub.") },
                )
            },
        )
    }
}
