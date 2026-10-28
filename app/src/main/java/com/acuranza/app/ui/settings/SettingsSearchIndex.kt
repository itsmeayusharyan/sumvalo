package com.acuranza.app.ui.settings

import com.acuranza.app.ui.nav.Routes

/**
 * The complete, recursively-registered settings index: every hub destination plus every row on
 * every nested page. MoreScreen's search matches this flat registry — case-insensitive partial
 * matches on title, description and keywords — so settings buried two screens deep are
 * discoverable without duplicating any real setting. Each hit navigates directly to its page;
 * in-page rows land on their host page.
 *
 * @param group the hub group the setting belongs to.
 * @param page the page that hosts the row (the "… → page" part of the navigation path).
 * @param title the row's label as it appears on its page.
 * @param description the row's subtitle.
 * @param keywords extra search terms users might type (synonyms, formats, synonyms of features).
 * @param route the screen to open.
 */
data class SettingsSearchHit(
    val group: String,
    val page: String,
    val title: String,
    val description: String,
    val keywords: String = "",
    val route: String,
)

private const val G_ACCOUNT = "Account"
private const val G_GENERAL = "General"
private const val G_APPEARANCE = "Appearance"
private const val G_PRIVACY = "Privacy & Security"
private const val G_DATA = "Data Management"
private const val G_NOTIFICATIONS = "Notifications"
private const val G_ABOUT = "About"

