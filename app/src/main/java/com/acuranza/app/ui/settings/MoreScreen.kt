package com.acuranza.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.acuranza.app.R
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.designsystem.theme.StaggeredEntrance
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.Routes

/** One destination on the Settings hub, with the group it belongs to. */
internal data class SettingsEntry(
    val key: SettingsEntryKey,
    val group: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
)

/** Every destination, in the spec's group structure — nothing removed, nothing hidden. */
// ONE registry, no drift: every shortcut's title/description/icon/route comes from
// SettingsEntryDisplay in SettingsLayout.kt — the same map the Customize editor renders from.
private val SettingsEntries = SettingsEntryKey.entries.map { key ->
    val ui = SettingsEntryDisplay.getValue(key)
    SettingsEntry(key, key.section.defaultTitle, ui.title, ui.description, ui.icon, ui.route)
}

private fun entryOf(key: SettingsEntryKey): SettingsEntry = SettingsEntries.first { it.key == key }

/**
 * The Settings hub: header with title + subtitle, search field that filters every destination by
 * name and description, a tappable profile tile, then the destinations grouped into labeled
 * sections (Money / Preferences / Data & privacy / More). Search shows a flat filtered list with
 * a graceful empty state; with an empty query the groups render in full. All colors come from the
 * theme, so Dark, Light, System and AMOLED flow through. The saved hub layout (order, hidden
 * shortcuts, title overrides) still governs what the landing page shows.
 */
@Composable
fun MoreScreen(nav: NavController) {
    val hubLayoutPref = LocalSettings.current.settingsLayout
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    // Deep search: the flat registry covers hub destinations AND rows buried inside nested pages.
    val hits = SettingsSearchIndex.searchSettings(q)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        item(key = "head") {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Settings",
                            style = TextStyle(fontFamily = Chakra, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
                            color = SumvaloTheme.colors.ink,
                        )
                        Text(
                            "Your money, on your phone",
                            style = chakraText(FontWeight.Medium, 13, 18),
                            color = SumvaloTheme.colors.subtleText,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS_EDIT) }) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Customize settings",
                            tint = SumvaloTheme.colors.subtleText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                SearchField(query, onQueryChange = { query = it })
            }
        }
        item(key = "profile") {
            val s = LocalSettings.current
            ProfileTile(name = s.profileName, avatarPath = s.profileAvatar) { nav.navigate(Routes.PROFILE) }
            Spacer(Modifier.height(12.dp))
        }
        if (q.isNotEmpty() && hits.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No settings match “${query.trim()}”", style = MaterialTheme.typography.titleSmall, color = SumvaloTheme.colors.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Try a shorter word — everything is also one tap away in the groups below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
            }
        } else if (q.isNotEmpty()) {
            item(key = "results-label") {
                Text(
                    "${hits.size} result" + if (hits.size == 1) "" else "s",
                    style = SumvaloText.sectionLabel,
                    color = SumvaloTheme.colors.subtleText,
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                )
            }
            items(hits, key = { "${it.group}/${it.page}/${it.title}" }) { hit ->
                SumvaloCard(Modifier.padding(vertical = 5.dp)) {
                    SettingsRow(
                        hit.title,
                        hit.description,
                        null,
                        value = hit.group,
                    ) { nav.navigate(hit.route) }
                }
            }
        } else {
            // The saved hub layout: order, per-section shortcuts and (display-only) title
            // overrides. Hidden shortcuts are simply absent here — search still finds them,
            // so nothing becomes unreachable, and Customize settings re-shows them any time.
            val layout = SettingsHubLayout.decodeWithHidden(hubLayoutPref)
            layout.sections.forEach { section ->
                val entries = section.entries
                if (entries.isEmpty()) return@forEach
                item(key = "group-${section.key.id}") {
                    SettingsGroup(section.title, entries) { key ->
                        nav.navigate(SettingsEntryDisplay.getValue(key).route)
                    }
                }
            }
        }
        item(key = "footer") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Sumvalo · Your data stays on this device",
                    style = chakraText(FontWeight.Medium, 12, 16),
                    color = SumvaloTheme.colors.subtleText,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** The search field: leading glass icon, clear button when something is typed. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search settings", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = SumvaloTheme.colors.tertiaryText) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear search", tint = SumvaloTheme.colors.tertiaryText)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = SumvaloTheme.colors.cardSurface,
            focusedContainerColor = SumvaloTheme.colors.cardSurface,
            unfocusedBorderColor = SumvaloTheme.colors.hairline,
            focusedBorderColor = SumvaloTheme.colors.accent,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
    )
}

