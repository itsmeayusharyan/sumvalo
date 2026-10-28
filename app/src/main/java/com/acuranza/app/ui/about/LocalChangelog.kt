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
 *
 * Release dates are included only where they can be verified from the project's release history.
 * For older versions where no verified date exists, the date is left unset and the UI shows
 * "Date not recorded" rather than fabricating a date.
 */
val LocalChangelog: List<ReleaseEntry> = listOf(
    ReleaseEntry(
        tag = "v0.6",
        name = "Sumvalo 0.6",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Activity tab restored to previous design: transaction list, filters, chips, and rows match v0.4 layout and styling", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.5",
        name = "Sumvalo 0.5",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Loan EMI auto-recording: each due EMI is added as an unreviewed expense from your first bank or wallet account, ready to confirm in Review", ChangeCategory.ADDED),
            ChangeItem("Duplicate detection on SMS import: same account, type and amount within ±1 day is skipped instead of double-counted", ChangeCategory.ADDED),
            ChangeItem("Bill countdown badges on home: urgent/overdue bills show a coloured \"Due today\" / \"Overdue\" pill", ChangeCategory.ADDED),
            ChangeItem("SMS confidence threshold setting: pick LOW/MEDIUM/HIGH to gate auto-import vs review", ChangeCategory.ADDED),
            ChangeItem("Tags as quick-add chips in the add-transaction screen", ChangeCategory.ADDED),
            ChangeItem("Universal APKs for arm64-v8a, armeabi-v7a, x86 and x86_64", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.4",
        name = "Sumvalo 0.4",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Loan EMI auto-recording: each due EMI is added as an unreviewed expense from your first bank or wallet account, ready to confirm in Review — no more entering EMIs by hand", ChangeCategory.ADDED),
            ChangeItem("Duplicate detection on SMS import: a transaction with the same account, type and amount within ±1 day of an existing one is skipped instead of silently double-counted", ChangeCategory.ADDED),
            ChangeItem("Bill countdown badges on the home screen: urgent and overdue bills show a coloured \"Due today\" / \"Overdue\" pill next to the card name", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.3",
        name = "Sumvalo 0.3",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Universal APK packaging: both standard and full build variants now include native libraries for arm64-v8a, armeabi-v7a, x86, and x86_64 — any compatible Android CPU architecture can install", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.2",
        name = "Sumvalo 0.2",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Removed Changelog section from the About page — About now shows Device and build and Legal sections only", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.1",
        name = "Sumvalo 0.1",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Rolled back to v2.11 codebase and reset to v0.1 — clean minimal foundation for the whole-app UI redesign", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.11",
        name = "Sumvalo 2.11",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Changelog screen release cards now match the About section cards exactly — same SumvaloCard surface, hairline border, 20dp corner radius, IconBubble leading icon, ChevronRight trailing, RowSkeleton padding and typography; removed SettingsSection's extra 24dp top spacing so each card is exactly one row tall", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.10",
        name = "Sumvalo 2.10",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Changelog screen release cards now match the About section card layout exactly — leading version icon in rounded-square container, ChevronRight on the right, same card surface, hairline border, corner radius, internal padding, typography and spacing as Privacy Policy and Terms of Service cards", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.9",
        name = "Sumvalo 2.9",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Changelog screen cards now match the About section layout exactly — same card surface, hairline border, corner radius, internal padding, typography and expand/collapse indicator as Privacy Policy and other About entries", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.8",
        name = "Sumvalo 2.8",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Redesigned Changelog screen UI: tighter, more compact release cards with reduced vertical spacing and balanced padding throughout", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.7",
        name = "Sumvalo 2.7",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Redesigned Changelog screen UI: clearer visual hierarchy with prominent version numbers, muted release dates, balanced card spacing, refined empty state, and consistent expand/collapse indicators", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.6",
        name = "Sumvalo 2.6",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Redesigned Changelog screen: cleaner version cards with prominent version numbers, muted release dates, balanced spacing, and a premium empty state", ChangeCategory.IMPROVED),
            ChangeItem("Changelog cards: refined padding, subtle hairline borders, consistent corner radii, release notes that wrap naturally without clipping", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.5",
        name = "Sumvalo 2.5",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed Review Bank Transactions screen: removed excessive bottom padding on Cancel/Confirm buttons so they sit at a balanced distance above the floating navigation bar", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.4",
        name = "Sumvalo 2.4",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Redesigned Changelog screen: cleaner version cards with prominent version numbers, secondary muted release dates, consistent expand/collapse icons, and balanced spacing", ChangeCategory.IMPROVED),
            ChangeItem("Changelog cards: refined padding, subtle hairline borders, consistent corner radii, and release notes that wrap naturally without clipping", ChangeCategory.IMPROVED),
            ChangeItem("Empty state: refined spacing and typography for a premium empty state", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.3",
        name = "Sumvalo 2.3",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed bottom navigation tab switching: tabs remain responsive after visiting nested Settings screens such as Appearance, Theme, Accent Colour, Haptic Feedback, Edit Navigation Tabs and Changelog", ChangeCategory.FIXED),
            ChangeItem("Repeated Home taps while on the dashboard scroll to the top and refresh displayed content from local data", ChangeCategory.IMPROVED),
            ChangeItem("Navigation back stack preserved correctly across tab switches — no duplicate routes or stale navigation state", ChangeCategory.FIXED),
            ChangeItem("Active tab highlight always matches the current destination", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v2.2",
        name = "Sumvalo 2.2",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed Review Bank Transactions screen: header text no longer overlaps or duplicates; proper vertical hierarchy with back button, title and subtitle", ChangeCategory.FIXED),
            ChangeItem("Transaction cards: independent layout bounds, consistent spacing, multiline wrapping for merchant names, dates and account warnings", ChangeCategory.FIXED),
            ChangeItem("Selection switches stay in sync during scrolling; stable list keys prevent incorrect row reuse", ChangeCategory.FIXED),
            ChangeItem("Confirm button shows selected-transaction count and disables when nothing is selected", ChangeCategory.FIXED),
            ChangeItem("Cancel exits without importing; Confirm imports only selected, valid, non-duplicate transactions", ChangeCategory.FIXED),
        ),
    ),
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
            ChangeItem("Changelog feature added to the About section", ChangeCategory.ADDED),
            ChangeItem("Screenshots updated with latest Paparazzi renders", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.8",
        name = "Sumvalo 1.8",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed bottom navigation bar navigation bugs — both floating and attached navigation bars restored to full functionality", ChangeCategory.FIXED),
            ChangeItem("Restored v1.1 navigation bar with floating and docked modes", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.7",
        name = "Sumvalo 1.7",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed bottom navigation bar excessive gap — removed excessive bottom padding so content scrolls naturally and the bar stays near the system navigation area", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.6",
        name = "Sumvalo 1.6",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed oversized floating navigation bar height and padding", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.5",
        name = "Sumvalo 1.5",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed floating navigation bar — persistent selection, gestures, and window insets", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.4",
        name = "Sumvalo 1.4",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Replaced glassmorphism and gradients with flat, restrained design", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.3",
        name = "Sumvalo 1.3",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Package ID switched to com.sumvalo.app", ChangeCategory.OTHER),
        ),
    ),
    ReleaseEntry(
        tag = "v1.2",
        name = "Sumvalo 1.2",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Animation audit and fixes", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.1",
        name = "Sumvalo 1.1",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Floating and docked navigation bar modes", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v1.0",
        name = "Sumvalo 1.0",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Initial release", ChangeCategory.ADDED),
        ),
    ),
).sortedWith(compareByDescending<ReleaseEntry> { it.publishedAt }.thenByDescending { it.tag })
