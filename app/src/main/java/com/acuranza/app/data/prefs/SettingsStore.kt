package com.acuranza.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.acuranza.app.finance.IndianHolidays
import com.acuranza.app.finance.PayCycle
import com.acuranza.app.finance.PayWeekend
import com.acuranza.app.sms.SmsConfidence
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// A corrupt settings file is replaced with defaults instead of crashing every launch.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kosh_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val onboardingDone: Boolean = false,
    /** Customized Settings hub layout; null = the default. Encoded by SettingsLayout. */
    val settingsLayout: String? = null,
    /** Bottom-bar customization: order+hidden, label overrides, icon overrides, labels on/off, floating on/off. Defaults = today's look. */
    val navTabLayout: String? = null,
    val navTabLabels: String? = null,
    val navTabIcons: String? = null,
    val navShowLabels: Boolean = true,
    val navFloatingBar: Boolean = true,
    /** Transaction reminder: a daily nudge to record spending. Off by default. */
    val txnReminderEnabled: Boolean = false,
    /** Reminder type: only when nothing was recorded today (true) or unconditionally (false). */
    val txnReminderOnlyIfNone: Boolean = true,
    /** Minutes from midnight the reminder fires; coerced into 0..1439 on read. */
    val txnReminderMinuteOfDay: Int = 21 * 60,
    /** Reminders for scheduled payments (recurring rules due today). Inherits the insights choice for existing users. */
    val notifyRecurring: Boolean = true,
    /** The main tab a plain launch should open; null = the default (Home). Validated against the tab list on read. */
    val landingTab: String? = null,
    val biometricLockEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val colorTheme: com.acuranza.app.designsystem.theme.ColorTheme =
        com.acuranza.app.designsystem.theme.ColorTheme.DEFAULT,
    val amoled: Boolean = false,
    /** When the user dismissed the Home notice about SMS going to ignored accounts; only newer ones show. */
    val ignoredNoticeAt: Long = 0L,
    val monthStartDay: Int = 1,
    /** Salary comes on the working day before a weekend or holiday payday (see [payCycle]). */
    val paydayToWorkingDay: Boolean = true,
    val payWeekend: PayWeekend = PayWeekend.SAT_SUN,
    /** Count India's national holidays as non-paydays. */
    val nationalHolidays: Boolean = true,
    /** State or company holidays the user added. */
    val customHolidays: Set<LocalDate> = emptySet(),
    /** Days salary actually arrived, learned from salary credits (see `repo.PaydayLearner`). */
    val salaryDays: Set<LocalDate> = emptySet(),
    val notifyBills: Boolean = true,
    val notifyInsights: Boolean = true,
    val hideAmounts: Boolean = false,
    val dismissedInsightIds: Set<String> = emptySet(),
    /** In-app notification-center ids already read. */
    val readNotificationIds: Set<String> = emptySet(),
    /** Saved dashboard layout (see HomeLayout); null = the stock layout. */
    val homeLayout: String? = null,
    /** The accent hue the whole app uses; the brand blue by default. */
    val accentChoice: com.acuranza.app.model.AccentColor = com.acuranza.app.model.AccentColor.BLUE,
    /** User's explicit choice for blocking screenshots and the recents preview; null follows [blockScreenshots]. */
    val blockScreenshotsChoice: Boolean? = null,
    /** The user explicitly agreed to SMS transaction tracking; nothing is processed before this. */
    val smsConsent: Boolean = false,
    /** Full variant: automatic inbox scanning toggle (consent + READ_SMS grant required). */
    val smsAutoScan: Boolean = false,
    /** Full variant: when the last inbox scan finished (any kind); 0 = never. */
    val smsLastScanAt: Long = 0L,
    /** Full variant: last-scan outcome ("ok", "no permission", "consent withdrawn"…). */
    val smsLastScanStatus: String = "",
    /** Full variant: set once the first reviewed inbox import has been saved. */
    val smsFirstImportDone: Boolean = false,
    /** Full variant: automatic capture from the SMS app's notifications (consent + system grant). */
    val smsAutoCapture: Boolean = false,
    /** Lowest confidence the app auto-imports without review; MEDIUM by default. */
    val smsConfidenceThreshold: SmsConfidence = SmsConfidence.MEDIUM,
    /** App-generated haptic feedback; the system's own touch-feedback setting rules underneath. */
    val hapticsEnabled: Boolean = true,
    /** Haptic categories, all gated by the master switch; individual choices survive it being off. */
    val hapticKeypad: Boolean = true,
    val hapticActions: Boolean = true,
    val hapticTabsAndButtons: Boolean = true,
    /** Feedback strength: LIGHT / STANDARD / STRONG; unknown stored values coerce to STANDARD. */
    val hapticIntensity: String = "STANDARD",
    /** The user's display name for the profile card and the home greeting; null = not configured. */
    val profileName: String? = null,
    /** Absolute path of the profile photo inside app-private storage; null = no photo (initials fallback). */
    val profileAvatar: String? = null,
) {
    /** When salary arrives, which sets where every budget period starts. */
    val payCycle: PayCycle by lazy {
        PayCycle(
            day = monthStartDay,
            toWorkingDay = paydayToWorkingDay,
            weekend = payWeekend,
            holidays = customHolidays + (if (nationalHolidays) IndianHolidays.dates else emptySet()),
            paid = salaryDays,
        )
    }

    /** Names for the holidays in [payCycle], for explaining a moved payday. */
    val holidayNames: Map<LocalDate, String>
        get() = (if (nationalHolidays) IndianHolidays.named else emptyMap()) + customHolidays.associateWith { "your holiday" }

    val currencySymbol: String get() = "₹"

    /** Screenshots and the recents preview are blocked by default while app lock or privacy mode is on. */
    val blockScreenshots: Boolean get() = blockScreenshotsChoice ?: (biometricLockEnabled || hideAmounts)
}