/** Tappable profile tile: avatar + name (or default prompt), edit affordance. */
@Composable
private fun ProfileTile(name: String?, avatarPath: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SumvaloTheme.colors.cardSurface)
            .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(SumvaloTheme.colors.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.AccountBalanceWallet,
                contentDescription = null,
                tint = SumvaloTheme.colors.ink,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name?.takeIf { it.isNotBlank() } ?: "Your profile",
                style = chakraText(FontWeight.SemiBold, 15, 20),
                color = SumvaloTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (name.isNullOrBlank()) "Add your name and photo" else "Your profile",
                style = chakraText(FontWeight.Medium, 13, 18),
                color = SumvaloTheme.colors.subtleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onClick) {
            Icon(Icons.Outlined.Edit, contentDescription = "Edit profile", tint = SumvaloTheme.colors.tertiaryText, modifier = Modifier.size(20.dp))
        }
    }
}

/** One labeled group of hub rows (Money / Preferences / Data & privacy / More). */
@Composable
internal fun SettingsGroup(
    label: String,
    entries: List<SettingsEntryKey>,
    onNavigate: (SettingsEntryKey) -> Unit,
) {
    Column(Modifier.padding(top = 20.dp)) {
        Text(
            label.uppercase(),
            style = chakraText(FontWeight.SemiBold, 11, 14, letterSpacingSp = 0.12f),
            color = SumvaloTheme.colors.subtleText,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SumvaloTheme.colors.cardSurface)
                .border(1.dp, SumvaloTheme.colors.hairline, RoundedCornerShape(14.dp)),
        ) {
            entries.forEachIndexed { index, key ->
                val entry = entryOf(key)
                if (index > 0) {
                    V2Hairline(Modifier.padding(start = 56.dp))
                }
                SettingsRowV2(entry, onNavigate = { onNavigate(entry.key) })
            }
        }
    }
}

/** One hub row: icon box + title + chevron, 52dp tall, full width. */
@Composable
internal fun SettingsRowV2(entry: SettingsEntry, onNavigate: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(role = Role.Button, onClick = onNavigate)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SumvaloTheme.colors.ink.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(entry.icon, contentDescription = null, tint = SumvaloTheme.colors.ink, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            entry.title,
            style = chakraText(FontWeight.SemiBold, 14, 20),
            color = SumvaloTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = SumvaloTheme.colors.tertiaryText, modifier = Modifier.size(16.dp))
    }
}

/** A titled group of settings rows on a card. Kept for any other screens that use it. */
@Composable
fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(title.uppercase(), style = SumvaloText.sectionLabel, color = SumvaloTheme.colors.subtleText)
        Spacer(Modifier.height(8.dp))
        SumvaloCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) { content() }
    }
}

/** Chakra-flavoured copy of a text style, so settings headings carry the new voice. */
private fun chakraText(weight: FontWeight, size: Int, lineHeight: Int, letterSpacingSp: Float = 0f): TextStyle =
    TextStyle(fontFamily = Chakra, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = letterSpacingSp.sp)

/** Chakra Petch — bundled under res/font (SIL OFL). */
val Chakra = FontFamily(
    Font(R.font.chakrapetch_medium, FontWeight.Medium),
    Font(R.font.chakrapetch_semibold, FontWeight.SemiBold),
    Font(R.font.chakrapetch_bold, FontWeight.Bold),
)

/** One-line horizontal hairline used between design rows. */
@Composable
private fun V2Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
