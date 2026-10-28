package com.acuranza.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Customize-settings model: everything that persists or degrades goes through these pure
 * functions, so persistence, reset and corrupt-string behaviour are testable without DataStore.
 */
class SettingsLayoutTest {

    @Test
    fun `null or blank decodes to the default layout`() {
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden(null))
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden("  "))
    }

    @Test
    fun `garbage decodes safely to the default`() {
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden("~~;;>>>"))
    }

    @Test
    fun `every shortcut survives a round trip`() {
        val encoded = SettingsHubLayout.encode(SettingsHubLayout.DEFAULT)
        val decoded = SettingsHubLayout.decodeWithHidden(encoded)
        assertEquals(SettingsEntryKey.entries.toSet(), decoded.sections.flatMap { it.entries }.toSet())
        assertTrue(decoded.hidden.isEmpty())
    }

    @Test
    fun `unknown ids are dropped and known sections missing from an old string come back`() {
        // An old install's string missing a section added later: the section returns, entries intact.
        // A v1.0.25-era layout string mentioning the retired "management"/"general" sections:
        // unknown sections drop out, and their entries return via their NEW default homes.
        val legacy = "management>Account|accounts;general>General|financial,widgets;appearance>Appearance|appearance"
        val decoded = SettingsHubLayout.decode(legacy)
        assertEquals(SettingsSectionKey.entries.toSet(), decoded.sections.map { it.key }.toSet())
        assertEquals(
            listOf(SettingsEntryKey.FINANCIAL, SettingsEntryKey.LENDING, SettingsEntryKey.LOANS, SettingsEntryKey.BUDGET, SettingsEntryKey.GOALS),
            decoded.sections.first { it.key == SettingsSectionKey.FINANCIAL }.entries,
        )
        assertEquals(
            listOf(SettingsEntryKey.ACCOUNTS, SettingsEntryKey.DATA),
            decoded.sections.first { it.key == SettingsSectionKey.DATA }.entries,
        )
    }

    @Test
    fun `duplicate ids are ignored on decode`() {
        val decoded = SettingsHubLayout.decode("about>about,about,about")
        assertEquals(listOf(SettingsEntryKey.ABOUT), decoded.sections.first { it.key == SettingsSectionKey.ABOUT }.entries)
    }

    @Test
    fun `an entry mentioned nowhere keeps its default home`() {
        val decoded = SettingsHubLayout.decode("privacy>privacy")
        assertTrue(decoded.sections.first { it.key == SettingsSectionKey.APPEARANCE }.entries.contains(SettingsEntryKey.WIDGETS))
        assertTrue(decoded.sections.first { it.key == SettingsSectionKey.DATA }.entries.contains(SettingsEntryKey.ACCOUNTS))
    }

    @Test
    fun `hidden entries are removed from sections and listed, then re-shown into their default section`() {
        var l = SettingsHubLayout.decodeWithHidden("appearance>Appearance & personalization|appearance;!widgets;financial>Financial tools|financial")
        assertEquals(listOf(SettingsEntryKey.APPEARANCE), l.sections.first { it.key == SettingsSectionKey.APPEARANCE }.entries)
        assertEquals(listOf(SettingsEntryKey.WIDGETS), l.hidden)
        l = SettingsHubLayout.showEntry(l, SettingsEntryKey.WIDGETS)
        assertTrue(l.hidden.isEmpty())
        assertEquals(
            listOf(SettingsEntryKey.APPEARANCE, SettingsEntryKey.WIDGETS),
            l.sections.first { it.key == SettingsSectionKey.APPEARANCE }.entries,
        )
    }

    @Test
    fun `hide keeps a shortcut exactly once even if hidden twice`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.hideEntry(l, SettingsEntryKey.WIDGETS)
        l = SettingsHubLayout.hideEntry(l, SettingsEntryKey.WIDGETS)
        assertEquals(1, l.hidden.count { it == SettingsEntryKey.WIDGETS })
    }

    @Test
    fun `rename changes only the displayed title`() {
        val l = SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.ABOUT, "About Sumvalo")
        assertEquals("About Sumvalo", l.sections.first { it.key == SettingsSectionKey.ABOUT }.title)
        assertEquals(SettingsSectionKey.ABOUT.defaultTitle, SettingsSectionKey.ABOUT.defaultTitle)
        // The key, the entries and the routes are untouched.
        assertEquals(
            listOf(SettingsEntryKey.ABOUT),
            l.sections.first { it.key == SettingsSectionKey.ABOUT }.entries,
        )
    }

    @Test
    fun `blank or overlong renames are rejected`() {
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.ABOUT, "   "))
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.ABOUT, "x".repeat(41)))
    }

    @Test
    fun `default layout sorts the landing grid alphabetically by title`() {
        val titles = SettingsHubLayout.visibleFlat(null).map { SettingsEntryDisplay.getValue(it).title.lowercase() }
        assertEquals(titles, titles.sorted())
        // The spec's example ordering, concretely:
        assertEquals(
            listOf(
                "about", "accounts & cards", "appearance", "budget", "data management",
                "financial tools", "goals", "lending", "loans & emi", "notifications",
                "privacy & security", "widgets",
            ),
            titles,
        )
    }

    @Test
    fun `an explicitly customized layout keeps the user order over the alphabetical default`() {
        val custom = SettingsHubLayout.encode(
            SettingsHubLayout.DEFAULT.copy(sections = SettingsHubLayout.DEFAULT.sections.reversed()),
        )
        val flat = SettingsHubLayout.visibleFlat(custom)
        // User's (reversed) section order wins — nothing is silently re-sorted.
        assertEquals(SettingsHubLayout.decodeWithHidden(custom).sections.flatMap { it.entries }, flat)
        val titles = flat.map { SettingsEntryDisplay.getValue(it).title.lowercase() }
        assertEquals(false, titles == titles.sorted())
    }

    @Test
    fun `hidden shortcuts are excluded from the landing grid`() {
        val flat = SettingsHubLayout.visibleFlat("appearance>Appearance & personalization|appearance;!widgets")
        assertEquals(false, flat.contains(SettingsEntryKey.WIDGETS))
        assertEquals(true, flat.contains(SettingsEntryKey.ABOUT))
    }

    @Test
    fun `section order persists through encode and decode`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.moveSection(l, l.sections.indexOfFirst { it.key == SettingsSectionKey.ABOUT }, up = true)
        val decoded = SettingsHubLayout.decodeWithHidden(SettingsHubLayout.encode(l))
        assertEquals(l.sections.map { it.key }, decoded.sections.map { it.key })
    }

    @Test
    fun `entry order persists through encode and decode`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.moveEntry(l, SettingsSectionKey.DATA, 1, up = true)
        val decoded = SettingsHubLayout.decodeWithHidden(SettingsHubLayout.encode(l))
        assertEquals(l.sections.first { it.key == SettingsSectionKey.DATA }.entries, decoded.sections.first { it.key == SettingsSectionKey.DATA }.entries)
    }

    @Test
    fun `a customized layout with a default look encodes to null-equivalent minimal form`() {
        // Saving the default layout stores null (the setter receives null), so Reset and
        // fresh installs share one code path.
        val encoded = SettingsHubLayout.encode(SettingsHubLayout.DEFAULT)
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden(encoded))
    }
}
