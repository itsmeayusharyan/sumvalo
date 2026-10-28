package com.acuranza.app.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Widgets
import com.acuranza.app.ui.nav.Routes

/**
 * The customizable Settings hub: which sections exist, their display order, the shortcuts inside
 * each (visible ones only), and optional display-only title overrides. Mirrors [com.acuranza.app.ui.home.HomeLayout]:
 * stable string ids in the persisted encoding, so renaming a label never breaks a saved layout and a
 * removed feature degrades gracefully on the next decode. Custom titles change ONLY the displayed
 * heading — never the entry identity, route or preference key.
 */

/** One hub section; [id] is what persists. */
enum class SettingsSectionKey(val id: String, val defaultTitle: String) {
    APPEARANCE("appearance", "Appearance & personalization"),
    FINANCIAL("financial", "Financial tools"),
    PRIVACY("privacy", "Privacy & security"),
    DATA("data", "Data management"),
    NOTIFICATIONS("notifications", "Notifications"),
    ABOUT("about", "About"),
}

/** One hub shortcut; [id] is what persists and [section] is where it lives by default. */
enum class SettingsEntryKey(val id: String, val section: SettingsSectionKey) {
    ACCOUNTS("accounts", SettingsSectionKey.DATA),
    FINANCIAL("financial", SettingsSectionKey.FINANCIAL),
    WIDGETS("widgets", SettingsSectionKey.APPEARANCE),
    APPEARANCE("appearance", SettingsSectionKey.APPEARANCE),
    PRIVACY("privacy", SettingsSectionKey.PRIVACY),
    DATA("data", SettingsSectionKey.DATA),
    NOTIFICATIONS("notifications", SettingsSectionKey.NOTIFICATIONS),
    ABOUT("about", SettingsSectionKey.ABOUT),
    LENDING("lending", SettingsSectionKey.FINANCIAL),
    LOANS("loans", SettingsSectionKey.FINANCIAL),
    BUDGET("budget", SettingsSectionKey.FINANCIAL),
}

/** Display data for one hub shortcut, shared by the hub and the Customize screen. */
internal data class SettingsEntryUi(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String,
)

internal val SettingsEntryDisplay: Map<SettingsEntryKey, SettingsEntryUi> = linkedMapOf(
    SettingsEntryKey.ACCOUNTS to SettingsEntryUi("Accounts & cards", "Accounts, imports and categories", Icons.Outlined.AccountBalance, Routes.SETTINGS_MANAGEMENT),
    SettingsEntryKey.FINANCIAL to SettingsEntryUi("Financial tools", "Recurring payments, reports", Icons.AutoMirrored.Outlined.TrendingUp, Routes.SETTINGS_FINANCIAL),
    SettingsEntryKey.LENDING to SettingsEntryUi("Lending", "Money between you and friends or family", Icons.Outlined.Handshake, Routes.DEBTS),
    SettingsEntryKey.LOANS to SettingsEntryUi("Loans & EMI", "What's left, prepayment savings, tax split", Icons.Outlined.RealEstateAgent, Routes.LOANS),
    SettingsEntryKey.BUDGET to SettingsEntryUi("Budgets & goals", "Budgets, savings goals and your salary plan", Icons.Outlined.TrackChanges, Routes.PLANNING),
    SettingsEntryKey.WIDGETS to SettingsEntryUi("Widgets", "Preview and add home-screen widgets", Icons.Outlined.Widgets, Routes.WIDGETS_PREVIEW),
    SettingsEntryKey.APPEARANCE to SettingsEntryUi("Appearance", "Theme, colour theme, accent, AMOLED, hide amounts, edit home", Icons.Outlined.Palette, Routes.SETTINGS_APPEARANCE),
    SettingsEntryKey.PRIVACY to SettingsEntryUi("Privacy & security", "App lock, PINs, encryption and recovery", Icons.Outlined.Shield, Routes.SETTINGS_PRIVACY),
    SettingsEntryKey.DATA to SettingsEntryUi("Data management", "Backups, imports, exports, sample data, erase", Icons.Outlined.Backup, Routes.SETTINGS_DATA),
    SettingsEntryKey.NOTIFICATIONS to SettingsEntryUi("Notifications", "Reminders, digest, widgets and where your budget month starts", Icons.Outlined.Alarm, Routes.SETTINGS_NOTIFICATIONS),
    SettingsEntryKey.ABOUT to SettingsEntryUi("About", "Version, privacy policy and licence", Icons.Outlined.Info, Routes.ABOUT),
)

/** A section as customized: its key, display title (override or default), visible entries in order. */
data class SettingsSection(val key: SettingsSectionKey, val title: String, val entries: List<SettingsEntryKey>)

/**
 * The full customized hub layout: ordered sections plus the shortcuts hidden from the landing page
 * (hidden shortcuts keep working — search still finds them, and edit mode can re-show them).
 */