/** App preferences backed by DataStore. */
class SettingsStore(
    private val context: Context,
    /** The decoy profile passes its own store so it never reads or changes the real settings. */
    private val store: DataStore<Preferences> = context.dataStore,
) {

    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val settingsLayout = stringPreferencesKey("settings_layout")
        val navTabLayout = stringPreferencesKey("nav_tab_layout")
        val navTabLabels = stringPreferencesKey("nav_tab_labels")
        val navTabIcons = stringPreferencesKey("nav_tab_icons")
        val navShowLabels = booleanPreferencesKey("nav_show_labels")
        val navFloatingBar = booleanPreferencesKey("nav_floating_bar")
        val txnReminderEnabled = booleanPreferencesKey("txn_reminder_enabled")
        val txnReminderOnlyIfNone = booleanPreferencesKey("txn_reminder_only_if_none")
        val txnReminderMinuteOfDay = intPreferencesKey("txn_reminder_minute_of_day")
        val notifyRecurring = booleanPreferencesKey("notify_recurring")
        val landingTab = stringPreferencesKey("landing_tab")
        val biometricLock = booleanPreferencesKey("biometric_lock")
        val themeMode = stringPreferencesKey("theme_mode")
        val colorTheme = stringPreferencesKey("color_theme")
        val amoled = booleanPreferencesKey("amoled_black")
        val ignoredNoticeAt = longPreferencesKey("ignored_notice_at")
        // Left by the removed Gmail import; cleared on start.
        val legacyGmail = listOf(
            stringPreferencesKey("gmail_email"), longPreferencesKey("gmail_last_sync"), booleanPreferencesKey("gmail_reconnect"),
            stringSetPreferencesKey("processed_email_ids"), stringPreferencesKey("gmail_last_summary"),
        )
        val monthStartDay = intPreferencesKey("month_start_day")
        val paydayToWorkingDay = booleanPreferencesKey("payday_to_working_day")
        val payWeekend = stringPreferencesKey("pay_weekend")
        val nationalHolidays = booleanPreferencesKey("national_holidays")
        val customHolidays = stringSetPreferencesKey("custom_holidays")
        val salaryDays = stringSetPreferencesKey("salary_days")
        val notifyBills = booleanPreferencesKey("notify_bills")
        val notifyInsights = booleanPreferencesKey("notify_insights")
        val hideAmounts = booleanPreferencesKey("hide_amounts")
        val dismissedInsights = stringSetPreferencesKey("dismissed_insights")
        val readNotifications = stringSetPreferencesKey("read_notifications")
        val homeLayout = stringPreferencesKey("home_layout")
        val accentChoice = stringPreferencesKey("accent_choice")
        val notifiedInsights = stringSetPreferencesKey("notified_insights")
        val notifiedBills = stringSetPreferencesKey("notified_bills")
        val notifiedTxnReminders = stringSetPreferencesKey("notified_txn_reminders")
        val repairVersion = intPreferencesKey("repair_version")
        val blockScreenshots = booleanPreferencesKey("block_screenshots")
        val smsConsent = booleanPreferencesKey("sms_consent")
        val smsAutoScan = booleanPreferencesKey("sms_auto_scan")
        val smsLastScanAt = longPreferencesKey("sms_last_scan_at")
        val smsLastScanStatus = stringPreferencesKey("sms_last_scan_status")
        val smsFirstImportDone = booleanPreferencesKey("sms_first_import_done")
        val smsAutoCapture = booleanPreferencesKey("sms_auto_capture")
        /** Lowest confidence for auto-import without review; MEDIUM by default. */
        val smsConfidenceThreshold = stringPreferencesKey("sms_confidence_threshold")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val hapticKeypad = booleanPreferencesKey("haptic_keypad")
        val hapticActions = booleanPreferencesKey("haptic_actions")
        val hapticTabsAndButtons = booleanPreferencesKey("haptic_tabs_buttons")
        val hapticIntensity = stringPreferencesKey("haptic_intensity")
        val profileName = stringPreferencesKey("profile_name")
        val profileAvatar = stringPreferencesKey("profile_avatar")
    }

    val settings: Flow<Settings> = store.data.catch { e ->
        // An unreadable file reads as defaults rather than crashing the collectors.
        if (e is java.io.IOException) emit(emptyPreferences()) else throw e
    }.map { p ->
        Settings(
            onboardingDone = p[Keys.onboardingDone] ?: false,
            settingsLayout = p[Keys.settingsLayout],
            navTabLayout = p[Keys.navTabLayout],
            navTabLabels = p[Keys.navTabLabels],
            navTabIcons = p[Keys.navTabIcons],
            navShowLabels = p[Keys.navShowLabels] ?: true,
            navFloatingBar = p[Keys.navFloatingBar] ?: true,
            txnReminderEnabled = p[Keys.txnReminderEnabled] ?: false,
            txnReminderOnlyIfNone = p[Keys.txnReminderOnlyIfNone] ?: true,
            txnReminderMinuteOfDay = (p[Keys.txnReminderMinuteOfDay] ?: 21 * 60).coerceIn(0, 24 * 60 - 1),
            // First read after update: keep the user's current behaviour (recurring reminders
            // used to ride on the insights switch) instead of surprising anyone with new posts.
            notifyRecurring = p[Keys.notifyRecurring] ?: (p[Keys.notifyInsights] ?: true),
            landingTab = p[Keys.landingTab]?.takeIf { it in com.acuranza.app.ui.nav.TabRoutes },
            biometricLockEnabled = p[Keys.biometricLock] ?: false,
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            colorTheme = p[Keys.colorTheme]?.let {
                runCatching { com.acuranza.app.designsystem.theme.ColorTheme.valueOf(it) }.getOrNull()
            } ?: com.acuranza.app.designsystem.theme.ColorTheme.DEFAULT,
            amoled = p[Keys.amoled] ?: false,
            ignoredNoticeAt = p[Keys.ignoredNoticeAt] ?: 0L,
            monthStartDay = p[Keys.monthStartDay] ?: 1,
            paydayToWorkingDay = p[Keys.paydayToWorkingDay] ?: true,
            payWeekend = p[Keys.payWeekend]?.let { runCatching { PayWeekend.valueOf(it) }.getOrNull() } ?: PayWeekend.SAT_SUN,
            nationalHolidays = p[Keys.nationalHolidays] ?: true,
            customHolidays = dates(p[Keys.customHolidays]),
            salaryDays = dates(p[Keys.salaryDays]),
            notifyBills = p[Keys.notifyBills] ?: true,
            notifyInsights = p[Keys.notifyInsights] ?: true,
            hideAmounts = p[Keys.hideAmounts] ?: false,
            dismissedInsightIds = p[Keys.dismissedInsights] ?: emptySet(),
            readNotificationIds = p[Keys.readNotifications] ?: emptySet(),
            homeLayout = p[Keys.homeLayout],
            accentChoice = p[Keys.accentChoice]?.let { raw ->
                com.acuranza.app.model.AccentColor.entries.firstOrNull { it.name == raw }
            } ?: com.acuranza.app.model.AccentColor.BLUE,
            blockScreenshotsChoice = p[Keys.blockScreenshots],
            smsConsent = p[Keys.smsConsent] ?: false,
            smsAutoScan = p[Keys.smsAutoScan] ?: false,
            smsLastScanAt = p[Keys.smsLastScanAt] ?: 0L,
            smsLastScanStatus = p[Keys.smsLastScanStatus] ?: "",
            smsFirstImportDone = p[Keys.smsFirstImportDone] ?: false,
            smsAutoCapture = p[Keys.smsAutoCapture] ?: false,
            /** Lowest confidence the app auto-imports without review; MEDIUM by default. */
            smsConfidenceThreshold =
                p[Keys.smsConfidenceThreshold]?.let { runCatching { SmsConfidence.valueOf(it) }.getOrNull() } ?: SmsConfidence.MEDIUM,
            hapticsEnabled = p[Keys.hapticsEnabled] ?: true,
            hapticKeypad = p[Keys.hapticKeypad] ?: true,
            hapticActions = p[Keys.hapticActions] ?: true,
            hapticTabsAndButtons = p[Keys.hapticTabsAndButtons] ?: true,
            hapticIntensity = (p[Keys.hapticIntensity] ?: "STANDARD").uppercase().takeIf { it in setOf("LIGHT", "STANDARD", "STRONG") } ?: "STANDARD",
            profileName = p[Keys.profileName],
            profileAvatar = p[Keys.profileAvatar],
        )
    }
        // Writes to keys outside Settings (claimed notifications, repair version) mustn't re-run every screen.
        .distinctUntilChanged()

    suspend fun current(): Settings = settings.first()

    suspend fun setOnboardingDone(done: Boolean = true) = edit { it[Keys.onboardingDone] = done }
    suspend fun setLandingTab(route: String?) = edit { if (route == null) it.remove(Keys.landingTab) else it[Keys.landingTab] = route }
    suspend fun setSettingsLayout(encoded: String?) = edit { if (encoded == null) it.remove(Keys.settingsLayout) else it[Keys.settingsLayout] = encoded }
    suspend fun setNavTabLayout(encoded: String?) = edit { if (encoded == null) it.remove(Keys.navTabLayout) else it[Keys.navTabLayout] = encoded }
    suspend fun setNavTabLabels(encoded: String?) = edit { if (encoded == null) it.remove(Keys.navTabLabels) else it[Keys.navTabLabels] = encoded }
    suspend fun setNavTabIcons(encoded: String?) = edit { if (encoded == null) it.remove(Keys.navTabIcons) else it[Keys.navTabIcons] = encoded }
    suspend fun setNavShowLabels(on: Boolean) = edit { it[Keys.navShowLabels] = on }
    suspend fun setNavFloatingBar(on: Boolean) = edit { it[Keys.navFloatingBar] = on }
    suspend fun setTxnReminderEnabled(on: Boolean) = edit { it[Keys.txnReminderEnabled] = on }
    suspend fun setTxnReminderOnlyIfNone(onlyIfNone: Boolean) = edit { it[Keys.txnReminderOnlyIfNone] = onlyIfNone }
    suspend fun setTxnReminderMinuteOfDay(minute: Int) = edit { it[Keys.txnReminderMinuteOfDay] = minute.coerceIn(0, 24 * 60 - 1) }
    suspend fun setNotifyRecurring(on: Boolean) = edit { it[Keys.notifyRecurring] = on }
    suspend fun setBiometricLock(enabled: Boolean) = edit { it[Keys.biometricLock] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.themeMode] = mode.name }

    suspend fun setColorTheme(theme: com.acuranza.app.designsystem.theme.ColorTheme) =
        edit { it[Keys.colorTheme] = theme.name }

    suspend fun setAmoled(on: Boolean) = edit { it[Keys.amoled] = on }
    suspend fun dismissIgnoredNotice(at: Long = System.currentTimeMillis()) = edit { it[Keys.ignoredNoticeAt] = at }
    suspend fun setMonthStartDay(day: Int) = edit { it[Keys.monthStartDay] = day.coerceIn(1, 28) }
    suspend fun setPaydayToWorkingDay(on: Boolean) = edit { it[Keys.paydayToWorkingDay] = on }
    suspend fun setPayWeekend(weekend: PayWeekend) = edit { it[Keys.payWeekend] = weekend.name }
    suspend fun setNationalHolidays(on: Boolean) = edit { it[Keys.nationalHolidays] = on }
    suspend fun setCustomHolidays(days: Set<LocalDate>) = edit { it[Keys.customHolidays] = days.map(LocalDate::toString).toSet() }
    suspend fun setSalaryDays(days: Set<LocalDate>) = edit { it[Keys.salaryDays] = days.map(LocalDate::toString).toSet() }

    private fun dates(raw: Set<String>?): Set<LocalDate> =
        raw.orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    suspend fun setNotifyBills(enabled: Boolean) = edit { it[Keys.notifyBills] = enabled }
    suspend fun setNotifyInsights(enabled: Boolean) = edit { it[Keys.notifyInsights] = enabled }
    suspend fun setHideAmounts(hidden: Boolean) = edit { it[Keys.hideAmounts] = hidden }
    suspend fun setBlockScreenshots(blocked: Boolean) = edit { it[Keys.blockScreenshots] = blocked }

    // ---- SMS transaction tracking (Settings → Tools) ----
    /** Records explicit consent; nothing is processed — shared, pasted or otherwise — before this. */
    suspend fun setSmsConsent(granted: Boolean) = edit { it[Keys.smsConsent] = granted }

    /** Withdraws consent and switches every capture off. Deleting the feature's data is a
     *  separate, explicit step performed by the withdrawal flow in the UI. */
    suspend fun revokeSmsConsent() = edit {
        it[Keys.smsFirstImportDone] = false
        it[Keys.smsLastScanAt] = 0L
        it[Keys.smsConsent] = false
        it[Keys.smsAutoScan] = false
        it[Keys.smsAutoCapture] = false
    }

    // Full variant only: inbox scanning bookkeeping.
    suspend fun setSmsAutoScan(enabled: Boolean) = edit { it[Keys.smsAutoScan] = enabled }
    suspend fun setSmsAutoCapture(enabled: Boolean) = edit { it[Keys.smsAutoCapture] = enabled }

    /** Lowest confidence the app auto-imports without review; below this goes to Review. */
    suspend fun setSmsConfidenceThreshold(threshold: SmsConfidence) = edit { it[Keys.smsConfidenceThreshold] = threshold.name }

    /** Haptic feedback preference (Appearance); system settings still gate the actual vibration. */
    suspend fun setHapticsEnabled(on: Boolean) = edit { it[Keys.hapticsEnabled] = on }
    suspend fun setHapticKeypad(on: Boolean) = edit { it[Keys.hapticKeypad] = on }
    suspend fun setHapticActions(on: Boolean) = edit { it[Keys.hapticActions] = on }
    suspend fun setHapticTabsAndButtons(on: Boolean) = edit { it[Keys.hapticTabsAndButtons] = on }
    suspend fun setHapticIntensity(raw: String) = edit { it[Keys.hapticIntensity] = raw.uppercase() }

    /** Profile: display name (null clears it); validated by the caller. */
    suspend fun setProfileName(name: String?) = edit {
        if (name == null) it.remove(Keys.profileName) else it[Keys.profileName] = name
    }

    /** Profile: photo path inside app-private storage (null removes the photo). */
    suspend fun setProfileAvatar(path: String?) = edit {
        if (path == null) it.remove(Keys.profileAvatar) else it[Keys.profileAvatar] = path
    }
    suspend fun setSmsFirstImportDone() = edit { it[Keys.smsFirstImportDone] = true }
    suspend fun recordSmsScan(status: String, skipped: Int = 0) = edit {
        it[Keys.smsLastScanAt] = System.currentTimeMillis()
        it[Keys.smsLastScanStatus] = if (skipped > 0) "$status ($skipped skipped)" else status
    }

    /** Clears preferences left by the removed Gmail import. */
    suspend fun clearLegacyGmail() = edit { prefs -> Keys.legacyGmail.forEach { prefs.remove(it) } }

    /** Version of the last one-time data repair applied after an update (see `repo.DataRepairs`). */
    suspend fun repairVersion(): Int = runCatching { store.data.first()[Keys.repairVersion] }.getOrNull() ?: 0

    suspend fun setRepairVersion(version: Int) = edit { it[Keys.repairVersion] = version }

    suspend fun dismissInsight(id: String) = edit {
        val current = (it[Keys.dismissedInsights] ?: emptySet()).toList()
        it[Keys.dismissedInsights] = (current + id).distinct().takeLast(MAX_IDS).toSet()
    }

    suspend fun clearDismissedInsights() = edit { it.remove(Keys.dismissedInsights) }

    /** Saves the dashboard layout; see HomeLayout for the encoding. */
    suspend fun setHomeLayout(raw: String) = edit { it[Keys.homeLayout] = raw }

    /** Returns the dashboard to the stock layout. */
    suspend fun resetHomeLayout() = edit { it.remove(Keys.homeLayout) }

    /** Persists the app-wide accent; the theme recomposes around it on the next emission. */
    suspend fun setAccentColor(choice: com.acuranza.app.model.AccentColor) = edit { it[Keys.accentChoice] = choice.name }

    /** Marks in-app notification-center items read; old ids fall off the bounded set. */
    suspend fun markNotificationsRead(ids: Collection<String>) = edit {
        val current = (it[Keys.readNotifications] ?: emptySet()).toList()
        it[Keys.readNotifications] = (current + ids).distinct().takeLast(MAX_IDS).toSet()
    }


    /** Returns the ids from [ids] not yet notified, and records them as notified. */
    suspend fun claimNewInsightNotifications(ids: Collection<String>): Set<String> = claim(Keys.notifiedInsights, ids)

    suspend fun claimNewBillNotifications(ids: Collection<String>): Set<String> = claim(Keys.notifiedBills, ids)

    /** Once-per-day guard for the transaction reminder: the same calendar day can only post once. */
    suspend fun claimTxnReminderDay(day: Long): Boolean = claim(Keys.notifiedTxnReminders, listOf("txnremind-$day")).isNotEmpty()

    private suspend fun claim(key: Preferences.Key<Set<String>>, ids: Collection<String>): Set<String> {
        var fresh: Set<String> = emptySet()
        edit {
            val current = it[key] ?: emptySet()
            fresh = ids.filterNot { id -> id in current }.toSet()
            it[key] = (current.toList() + fresh).distinct().takeLast(MAX_IDS).toSet()
        }
        return fresh
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    companion object {
        const val MAX_IDS = 500
    }
}
