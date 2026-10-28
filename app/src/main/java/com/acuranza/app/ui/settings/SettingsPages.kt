@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.acuranza.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.designsystem.theme.isSupported
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.Check
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.acuranza.app.data.prefs.ThemeMode
import com.acuranza.app.model.AccentColor
import com.acuranza.app.model.TxnType
import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.security.BackupCrypto
import com.acuranza.app.ui.accounts.ordinal
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.ConfirmDialog
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.common.NO_FILE_PICKER
import com.acuranza.app.ui.common.launchOrFalse
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.ui.text.style.TextOverflow

/**
 * The Settings subpages: the hub in [MoreScreen] opens each of these on its own screen with a back
 * button, so the settings list stays short while every control keeps its existing behaviour.
 */

/** One scrolling page: padding + the standard 20dp gutters, so sections align everywhere. */
@Composable
internal fun SettingsPage(
    title: String,
    subtitle: String,
    nav: NavController,
    snackbar: SnackbarHostState? = null,
    content: @Composable () -> Unit,
) {
    SumvaloScreen(title = title, subtitle = subtitle, onBack = { nav.popBackSafely() }, snackbar = snackbar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ManagementSettingsPage(nav: NavController) {
    SettingsPage("Management", "Accounts, imports and categories", nav) {
        SettingsSection(null) {
            SettingsRow("Accounts & cards", "Balances, limits and reconciliation", Icons.Outlined.AccountBalance) { nav.navigate(Routes.ACCOUNTS) }
            SettingsRow("Review imports", "Confirm imported transactions", Icons.Outlined.Inbox) { nav.navigate(Routes.REVIEW) }
            SettingsRow("Categories", "Add, rename, restyle and delete your categories", Icons.Outlined.Category, divider = false) { nav.navigate(Routes.CATEGORIES) }
        }
    }
}

@Composable
fun FinancialToolsPage(nav: NavController) {
    SettingsPage("Tools", "Everything beyond everyday tracking", nav) {
        SettingsSection(null) {
            SettingsRow("SMS transaction tracking", "Bank messages by share or paste, on-device only", Icons.Outlined.Sms) { nav.navigate(Routes.SMS_TRACKING) }
            SettingsRow("Recurring payments", "Upcoming payments, AutoPay mandates, price changes", Icons.Outlined.Autorenew) { nav.navigate(Routes.RECURRING) }
            SettingsRow("Reports", "Monthly and yearly PDF, year in review, calendar", Icons.Outlined.Assessment) { nav.navigate(Routes.REPORTS) }
            SettingsRow("Widgets", "Preview and add home-screen widgets", Icons.Outlined.Widgets, divider = false) { nav.navigate(Routes.WIDGETS_PREVIEW) }
        }
    }
}

@Composable
fun PrivacySecurityPage(nav: NavController) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    SettingsPage("Privacy & security", "Locks, PINs, encryption and recovery", nav, snackbar) {
        PrivacySecuritySection(onMessage = { show(it) })
    }
}

@Composable
fun NotificationsPage(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val ensurePermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    SettingsPage("Notifications", "Reminders, and where your budget month starts", nav) {
        SettingsSection("Transaction reminder") {
            SettingsToggle("Transaction reminder", "A daily nudge to record your spending", s.txnReminderEnabled, { v ->
                if (v) ensurePermission()
                com.acuranza.app.work.WorkScheduler.setTxnReminder(context, v, s.txnReminderMinuteOfDay)
                scope.launch { c.settings.setTxnReminderEnabled(v) }
            }, Icons.Outlined.EditNote)
            if (s.txnReminderEnabled) {
                SettingsPickerRow(
                    "Remind me",
                    listOf(true, false),
                    s.txnReminderOnlyIfNone,
                    { onlyIfNone -> if (onlyIfNone) "Only if nothing recorded today" else "Every day" },
                    Icons.AutoMirrored.Outlined.Rule,
                ) { onlyIfNone -> scope.launch { c.settings.setTxnReminderOnlyIfNone(onlyIfNone) } }
                ReminderTimeRow(s.txnReminderMinuteOfDay) { minute ->
                    com.acuranza.app.work.WorkScheduler.setTxnReminder(context, true, minute)
                    scope.launch { c.settings.setTxnReminderMinuteOfDay(minute) }
                }
            }
        }
        SettingsSection("Upcoming payments") {
            SettingsToggle("Bill reminders", "3 days and 1 day before the due date", s.notifyBills, { v ->
                if (v) ensurePermission()
                scope.launch { c.settings.setNotifyBills(v) }
            }, Icons.Outlined.Alarm)
            SettingsToggle("Scheduled payments", "Recurring payments on the day they're due", s.notifyRecurring, { v ->
                if (v) ensurePermission()
                scope.launch { c.settings.setNotifyRecurring(v) }
            }, Icons.Outlined.Autorenew)
        }
        SettingsSection("Insights") {
            SettingsToggle("Important insights", "Overspending and unusual spends", s.notifyInsights, { v ->
                if (v) ensurePermission()
                scope.launch { c.settings.setNotifyInsights(v) }
            }, Icons.Outlined.Bolt)
            SettingsPickerRow(
                "Month starts on",
                (1..28).toList(),
                s.monthStartDay,
                { if (it == 1) "1st (calendar month)" else ordinal(it) + " (e.g. salary day)" },
                Icons.Outlined.CalendarMonth,
            ) { d -> scope.launch { c.settings.setMonthStartDay(d) } }
            PaydaySettings(s)
        }
        com.acuranza.app.ui.tools.DigestSection(onMessage = {})
    }
}

/** The reminder time row: opens a Material time picker that follows the device's 12/24-hour clock. */
@Composable
private fun ReminderTimeRow(minuteOfDay: Int, onPick: (Int) -> Unit) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsRow(
        "Remind at",
        icon = Icons.Outlined.Schedule,
        value = java.time.LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
            .let { java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(it) },
        onClick = { open = true },
    )
    if (open) {
        val state = androidx.compose.material3.rememberTimePickerState(
            initialHour = minuteOfDay / 60,
            initialMinute = minuteOfDay % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    open = false
                    onPick(state.hour * 60 + state.minute)
                }) { Text("Save") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { open = false }) { Text("Cancel") } },
            title = { Text("Reminder time") },
            text = { androidx.compose.material3.TimePicker(state = state) },
        )
    }
}

