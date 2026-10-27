package com.acuranza.app.ui.about

import com.acuranza.app.model.ChangeCategory
import com.acuranza.app.model.ChangeItem
import com.acuranza.app.model.ReleaseEntry

/**
 * Changelog entries maintained locally in the app's source code.
 *
 * This is the single source of truth for the About → Changelog screen. Entries are bundled
 * with the APK and work completely offline — no network request, API key, or account is needed.
 *
 * Workflow (mandatory): before every APK build after code changes, update the entry for the
 * version being built. See AGENTS.md for the full procedure.
 *
 * Entries are sorted newest first (newest release displayed first).
 */
val LocalChangelog: List<ReleaseEntry> = listOf(
    ReleaseEntry(
        tag = "v2.0",
        name = "Sumvalo 2.0",
        publishedAt = "2026-10-27",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Offline changelog — release notes bundled in app source code", ChangeCategory.ADDED),
            ChangeItem("Automatic SMS inbox scan every 10 minutes while the app is open (background catch-up every 15 minutes, Android minimum)", ChangeCategory.IMPROVED),
            ChangeItem("Fixed SMS import/review screen: header text no longer overlaps or duplicates; cancel/confirm buttons stay above the navigation bar; review list items have stable keys and independent layout bounds", ChangeCategory.FIXED),
            ChangeItem("Changelog screen works fully offline: no network, account, or API key needed", ChangeCategory.IMPROVED),
            ChangeItem("Removed INTERNET permission and GitHub Releases API fetching", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.9",
        name = "Sumvalo 1.9",
        publishedAt = "2026-10-25",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Restored v1.1 navigation bar with floating and docked modes", ChangeCategory.ADDED),
            ChangeItem("Fixed broken tab navigation and selected-state updates", ChangeCategory.FIXED),
        ),
    ),
).sortedByDescending { it.publishedAt }
