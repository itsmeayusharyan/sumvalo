package com.acuranza.app.ui.tools

import com.acuranza.app.ui.common.NO_FILE_PICKER
import com.acuranza.app.ui.common.launchOrFalse
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.di.AppContainer
import com.acuranza.app.export.ExportData
import com.acuranza.app.export.ExportFormat
import com.acuranza.app.export.Exporters
import com.acuranza.app.tools.DigestSchedule
import com.acuranza.app.security.DecoySession
import com.acuranza.app.tools.ToolsPrefs
import com.acuranza.app.ui.settings.DECOY_PLACEHOLDER
import com.acuranza.app.tools.ToolsSettings
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import com.acuranza.app.ui.settings.SettingsToggle
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Date ranges offered by the export dialog. */
enum class ExportRange(val label: String) { THIS_PERIOD("This month"), LAST_PERIOD("Last month"), LAST_12("Last 12 months"), ALL("All time") }

/**
 * Export settings, now hosted on the Data Management page: CSV, Excel, Beancount and Ledger
 * exports with range choice, file handling and share behavior unchanged.
 */
@Composable
fun ExportSection(onMessage: (String) -> Unit) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ToolsSettings.get(context) }
    val realPrefs by remember { store.prefs }.collectAsStateWithLifecycle(initialValue = ToolsPrefs())

    // The decoy profile shows ordinary-looking settings but never changes the real ones.
    val decoy = DecoySession.isActive
    var decoyPrefs by remember { mutableStateOf(ToolsPrefs(pinHash = DECOY_PLACEHOLDER)) }
    val prefs = if (decoy) decoyPrefs else realPrefs

    var exportOpen by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<Pair<ExportFormat, ExportRange>?>(null) }

    fun writeExport(uri: Uri) {
        val (format, range) = pendingExport ?: return
        scope.launch {
            val r = runCatching {
                withContext(Dispatchers.IO) {
                    val data = exportData(c, range, s.payCycle)
                    val text = Exporters.export(format, data)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                        ?: error("Couldn't open the file")
                    Exporters.rows(data).size
                }
            }
            onMessage(r.map { "Exported $it transactions" }.getOrElse { "Export failed: ${it.message}" })
        }
    }

    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) writeExport(uri) }
    val createText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri -> if (uri != null) writeExport(uri) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun saveDigest(enabled: Boolean, minutes: Int = prefs.digestMinutes) {
        if (decoy) {
            decoyPrefs = decoyPrefs.copy(digestEnabled = enabled, digestMinutes = minutes); return
        }
        scope.launch {
            store.setDigestEnabled(enabled)
            store.setDigestMinutes(minutes)
            DigestSchedule.sync(context)
        }
    }

    SettingsRow("Export data", "CSV, Excel, Beancount or Ledger", Icons.Outlined.FileDownload) { exportOpen = true }

    if (exportOpen) {
        ExportDialog(
            onDismiss = { exportOpen = false },
            onExport = { format, range ->
                exportOpen = false
                pendingExport = format to range
                val (from, to) = rangeDates(range, s.payCycle, null)
                val name = Exporters.fileName(format, from ?: LocalDate.of(2000, 1, 1), to).let {
                    if (range == ExportRange.ALL) "acuranza-all-${LocalDate.now()}.${format.extension}" else it
                }
                val opened = if (format.mimeType == "text/csv") createCsv.launchOrFalse(name) else createText.launchOrFalse(name)
                if (!opened) {
                    pendingExport = null
                    onMessage(NO_FILE_PICKER)
                }
            },
        )
    }

}

/**
 * Daily digest and home-screen widget settings, now hosted on the Notifications page: digest
 * scheduling (with its notification-permission request), the time picker and the app-lock-gated
 * widget privacy switch — including decoy-profile behavior — unchanged.
 */