@Composable
fun AppearancePage(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()
    SettingsPage("Appearance", "Theme, accent colour and your dashboard", nav) {
        SettingsSection(null) {
            SettingsPickerRow(
                "Theme",
                ThemeMode.entries,
                s.themeMode,
                { when (it) { ThemeMode.SYSTEM -> "System default"; ThemeMode.DARK -> "Dark"; ThemeMode.LIGHT -> "Light" } },
                Icons.Outlined.Palette,
            ) { mode -> scope.launch { c.settings.setThemeMode(mode) } }
            ColorThemeGallery(s.colorTheme) { theme -> scope.launch { c.settings.setColorTheme(theme) } }
            SettingsToggle(
                "AMOLED black",
                if (s.themeMode == ThemeMode.LIGHT) "Saved — active when dark mode is on"
                else "Pure-black surfaces in dark mode",
                s.amoled && s.themeMode != ThemeMode.LIGHT,
                onChecked = { v -> scope.launch { c.settings.setAmoled(v) } },
                icon = Icons.Outlined.Contrast,
                enabled = s.themeMode != ThemeMode.LIGHT,
            )
            SettingsPickerRow(
                "Accent colour",
                AccentColor.entries,
                s.accentChoice,
                { it.label },
                Icons.Outlined.ColorLens,
            ) { choice -> scope.launch { c.settings.setAccentColor(choice) } }
            SettingsToggle("Hide amounts", "Show ₹ •••• until you tap the eye", s.hideAmounts, { v -> scope.launch { c.settings.setHideAmounts(v) } },
                Icons.Outlined.VisibilityOff)
            SettingsRow("Haptic feedback", "Master switch, strength and what vibrates", Icons.Outlined.Vibration) {
                nav.navigate(Routes.SETTINGS_HAPTICS)
            }
            SettingsRow("Edit navigation tabs", "Icons, names, order and the floating bar", Icons.Outlined.SpaceDashboard, divider = false) {
                nav.navigate(Routes.SETTINGS_NAV_TABS)
            }
            SettingsPickerRow(
                "Default landing tab",
                com.acuranza.app.ui.nav.Tabs,
                com.acuranza.app.ui.nav.Tabs.firstOrNull { it.route == s.landingTab } ?: com.acuranza.app.ui.nav.Tabs.first(),
                { it.label },
                Icons.AutoMirrored.Outlined.Launch,
            ) { tab -> scope.launch { c.settings.setLandingTab(tab.route) } }
            SettingsRow("Edit home", "Choose what appears on your dashboard and in what order", Icons.Outlined.DashboardCustomize, divider = false) {
                nav.navigate(Routes.EDIT_HOME)
            }
            SettingsPickerRow(
                "SMS import threshold",
                SmsConfidence.entries,
                s.smsConfidenceThreshold,
                { it.name },
                Icons.Outlined.FilterList,
            ) { threshold -> scope.launch { c.settings.setSmsConfidenceThreshold(threshold) } }
        }
    }
}

/**
 * Horizontally scrollable palette gallery: each card is a live preview (background, card and
 * accent of the palette in the current mode) with a ring on the selected one. Dynamic is
 * disabled with a one-line explanation below Android 12 — never a dead-looking control.
 */
