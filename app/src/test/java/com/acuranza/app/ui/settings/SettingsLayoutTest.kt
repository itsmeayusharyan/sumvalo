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
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden("~~;;>>>" ))
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
        // An old install's string mentions retired section ids ("management", "general") plus
        // one current one ("money"). Unknown sections drop out; their entries return via their
        // NEW default homes. Sections mentioned keep their listed entries plus defaults not listed.
        val legacy = "management>Account|accounts;general>General|financial;money~Money>accounts,budget"
        val decoded = SettingsHubLayout.decode(legacy)
        assertEquals(SettingsSectionKey.entries.toSet(), decoded.sections.map { it.key }.toSet())
        assertEquals(
            listOf(SettingsEntryKey.ACCOUNTS, SettingsEntryKey.BUDGET, SettingsEntryKey.LOANS, SettingsEntryKey.LENDING),
            decoded.sections.first { it.key == SettingsSectionKey.MONEY }.entries,
        )
        assertEquals(
            listOf(SettingsEntryKey.DATA, SettingsEntryKey.PRIVACY),
            decoded.sections.first { it.key == SettingsSectionKey.DATA_PRIVACY }.entries,
        )
    }

    @Test
    fun `duplicate ids are ignored on decode`() {
        val decoded = SettingsHubLayout.decode("more>about,about,about")
        // Duplicate "about" in the entry list is ignored: ABOUT appears once in More; FINANCIAL
        // (mentioned nowhere) keeps its default home under Preferences.
        val moreEntries = decoded.sections.first { it.key == SettingsSectionKey.MORE }.entries
        assertEquals(1, moreEntries.count { it == SettingsEntryKey.ABOUT })
        val prefEntries = decoded.sections.first { it.key == SettingsSectionKey.PREFERENCES }.entries
        assertEquals(1, prefEntries.count { it == SettingsEntryKey.FINANCIAL })
    }

    @Test
    fun `an entry mentioned nowhere keeps its default home`() {
        val decoded = SettingsHubLayout.decode("privacy>privacy")
        assertTrue(decoded.sections.first { it.key == SettingsSectionKey.MONEY }.entries.contains(SettingsEntryKey.BUDGET))
        assertTrue(decoded.sections.first { it.key == SettingsSectionKey.DATA_PRIVACY }.entries.contains(SettingsEntryKey.DATA))
    }

    @Test
    fun `hidden entries are removed from sections and listed, then re-shown into their default section`() {
        var l = SettingsHubLayout.decodeWithHidden("money~Money>accounts,budget;!lending")
        assertEquals(false, l.sections.first { it.key == SettingsSectionKey.MONEY }.entries.contains(SettingsEntryKey.LENDING))
        assertTrue(l.hidden.contains(SettingsEntryKey.LENDING))
    }

    @Test
    fun `hide keeps a shortcut exactly once even if hidden twice`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.hideEntry(l, SettingsEntryKey.BUDGET)
        l = SettingsHubLayout.hideEntry(l, SettingsEntryKey.BUDGET)
        assertEquals(1, l.hidden.count { it == SettingsEntryKey.BUDGET })
    }

    @Test
    fun `rename changes only the displayed title`() {
        val l = SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.MORE, "About Sumvalo")
        assertEquals("About Sumvalo", l.sections.first { it.key == SettingsSectionKey.MORE }.title)
        assertEquals(SettingsSectionKey.MORE.defaultTitle, SettingsSectionKey.MORE.defaultTitle)
        // The key, the entries and the routes are untouched.
        assertEquals(
            listOf(SettingsEntryKey.ABOUT),
            l.sections.first { it.key == SettingsSectionKey.MORE }.entries,
        )
    }

    @Test
    fun `blank or overlong renames are rejected`() {
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.MORE, "   "))
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.renameSection(SettingsHubLayout.DEFAULT, SettingsSectionKey.MORE, "x".repeat(41)))
    }

    @Test
    fun `default layout sorts the landing grid alphabetically by title`() {
        val titles = SettingsHubLayout.visibleFlat(null).map { SettingsEntryDisplay.getValue(it).title.lowercase() }
        assertEquals(titles, titles.sorted())
        // The spec's example ordering, concretely:
        assertEquals(
            listOf(
                "about",
                "accounts & cards",
                "appearance",
                "budgets & goals",
                "data management",
                "lending",
                "loans & emi",
                "notifications",
                "privacy & security",
                "tools",
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
        val flat = SettingsHubLayout.visibleFlat("money>Money|accounts,budget;!lending")
        assertEquals(false, flat.contains(SettingsEntryKey.LENDING))
        assertEquals(true, flat.contains(SettingsEntryKey.ABOUT))
    }

    @Test
    fun `section order persists through encode and decode`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.moveSection(l, l.sections.indexOfFirst { it.key == SettingsSectionKey.MORE }, up = true)
        val decoded = SettingsHubLayout.decodeWithHidden(SettingsHubLayout.encode(l))
        assertEquals(l.sections.map { it.key }, decoded.sections.map { it.key })
    }

    @Test
    fun `entry order persists through encode and decode`() {
        var l = SettingsHubLayout.DEFAULT
        l = SettingsHubLayout.moveEntry(l, SettingsSectionKey.MONEY, 1, up = true)
        val decoded = SettingsHubLayout.decodeWithHidden(SettingsHubLayout.encode(l))
        assertEquals(l.sections.first { it.key == SettingsSectionKey.MONEY }.entries, decoded.sections.first { it.key == SettingsSectionKey.MONEY }.entries)
    }

    @Test
    fun `a customized layout with a default look encodes to null-equivalent minimal form`() {
        // Saving the default layout stores null (the setter receives null), so Reset and
        // fresh installs share one code path.
        val encoded = SettingsHubLayout.encode(SettingsHubLayout.DEFAULT)
        assertEquals(SettingsHubLayout.DEFAULT, SettingsHubLayout.decodeWithHidden(encoded))
    }
}