@Composable
fun DigestSection(onMessage: (String) -> Unit) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ToolsSettings.get(context) }
    val realPrefs by remember { store.prefs }.collectAsStateWithLifecycle(initialValue = ToolsPrefs())

    // The decoy profile shows ordinary-looking settings but never changes the real ones.
    val decoy = DecoySession.isActive
    var decoyPrefs by remember { mutableStateOf(ToolsPrefs(pinHash = DECOY_PLACEHOLDER)) }
    val prefs = if (decoy) decoyPrefs else realPrefs

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun saveDigest(enabled: Boolean, minutes: Int = prefs.digestMinutes) {
        if (decoy) {
            decoyPrefs = decoyPrefs.copy(digestEnabled = enabled, digestMinutes = minutes); return
        }
        scope.launch {
            store.setDigestEnabled(enabled)
            store.setDigestMinutes(minutes)
            DigestSchedule.sync(context)
        }
    }

    SettingsSection(null) {
        SettingsToggle(
            "Daily digest",
            "Today's spending, budget left and items to review",
            prefs.digestEnabled,
            { on ->
                if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                saveDigest(on)
            },
            Icons.Outlined.Today,
        )
        if (prefs.digestEnabled) {
            SettingsRow("Digest time", null, Icons.Outlined.Schedule, value = formatMinutes(prefs.digestMinutes)) {
                TimePickerDialog(
                    context,
                    { _, h, m -> saveDigest(true, h * 60 + m) },
                    prefs.digestMinutes / 60, prefs.digestMinutes % 60,
                    DateFormat.is24HourFormat(context),
                ).show()
            }
        }
        SettingsToggle(
            "Home screen widgets",
            if (s.biometricLockEnabled) "Show real amounts on the home-screen widget even with app lock on" else "Turn on app lock to choose",
            prefs.widgetAmountsWhenLocked,
            { on ->
                if (decoy) decoyPrefs = decoyPrefs.copy(widgetAmountsWhenLocked = on)
                else scope.launch { store.setWidgetAmountsWhenLocked(on) }
            },
            Icons.Outlined.Widgets,
            enabled = s.biometricLockEnabled,
            divider = false,
        )
    }
}

private fun formatMinutes(minutes: Int): String =
    LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** Inclusive dates for [range]; `from` is null for "All time" until the data is known. */
internal fun rangeDates(range: ExportRange, cycle: com.acuranza.app.finance.PayCycle, earliest: LocalDate?, today: LocalDate = Time.today()): Pair<LocalDate?, LocalDate> {
    val period = Period.containing(today, cycle)
    return when (range) {
        ExportRange.THIS_PERIOD -> period.start to period.lastDay
        ExportRange.LAST_PERIOD -> period.previous().let { it.start to it.lastDay }
        ExportRange.LAST_12 -> today.minusMonths(12).plusDays(1) to today
        ExportRange.ALL -> earliest to today
    }
}

private suspend fun exportData(c: AppContainer, range: ExportRange, cycle: com.acuranza.app.finance.PayCycle): ExportData {
    val accounts = c.db.accounts().all()
    val categories = c.db.categories().all()
    val all = c.db.transactions().all()
    val earliest = all.minOfOrNull { it.timestamp }?.let { Time.toDate(it) }
    val (from, to) = rangeDates(range, cycle, earliest)
    val start = from ?: Time.today()
    // Transactions after the range stay in so balance assertions can tell whether a reported balance is final.
    return ExportData(accounts, categories, all, start, to)
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (ExportFormat, ExportRange) -> Unit) {
    var format by remember { mutableStateOf(ExportFormat.CSV) }
    var range by remember { mutableStateOf(ExportRange.THIS_PERIOD) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            com.acuranza.app.designsystem.components.DialogBlurBehind()
            Text("Export data")
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Range", style = MaterialTheme.typography.labelLarge, color = AcuranzaTheme.colors.subtleText)
                ExportRange.entries.forEach { r -> Choice(r.label, r == range) { range = r } }
                Text("Format", style = MaterialTheme.typography.labelLarge, color = AcuranzaTheme.colors.subtleText, modifier = Modifier.padding(top = 12.dp))
                ExportFormat.entries.forEach { f -> Choice(f.label, f == format) { format = f } }
                Text(
                    "The file is saved where you choose and isn't encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AcuranzaTheme.colors.subtleText,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onExport(format, range) }) { Text("Export", fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}