data class SettingsHubLayout(val sections: List<SettingsSection>, val hidden: List<SettingsEntryKey>) {
    companion object {
        /** The layout every fresh install and every Reset gets. */
        val DEFAULT = SettingsHubLayout(
            sections = SettingsSectionKey.entries.map { s ->
                SettingsSection(s, s.defaultTitle, SettingsEntryKey.entries.filter { it.section == s })
            },
            hidden = emptyList(),
        )

        /**
         * Pure decode so persistence and reset are unit-testable without DataStore.
         * Encoding: `sectionId~TitleOverride>entry,entry;sectionId>entry;…` — overrides optional,
         * unknown ids dropped, sections/entries missing from the string appended at their default
         * spot, duplicates ignored. Anything unreadable falls back to [DEFAULT].
         */
        fun decode(raw: String?): SettingsHubLayout {
            if (raw.isNullOrBlank()) return DEFAULT
            val seenSections = mutableSetOf<SettingsSectionKey>()
            val seenEntries = mutableSetOf<SettingsEntryKey>()
            val sections = mutableListOf<SettingsSection>()
            for (token in raw.split(";")) {
                if (token.isBlank()) continue
                val head = token.substringBefore(">")
                val entryPart = if (token.contains(">")) token.substringAfter(">") else ""
                val (secId, titleOverride) = if (head.contains("~")) {
                    head.substringBefore("~") to head.substringAfter("~").trim().ifBlank { null }
                } else head to null
                val section = SettingsSectionKey.entries.firstOrNull { it.id == secId } ?: continue
                if (!seenSections.add(section)) continue
                val entries = entryPart.split(",").mapNotNull { id ->
                    SettingsEntryKey.entries.firstOrNull { it.id == id.trim() }?.takeIf { seenEntries.add(it) }
                }
                sections += SettingsSection(section, titleOverride ?: section.defaultTitle, entries)
            }
            if (sections.isEmpty()) return DEFAULT
            // Anything the app knows about but the string didn't mention keeps its default home —
            // a shortcut can never be lost to an old layout string.
            val complete = sections.map { s ->
                s.copy(entries = s.entries + SettingsEntryKey.entries.filter { it.section == s.key && it !in s.entries })
            } + SettingsSectionKey.entries.filter { it !in seenSections }.map { s ->
                SettingsSection(s, s.defaultTitle, SettingsEntryKey.entries.filter { it.section == s })
            }
            return SettingsHubLayout(complete, emptyList())
        }

        /** Pure encode of everything the user customized: order, per-section entries, title overrides. */
        fun encode(layout: SettingsHubLayout): String =
            layout.sections.joinToString(";") { s ->
                val head = if (s.title != s.key.defaultTitle) "${s.key.id}~${s.title}" else s.key.id
                if (s.entries.isEmpty()) head else "$head>${s.entries.joinToString(",") { it.id }}"
            } +
                if (layout.hidden.isEmpty()) "" else ";" + layout.hidden.joinToString(";") { "!${it.id}" }

        /**
         * The landing page's flat visible-shortcut order: alphabetical by displayed title
         * (case-insensitive) for everyone who never customized — but a layout string the user
         * explicitly customized keeps THEIR order; we never silently override that preference.
         */
        fun visibleFlat(raw: String?): List<SettingsEntryKey> {
            val flat = decodeWithHidden(raw).sections.flatMap { it.entries }
            val customized = !raw.isNullOrBlank() && raw.trim() != encode(DEFAULT)
            return if (customized) flat else flat.sortedBy { SettingsEntryDisplay.getValue(it).title.lowercase() }
        }

        /** Decodes and applies the hidden markers (`!id` tokens) on top of the section encoding. */
        fun decodeWithHidden(raw: String?): SettingsHubLayout {
            val hiddenIds = raw.orEmpty().split(";").filter { it.startsWith("!") }
                .mapNotNull { h -> SettingsEntryKey.entries.firstOrNull { it.id == h.removePrefix("!").trim() } }
            val base = decode(raw)
            if (hiddenIds.isEmpty()) return base
            val hiddenSet = hiddenIds.toSet()
            return base.copy(
                sections = base.sections.map { s -> s.copy(entries = s.entries.filterNot { it in hiddenSet }) },
                hidden = SettingsEntryKey.entries.filter { it in hiddenSet },
            )
        }

        fun moveEntry(l: SettingsHubLayout, section: SettingsSectionKey, index: Int, up: Boolean): SettingsHubLayout {
            val target = index + if (up) -1 else 1
            val entries = l.sections.firstOrNull { it.key == section }?.entries ?: return l
            if (target < 0 || target >= entries.size) return l
            val swapped = entries.toMutableList().apply { set(index, entries[target]); set(target, entries[index]) }
            return l.copy(sections = l.sections.map { if (it.key == section) it.copy(entries = swapped) else it })
        }

        fun moveSection(l: SettingsHubLayout, index: Int, up: Boolean): SettingsHubLayout {
            val target = index + if (up) -1 else 1
            if (target < 0 || target >= l.sections.size) return l
            val swapped = l.sections.toMutableList().apply { set(index, l.sections[target]); set(target, l.sections[index]) }
            return l.copy(sections = swapped)
        }

        fun hideEntry(l: SettingsHubLayout, e: SettingsEntryKey): SettingsHubLayout =
            l.copy(sections = l.sections.map { s -> s.copy(entries = s.entries.filterNot { it == e }) }, hidden = (l.hidden + e).distinct())

        fun showEntry(l: SettingsHubLayout, e: SettingsEntryKey): SettingsHubLayout =
            if (e !in l.hidden) l
            else l.copy(
                sections = l.sections.map { s ->
                    if (s.key == e.section) s.copy(entries = s.entries + e) else s
                },
                hidden = l.hidden.filterNot { it == e },
            )

        fun renameSection(l: SettingsHubLayout, s: SettingsSectionKey, title: String): SettingsHubLayout {
            val clean = title.trim()
            if (clean.isEmpty() || clean.length > 40) return l
            return l.copy(sections = l.sections.map { if (it.key == s) it.copy(title = clean) else it })
        }
    }
}
