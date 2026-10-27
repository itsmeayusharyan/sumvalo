package com.acuranza.app.ui.about

import com.acuranza.app.model.ChangeCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalChangelogTests {

    @Test
    fun testLocalChangelogHasV2Entry() {
        val v20 = LocalChangelog.firstOrNull { it.tag == "v2.0" }
        assertNotNull(v20)
        assertEquals("2.0", v20!!.version)
        assertFalse(v20.changes.isEmpty(), "v2.0 changes must not be empty")
        assertTrue(v20.changes.any { it.category == ChangeCategory.ADDED }, "v2.0 must have an Added change")
        assertTrue(v20.changes.any { it.category == ChangeCategory.IMPROVED }, "v2.0 must have an Improved change")
        assertTrue(v20.changes.any { it.category == ChangeCategory.FIXED }, "v2.0 must have a Fixed change")
    }

    @Test
    fun testLocalChangelogIsNewestFirst() {
        val releases = LocalChangelog
        val lastIdx = releases.size - 1
        for (i in 0 until lastIdx) {
            assertTrue(
                releases[i].publishedAt >= releases[i + 1].publishedAt,
                "Releases must be sorted newest first: ${releases[i].tag} before ${releases[i + 1].tag}"
            )
        }
    }

    @Test
    fun testLocalChangelogNoNetworkUrls() {
        // No entry should contain a hardcoded https URL (offline-only)
        for (release in LocalChangelog) {
            assertEquals("", release.htmlUrl, "htmlUrl must be empty for offline changelog")
            release.body?.let { body ->
                assertFalse(body.contains("https://"), "Release ${release.tag} body contains a hardcoded URL")
            }
        }
    }

    @Test
    fun testLocalChangelogEntriesHaveChanges() {
        for (release in LocalChangelog) {
            assertTrue(release.changes.isNotEmpty(), "Release ${release.tag} must have at least one categorized change")
        }
    }
}