@Composable
private fun ColorThemeGallery(selected: com.acuranza.app.designsystem.theme.ColorTheme, onPick: (com.acuranza.app.designsystem.theme.ColorTheme) -> Unit) {
    val sdk = android.os.Build.VERSION.SDK_INT
    Column {
        Text("Color theme", style = MaterialTheme.typography.titleSmall, color = SumvaloTheme.colors.ink)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            items(com.acuranza.app.designsystem.theme.ColorTheme.entries) { theme ->
                val supported = theme.isSupported(sdk)
                val chosen = theme == selected
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    modifier = Modifier.width(84.dp),
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                            .background(previewBackground(theme))
                            .border(
                                width = if (chosen) 2.dp else 1.dp,
                                color = if (chosen) SumvaloTheme.colors.accent else SumvaloTheme.colors.hairline,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            )
                            .alpha(if (supported) 1f else 0.38f)
                            .clickable(enabled = supported) { onPick(theme) },
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)) {
                            previewSwatches(theme).forEach { color ->
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                )
                            }
                        }
                        if (chosen) {
                            Box(
                                Modifier
                                    .align(androidx.compose.ui.Alignment.TopEnd)
                                    .padding(5.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(SumvaloTheme.colors.accent),
                                contentAlignment = androidx.compose.ui.Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = SumvaloTheme.colors.onInk,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        theme.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (supported) SumvaloTheme.colors.subtleText else SumvaloTheme.colors.tertiaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (!com.acuranza.app.designsystem.theme.ColorTheme.DYNAMIC.isSupported(sdk)) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Dynamic needs Android 12+ to read your wallpaper colours — other themes work everywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.tertiaryText,
            )
        }
    }
}

/** Three representative swatches per palette, honouring the current dark/light mode. */
@Composable
private fun previewSwatches(theme: com.acuranza.app.designsystem.theme.ColorTheme): List<androidx.compose.ui.graphics.Color> {
    val dark = SumvaloTheme.colors.isDark
    return when (theme) {
        com.acuranza.app.designsystem.theme.ColorTheme.DYNAMIC ->
            listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary)
        else -> {
            val resolved = com.acuranza.app.designsystem.theme.builtInPalette(theme, dark)
            listOf(resolved.background, resolved.tokens.cardSurface, resolved.tokens.accent)
        }
    }
}

@Composable
private fun previewBackground(theme: com.acuranza.app.designsystem.theme.ColorTheme): androidx.compose.ui.graphics.Color {
    val dark = SumvaloTheme.colors.isDark
    return if (theme == com.acuranza.app.designsystem.theme.ColorTheme.DYNAMIC) {
        MaterialTheme.colorScheme.background
    } else {
        com.acuranza.app.designsystem.theme.builtInPalette(theme, dark).background
    }
}

/**
 * Backups, imports, sample data and the destructive actions. The backup machinery (activity
 * launchers, password dialogs, busy overlay) moved here unchanged from the old flat Settings.
 */
