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
        tag = "v0.36",
        name = "Sumvalo 0.36",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fix: Onboarding hero cards now use a consistent height across all five steps (no layout jumps between screens)", ChangeCategory.FIXED),
            ChangeItem("Fix: Onboarding spacing now follows a consistent 24dp grid — no more inconsistent padding between hero, progress, headline, and content sections", ChangeCategory.FIXED),
            ChangeItem("Fix: Onboarding decorative elements now scale proportionally to screen width — no overlap on small screens, no drift on large screens", ChangeCategory.FIXED),
            ChangeItem("Fix: Onboarding navigation buttons (Back/Skip) and hero logo use consistent 24dp horizontal padding", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.35",
        name = "Sumvalo 0.35",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fix: Home screen now shows a loading indicator during initial data load instead of rendering empty/zero-value dashboard UI (no more dummy/placeholder screen on launch)", ChangeCategory.FIXED),
            ChangeItem("Fix: Onboarding primary CTA buttons render as pill-shaped buttons instead of clipped circles", ChangeCategory.FIXED),
            ChangeItem("Fix: Onboarding Skip button now has a visible background on dark hero cards", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.34",
        name = "Sumvalo 0.34",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Security-review close-out: database fails closed when the key exists but can't be unwrapped (even after a prior plaintext fallback); background workers and widget updates route through acquireContainer so they no-op instead of crash-looping", ChangeCategory.FIXED),
            ChangeItem("Minor: weekly recurring monthly-equivalent now rounds instead of truncating (1 paise); removed leftover date formatter and unused route constants", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.33",
        name = "Sumvalo 0.33",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Security hardening from a full audit + penetration test — a broken key store can no longer silently store new data unencrypted, turning the app lock off (or installing a first PIN) now asks for fingerprint/face first, and backup/restore now carries split groups, purchase records and SMS-import bookkeeping", ChangeCategory.FIXED),
            ChangeItem("Money-integrity fixes — chained calculator expressions, crafted bank messages and restored files can no longer wrap amounts into garbage values; goal contributions and debt repayments became atomic so double-taps can no longer drop one", ChangeCategory.FIXED),
            ChangeItem("Privacy consistency — privacy mode now also masks the SMS-capture alert and price-change notification titles, and launcher widgets switch to the decoy profile with it", ChangeCategory.IMPROVED),
            ChangeItem("Startup polish — the splash now lasts exactly as long as settings take to load instead of a fixed 700 ms; background bookkeeping moved off the main thread", ChangeCategory.IMPROVED),
            ChangeItem("Dead code removed across DAOs, repositories, navigation and resources after a full unused-code audit", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.32",
        name = "Sumvalo 0.32",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("App lock screen redesigned to the new reference — bordered logo tile, the data-stays promise, a fingerprint sensor affordance, an Unlock pill and a device-PIN fallback; PIN pad and decoy-PIN unlock behave exactly as before", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.31",
        name = "Sumvalo 0.31",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Recurring payments redesigned to the new reference — monthly-cost hero card, the next 30 days in one grouped card, your schedules with reminder-only and paused states, and spotted subscriptions with price-change, AutoPay and Stopped? pills", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.30",
        name = "Sumvalo 0.30",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Goals tab redesigned to the new reference — goal cards with a donut progress ring, compact saved-of-target amounts, a green save-plan line, the confidence outlook row with projected monthly and date, and an Add savings button", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.29",
        name = "Sumvalo 0.29",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Budgets tab redesigned to the new reference — Budgets/Goals type-toggle pill, Chakra weekly cards with donut progress markers, per-budget pace bars with over-paced alert colors, and an envelope-mode card", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.28",
        name = "Sumvalo 0.28",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("About page redesigned to the new reference — glowing logo, version pill, label-value build facts, icon-tile legal rows and a data-stays-footer", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.27",
        name = "Sumvalo 0.27",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Categories redesigned to the new reference — search, a Spending/Income toggle with counts, count-pilled section headers with \"+ Add\", and one bordered card per section", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.26",
        name = "Sumvalo 0.26",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Accounts redesigned to the new reference — net-balance hero with Bank/Cash/Cards split, section cards with hairline-separated rows, Overdrawn badge, limit-usage bars and a dashed add-account row", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.25",
        name = "Sumvalo 0.25",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Activity redesigned to the new reference — compact month switcher, tracking-caps totals, 40dp search, All/Expenses/Income/Transfers segmented filter with a more-filters sheet, and inset hairlines between a day's rows", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.24",
        name = "Sumvalo 0.24",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Home redesigned to the new reference — greeting with date block, status chips, weekly spending card, 2×2 quick actions, refreshed cards", ChangeCategory.ADDED),
            ChangeItem("Bottom navigation redesigned — active tab as a pill, icon-only destinations, centered add button; the corner add button is gone", ChangeCategory.IMPROVED),
            ChangeItem("Edit home and Edit navigation tabs keep working exactly as before — sections and tabs stay reorderable and hideable", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.23",
        name = "Sumvalo 0.23",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Onboarding redesigned — all five first-run screens (Welcome, Features, History, Notifications, All Set) rebuilt with the new dark hero design", ChangeCategory.ADDED),
            ChangeItem("Navigation cleanup — removed 'Labels under icons' and 'Floating bar' toggles from Edit navigation tabs; redesigned bar kept intact", ChangeCategory.IMPROVED),
            ChangeItem("Settings customization fixed — section reordering and renaming now persist and appear in the Settings hub; deletion still works", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.22",
        name = "Sumvalo 0.22",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Release pipeline: security-audit.sh now fails loudly when jadx is missing instead of silently skipping the DEX scan (network code, trackers, secrets)", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.21",
        name = "Sumvalo 0.21",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Navigation icons now match the reference — Insights is a line chart, Activity a clipboard with list lines; Home and Settings were already correct", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.20",
        name = "Sumvalo 0.20",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Navigation replaced with the reference implementation — bordered rounded container, 20dp icons over Chakra Petch labels, the selected destination on a filled segment; the old elevated pill bar (shadow, 68dp, accent-tinted indicator) is gone", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.19",
        name = "Sumvalo 0.19",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Home trimmed to the essentials — the dashboard now ends at Budget progress; Recent, Accounts and the deeper analytics sections moved off the dashboard (Activity, Insights and Accounts screens still carry everything)", ChangeCategory.IMPROVED),
            ChangeItem("Bottom navigation redesigned — solid bordered container with the active destination on a filled segment, Chakra Petch labels", ChangeCategory.IMPROVED),
            ChangeItem("Default dark palette retuned from blue-navy to the neutral black direction (backgrounds, cards, hairlines, text greys)", ChangeCategory.IMPROVED),
            ChangeItem("Activity filter chips now select with the app's ink pill instead of a separate blue", ChangeCategory.FIXED),
            ChangeItem("Home date strip is interactive — tap a day or step weeks with the arrows; a Today chip returns to the current date", ChangeCategory.ADDED),
            ChangeItem("Net worth hide/show eye moved beside its title; the 1M/6M/1Y/All selector is one bordered pill", ChangeCategory.IMPROVED),
            ChangeItem("Settings: Tools moved from More into Preferences", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.18",
        name = "Sumvalo 0.18",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Insights redesigned — segmented range selector, month score ring with narrative summary, bordered stat strip, six-month spending chart card", ChangeCategory.ADDED),
            ChangeItem("Insights grouped by severity into bordered containers with divided rows and inline action buttons", ChangeCategory.ADDED),
            ChangeItem("Tap a bar on the six-month chart to inspect that month's spending", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.17",
        name = "Sumvalo 0.17",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings hub redesigned — labeled groups (Money, Preferences, Data & privacy, More), profile tile, search field, 52dp rows", ChangeCategory.ADDED),
            ChangeItem("Chakra Petch typeface bundled for home and settings headings", ChangeCategory.ADDED),
            ChangeItem("Home redesign (v0.16): net worth hero, monthly spending ring, budget progress rings, redesigned quick actions", ChangeCategory.ADDED),
            ChangeItem("Activity (transactions) redesigned — grouped sections, pill month switcher, combined income/spent/net stat row, search field, 52dp rows", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.16",
        name = "Sumvalo 0.16",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Home redesigned: new header with SUMVALO wordmark, greeting and live date strip", ChangeCategory.ADDED),
            ChangeItem("Net worth card rebuilt — 1M/6M/1Y/All ranges, movement since last month, assets vs liabilities split bar", ChangeCategory.ADDED),
            ChangeItem("Monthly spending card with budget ring, spent/budget/remaining and payday footer", ChangeCategory.ADDED),
            ChangeItem("Budget progress rings (new section) and redesigned quick actions, spending pace and upcoming bills", ChangeCategory.ADDED),
            ChangeItem("Edit Home still controls every section — reorder and hide as before, with the new look", ChangeCategory.IMPROVED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.15",
        name = "Sumvalo 0.15",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Fixed database migration v13→14 — missing MIGRATION_13_14 added so upgrades create shared-expense and purchase-memory tables", ChangeCategory.FIXED),
            ChangeItem("Fixed path traversal in attachment restore — saveBytes() now sanitizes file names through the same safe() check as read() and delete()", ChangeCategory.FIXED),
            ChangeItem("Removed duplicate icon import in Routes.kt", ChangeCategory.FIXED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.14",
        name = "Sumvalo 0.14",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: replaced deprecated icon references with AutoMirrored equivalents across nav tabs, home, onboarding, purchase memory and settings screens", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.13",
        name = "Sumvalo 0.13",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: Widgets moved from a standalone hub entry into the Tools section row", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.12",
        name = "Sumvalo 0.12",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: Widgets moved from the main Settings tab into the Tools section", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.11",
        name = "Sumvalo 0.11",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: revamped hub icons — Tools (gear), Loans & EMI (wallet), Budgets & goals (pie chart), Notifications (bell) for a cohesive visual language", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.10",
        name = "Sumvalo 0.10",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: 'Financial tools' section renamed to 'Tools'", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.9",
        name = "Sumvalo 0.9",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: Budgets & goals moved from Financial Tools to the main Settings screen as a standalone entry", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.8",
        name = "Sumvalo 0.8",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Settings: Lending and Loans & EMI moved from Financial Tools to the main Settings screen as standalone entries", ChangeCategory.ADDED),
        ),
    ),
    ReleaseEntry(
        tag = "v0.7",
        name = "Sumvalo 0.7",
        publishedAt = "",
        body = null,
        htmlUrl = "",
    ).copy(
        providedChanges = listOf(
            ChangeItem("Split Any Expense: shared-expense groups with participants, equal-split transactions, and balance tracking", ChangeCategory.ADDED),
            ChangeItem("Purchase Memory: purchase records with encrypted receipt attachments, warranty and return deadlines, and offline search", ChangeCategory.ADDED),
        ),
    ),
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
            ChangeItem("Bill countdown badges on home: urgent/overdue bills show a coloured Due today / Overdue pill", ChangeCategory.ADDED),
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
            ChangeItem("Bill countdown badges on the home screen: urgent and overdue bills show a coloured Due today / Overdue pill next to the card name", ChangeCategory.IMPROVED),
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