val SettingsSearchIndex: List<SettingsSearchHit> = listOf(
    // ── Hub destinations ────────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_ACCOUNT, "Profile", "Profile", "Your display name and profile photo", "profile name photo picture avatar", Routes.PROFILE),
    SettingsSearchHit(G_GENERAL, "Widgets", "Widgets", "Preview and add home-screen widgets", "widgets home screen add preview", Routes.WIDGETS_PREVIEW),
    SettingsSearchHit(G_ACCOUNT, "Management", "Management", "Accounts, imports and categories", "accounts cards review", Routes.SETTINGS_MANAGEMENT),
    SettingsSearchHit(G_GENERAL, "Financial tools", "Financial tools", "Recurring, loans, lending, reports, budgets & goals", "sms tracking", Routes.SETTINGS_FINANCIAL),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Appearance", "Theme, colour theme, accent, AMOLED, hide amounts, edit home", "dark light system amoled", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "Privacy & security", "App lock, PINs, encryption and recovery", "biometric decoy screenshots", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_DATA, "Data management", "Data management", "Backups, imports, exports and destructive actions", "backup restore csv erase", Routes.SETTINGS_DATA),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Notifications", "Reminders, digest, widgets and where your budget month starts", "alerts digest widgets", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_ABOUT, "About", "About", "Version, privacy policy and licence", "version github legal", Routes.ABOUT),

    // ── Management (Account) ───────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_ACCOUNT, "Management", "Accounts & cards", "Balances, limits and reconciliation", "", Routes.ACCOUNTS),
    SettingsSearchHit(G_ACCOUNT, "Management", "Review imports", "Confirm imported transactions", "", Routes.REVIEW),
    SettingsSearchHit(G_ACCOUNT, "Management", "Categories", "Add, rename, restyle and delete your categories", "", Routes.CATEGORIES),

    // ── Financial tools (General) ──────────────────────────────────────────────────────────────
    SettingsSearchHit(G_GENERAL, "Financial tools", "SMS transaction tracking", "Bank messages by share or paste, on-device only", "sms bank messages inbox scan notifications consent withdraw", Routes.SMS_TRACKING),
    SettingsSearchHit(G_GENERAL, "Financial tools", "Recurring payments", "Upcoming payments, AutoPay mandates, price changes", "subscriptions autopay", Routes.RECURRING),
    SettingsSearchHit(G_GENERAL, "Financial tools", "Reports", "Monthly and yearly PDF, year in review, calendar", "pdf summary", Routes.REPORTS),

    // ── Financial Management ──────────────────────────────────────────────────────────────
    SettingsSearchHit(G_GENERAL, "Financial Management", "Lending", "Money between you and friends or family", "debt iou borrow", Routes.DEBTS),
    SettingsSearchHit(G_GENERAL, "Financial Management", "Loans & EMI", "What's left, prepayment savings, tax split", "loan emi prepayment", Routes.LOANS),
    SettingsSearchHit(G_GENERAL, "Financial Management", "Budget", "Monthly spending limits and rollover", "envelope rollover", Routes.PLANNING),
    SettingsSearchHit(G_GENERAL, "Financial Management", "Goals", "Savings targets and progress", "savings target", Routes.SAVINGS_GOAL),

    // ── Appearance ─────────────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Theme", "Dark, Light or follow the system", "dark light system mode", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Color theme", "Default, Dynamic (wallpaper), Catppuccin, Cloud/Amber, Ocean, Forest, Rose", "palette dynamic material you wallpaper catppuccin mocha latte", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "AMOLED black", "Pure-black surfaces in dark mode", "amoled black oled battery", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Accent colour", "Blue, teal, violet or rose highlights", "accent tint color", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Default landing tab", "Choose which tab opens when you launch Sumvalo", "landing tab startup launch open home insights activity settings default", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Customize settings", "Order, hide and rename sections on this hub", "customize edit layout reorder rename hide sections hub", Routes.SETTINGS_EDIT),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Haptic feedback", "Master switch, strength and what vibrates", "haptic vibration strength keypad taps feedback feel", Routes.SETTINGS_HAPTICS),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Edit navigation tabs", "Icons, names, order and the floating bar", "navigation tabs bottom bar floating icons labels order hide", Routes.SETTINGS_NAV_TABS),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Hide amounts", "Show ₹ •••• until you tap the eye", "privacy blur amounts", Routes.SETTINGS_APPEARANCE),
    SettingsSearchHit(G_APPEARANCE, "Appearance", "Edit home", "Choose what appears on your dashboard and in what order", "dashboard widgets layout", Routes.EDIT_HOME),

    // ── Privacy & security ─────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "App lock", "Require biometrics or your PIN to open Sumvalo", "biometric fingerprint", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "App PIN", "A numeric PIN that unlocks the app", "", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "Decoy PIN", "A second PIN that opens an empty profile", "decoy hidden profile", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "Block screenshots", "Keep Sumvalo out of screenshots and the recents preview", "", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "Database encryption", "SQLCipher AES-256 with the key in the Android Keystore", "sqlcipher crypto", Routes.SETTINGS_PRIVACY),
    SettingsSearchHit(G_PRIVACY, "Privacy & security", "Recovery options", "How to get back in if biometrics or the PIN fail", "", Routes.SETTINGS_PRIVACY),

    // ── Data management ────────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_DATA, "Data management", "Back up", "Encrypted file protected by a password", "backup export", Routes.SETTINGS_DATA),
    SettingsSearchHit(G_DATA, "Data management", "Automatic backups", "Daily or weekly, to a folder you choose", "schedule backup", Routes.AUTO_BACKUP),
    SettingsSearchHit(G_DATA, "Data management", "Restore from backup", "Replaces everything on this phone", "restore import backup", Routes.SETTINGS_DATA),
    SettingsSearchHit(G_DATA, "Data management", "Import statements", "CSV from net banking to bring in past transactions", "csv import hdfc icici sbi axis kotak", Routes.STATEMENT_IMPORT),
    SettingsSearchHit(G_DATA, "Data management", "Imported transactions", "Transactions confirmed from bank messages — review, search or delete", "sms imported review delete", Routes.SETTINGS_SMS_IMPORTS),
    SettingsSearchHit(G_DATA, "Data management", "Export data", "CSV, Excel, Beancount or Ledger", "export csv excel beancount ledger share file", Routes.SETTINGS_DATA),
    SettingsSearchHit(G_DATA, "Data management", "Load sample data", "Explore Sumvalo with demo accounts and transactions", "demo", Routes.SETTINGS_DATA),
    SettingsSearchHit(G_DATA, "Data management", "Erase all data", "Deletes accounts, transactions and bills", "wipe delete everything destructive", Routes.SETTINGS_DATA),

    // ── Notifications ──────────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Bill reminders", "3 days and 1 day before the due date", "card due alert", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Important insights", "Overspending and unusual spends", "alerts spikes", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Month starts on", "Where your budget month begins", "salary cycle budget start", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Daily digest", "Today's spending, budget left and items to review", "digest summary schedule notification", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Digest time", "When the daily digest arrives", "digest schedule time", Routes.SETTINGS_NOTIFICATIONS),
    SettingsSearchHit(G_NOTIFICATIONS, "Notifications", "Home screen widgets", "Show real amounts on the widget even with app lock on", "widget amounts lock", Routes.SETTINGS_NOTIFICATIONS),

    // ── About ──────────────────────────────────────────────────────────────────────────────────
    SettingsSearchHit(G_ABOUT, "About", "Privacy policy", "What Sumvalo stores, and what it never sends", "legal privacy data", Routes.ABOUT_PRIVACY),
    SettingsSearchHit(G_ABOUT, "About", "Terms of service", "The plain terms of using the app", "legal terms", Routes.ABOUT_TERMS),
    SettingsSearchHit(G_ABOUT, "About", "Licence", "GPL-3.0 · free and open source", "license gpl legal", Routes.ABOUT_GPL),
)

/** Case-insensitive partial match over title, description and keywords, with the query trimmed. */
fun List<SettingsSearchHit>.searchSettings(rawQuery: String): List<SettingsSearchHit> {
    val q = rawQuery.trim()
    if (q.isEmpty()) return emptyList()
    return filter { hit ->
        hit.title.contains(q, ignoreCase = true) ||
            hit.description.contains(q, ignoreCase = true) ||
            hit.keywords.contains(q, ignoreCase = true) ||
            hit.page.contains(q, ignoreCase = true) ||
            hit.group.contains(q, ignoreCase = true)
    }
}