@Composable
fun DataManagementPage(nav: NavController) {
    val c = LocalContainer.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf<String?>(null) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmRemoveSample by remember { mutableStateOf(false) }
    var backupUri by remember { mutableStateOf<Uri?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var sampleLoaded by remember { mutableStateOf(false) }

    fun show(msg: String) = scope.launch { snackbar.showSnackbar(msg) }
    LaunchedEffect(Unit) { sampleLoaded = runCatching { c.demoData.isLoaded() }.getOrDefault(false) }
    var smsImportCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        smsImportCount = runCatching {
            c.smsImports.totalImports() - c.smsImports.pendingCount()
        }.getOrDefault(0)
    }
    val backupFlow = remember { com.acuranza.app.security.BackupFlow(context.applicationContext as com.acuranza.app.SumvaloApp) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) backupUri = uri
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) restoreUri = uri
    }

    SettingsPage("Data management", "Backups, imports and destructive actions", nav, snackbar) {
        busy?.let { msg ->
            com.acuranza.app.designsystem.components.SumvaloCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(msg)
                }
            }
        }
        SettingsSection(null) {
            SettingsRow("Back up", "Encrypted file protected by a password", Icons.Outlined.Backup, enabled = busy == null) {
                if (!createBackup.launchOrFalse("acuranza-backup-${LocalDate.now()}.acuranza")) show(NO_FILE_PICKER)
            }
            SettingsRow("Automatic backups", "Daily or weekly, to a folder you choose", Icons.Outlined.Sync) { nav.navigate(Routes.AUTO_BACKUP) }
            SettingsRow("Restore from backup", "Replaces everything on this phone", Icons.Outlined.Restore, enabled = busy == null) {
                if (!openBackup.launchOrFalse(arrayOf("*/*"))) show(NO_FILE_PICKER)
            }
            SettingsRow("Import statements", "CSV from net banking to bring in past transactions", Icons.Outlined.UploadFile) {
                nav.navigate(Routes.STATEMENT_IMPORT)
            }
            SettingsRow(
                "Imported transactions",
                if (smsImportCount > 0) "$smsImportCount from bank messages — review, search or delete" else "Transactions confirmed from bank messages appear here",
                Icons.Outlined.Sms,
            ) { nav.navigate(Routes.SETTINGS_SMS_IMPORTS) }
            com.acuranza.app.ui.tools.ExportSection(onMessage = ::show)
            if (sampleLoaded) {
                SettingsRow("Remove sample data", "Deletes only the demo accounts, transactions and investments", Icons.Outlined.Science, enabled = busy == null) {
                    confirmRemoveSample = true
                }
            } else {
                SettingsRow("Load sample data", "Explore Sumvalo with demo accounts and transactions", Icons.Outlined.Science, enabled = busy == null) {
                    busy = "Loading sample data…"
                    scope.launch {
                        val loaded = runCatching { c.demoData.load() }.getOrDefault(false)
                        if (loaded) c.netWorth.snapshot()
                        sampleLoaded = c.demoData.isLoaded()
                        busy = null
                        show(if (loaded) "Sample data loaded" else "Sample data is already loaded")
                    }
                }
            }
            SettingsRow(
                "Erase all data",
                "Deletes accounts, transactions and bills",
                Icons.Outlined.DeleteForever,
                tone = RowTone.DESTRUCTIVE,
                divider = false,
            ) { confirmErase = true }
        }
    }

    if (confirmErase) {
        ConfirmDialog(
            "Erase all data?",
            "This deletes every account, transaction, bill, budget and goal on this phone. Make a backup first if unsure.",
            "Erase",
            destructive = true,
            onConfirm = {
                confirmErase = false
                scope.launch {
                    c.demoData.clear()
                    runCatching { c.attachmentStore.deleteAll() }
                    sampleLoaded = false
                    show("All data erased")
                }
            },
            onDismiss = { confirmErase = false },
        )
    }
    if (confirmRemoveSample) {
        ConfirmDialog(
            "Remove sample data?",
            "This deletes the demo accounts, transactions and investments. Your own data is untouched.",
            "Remove",
            onConfirm = {
                confirmRemoveSample = false
                busy = "Removing sample data…"
                scope.launch {
                    runCatching { c.demoData.removeSample() }
                    sampleLoaded = c.demoData.isLoaded()
                    busy = null
                    show("Sample data removed")
                }
            },
            onDismiss = { confirmRemoveSample = false },
        )
    }
    backupUri?.let { uri ->
        BackupPasswordDialog("Protect your backup", "Choose a password. You'll need it to restore — it can't be recovered.", confirm = true,
            onDismiss = { backupUri = null }) { pw ->
            backupUri = null
            busy = "Creating backup…"
            scope.launch {
                val r = runCatching { backupFlow.export(uri, pw.toCharArray()) }
                busy = null
                show(r.map { "Backed up ${it.transactions} transactions and ${it.accounts} accounts" }.getOrElse { "Backup failed: ${it.message}" })
            }
        }
    }
    restoreUri?.let { uri ->
        BackupPasswordDialog("Restore backup", "Enter the backup's password. Current data on this phone will be replaced.", confirm = false,
            onDismiss = { restoreUri = null }) { pw ->
            restoreUri = null
            busy = "Restoring…"
            scope.launch {
                val r = runCatching { backupFlow.restore(uri, pw.toCharArray()) }
                busy = null
                sampleLoaded = c.demoData.isLoaded()
                show(
                    r.map { "Restored ${it.transactions} transactions and ${it.accounts} accounts" }.getOrElse {
                        if (it is BackupCrypto.WrongPasswordException) "Wrong password or damaged file" else "Restore failed: ${it.message}"
                    },
                )
            }
        }
    }
}

private const val NEW_BACKUP_MIN_PASSWORD = 8

@Composable
private fun BackupPasswordDialog(title: String, message: String, confirm: Boolean, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var pw2 by remember { mutableStateOf("") }
    val minLength = if (confirm) NEW_BACKUP_MIN_PASSWORD else 1
    val ok = pw.length >= minLength && (!confirm || pw == pw2)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    pw, { pw = it }, label = { Text(if (confirm) "Password ($NEW_BACKUP_MIN_PASSWORD+ characters)" else "Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                if (confirm) {
                    OutlinedTextField(
                        pw2, { pw2 = it }, label = { Text("Repeat password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = pw2.isNotEmpty() && pw != pw2,
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onDone(pw) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
