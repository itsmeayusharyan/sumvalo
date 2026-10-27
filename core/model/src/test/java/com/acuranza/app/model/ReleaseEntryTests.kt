package com.acuranza.app.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseEntryTests {

    @Test
    fun testVersionRemovesVPrefix() {
        val entry = ReleaseEntry("v1.9", "Sumvalo v1.9", "2026-10-26T10:00:00Z", null, "https://example.com")
        assertEquals("1.9", entry.version)
    }

    @Test
    fun testVersionWithoutVPrefix() {
        val entry = ReleaseEntry("1.9", "Sumvalo v1.9", "2026-10-26T10:00:00Z", null, "https://example.com")
        assertEquals("1.9", entry.version)
    }

    @Test
    fun testPublishedDateFormats() {
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", null, "https://example.com")
        assertEquals("26 Oct 2026", entry.publishedDate)
    }

    @Test
    fun testPublishedDateInvalidFormat() {
        val entry = ReleaseEntry("v1.9", "Release", "not-a-date", null, "https://example.com")
        assertEquals("not-a-date", entry.publishedDate)
    }

    @Test
    fun testChangesParsesAddedCategory() {
        val body = """## Added
        - Dark mode support
        - Export to CSV
        """
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", body, "https://example.com")
        assertEquals(2, entry.changes.size)
        assertEquals("Dark mode support", entry.changes[0].text)
        assertEquals(ChangeCategory.ADDED, entry.changes[0].category)
        assertEquals("Export to CSV", entry.changes[1].text)
        assertEquals(ChangeCategory.ADDED, entry.changes[1].category)
    }

    @Test
    fun testChangesParsesImprovedCategory() {
        val body = """## Improved
        - Better performance
        """
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", body, "https://example.com")
        assertEquals(1, entry.changes.size)
        assertEquals("Better performance", entry.changes[0].text)
        assertEquals(ChangeCategory.IMPROVED, entry.changes[0].category)
    }

    @Test
    fun testChangesParsesFixedCategory() {
        val body = """## Fixed
        - Crash on startup
        """
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", body, "https://example.com")
        assertEquals(1, entry.changes.size)
        assertEquals("Crash on startup", entry.changes[0].text)
        assertEquals(ChangeCategory.FIXED, entry.changes[0].category)
    }

    @Test
    fun testChangesParsesSecurityCategory() {
        val body = """## Security
        - Fix CVE-2026-1234
        """
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", body, "https://example.com")
        assertEquals(1, entry.changes.size)
        assertEquals("Fix CVE-2026-1234", entry.changes[0].text)
        assertEquals(ChangeCategory.SECURITY, entry.changes[0].category)
    }

    @Test
    fun testChangesEmptyBody() {
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", null, "https://example.com")
        assertTrue(entry.changes.isEmpty())
        assertFalse(entry.hasChanges)
    }

    @Test
    fun testChangesNoCategories() {
        val body = """- Just a change
        - Another change
        """
        val entry = ReleaseEntry("v1.9", "Release", "2026-10-26T10:00:00Z", body, "https://example.com")
        assertEquals(2, entry.changes.size)
        assertNull(entry.changes[0].category)
        assertNull(entry.changes[1].category)
    }

    @Test
    fun testChangesSortedNewestFirst() {
        val entries = listOf(
            ReleaseEntry("v1.8", "Release 1.8", "2026-09-15", "## Fixed\n- Bug fix", ""),
            ReleaseEntry("v1.9", "Release 1.9", "2026-10-26", "## Added\n- New feature", ""),
        ).sortedByDescending { it.publishedAt }

        assertEquals(2, entries.size)
        assertEquals("v1.9", entries[0].tag)
        assertEquals("v1.8", entries[1].tag)
        assertTrue(entries[0].hasChanges)
        assertTrue(entries[1].hasChanges)
    }

    @Test
    fun testChangeCategoryFromLine() {
        assertEquals(ChangeCategory.ADDED, ChangeCategory.fromLine("## Added features"))
        assertEquals(ChangeCategory.ADDED, ChangeCategory.fromLine("## New feature"))
        assertEquals(ChangeCategory.IMPROVED, ChangeCategory.fromLine("## Improved performance"))
        assertEquals(ChangeCategory.IMPROVED, ChangeCategory.fromLine("## Enhancements"))
        assertEquals(ChangeCategory.FIXED, ChangeCategory.fromLine("## Fixed bugs"))
        assertEquals(ChangeCategory.FIXED, ChangeCategory.fromLine("## Bug fix"))
        assertEquals(ChangeCategory.SECURITY, ChangeCategory.fromLine("## Security patch"))
        assertEquals(ChangeCategory.SECURITY, ChangeCategory.fromLine("## CVE-2026-1234"))
        assertNull(ChangeCategory.fromLine("## Random"))
    }
}
