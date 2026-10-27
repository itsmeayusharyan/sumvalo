package com.acuranza.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.data.SmsImportBatchEntity
import com.acuranza.app.data.SmsImportDao
import com.acuranza.app.data.SmsImportEntity
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.Money
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.SmsImportRepository
import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.style.TextOverflow

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault())

/**
 * Settings → Financial tools → SMS transaction tracking, permissionless edition: bank messages
 * arrive by Android's share sheet (SmsShareActivity) or by paste, are parsed on-device, and only
 * the extracted details are stored — after the user confirms them. The app holds no SMS
 * permission (Play Protect blocks sideloaded APKs that request one), so nothing ever reads the
 * inbox.
 */
@Composable
fun SmsTrackingScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<List<SmsImportEntity>>(emptyList()) }
    var reviewing by remember { mutableStateOf(false) }
    var clipboardHit by remember { mutableStateOf<String?>(null) }
    var showWithdrawNudge by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Full build: the SMS permission is requested the moment the user agrees — the consent
    // dialog and the runtime prompt follow each other, before anything is processed.
    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            message = "SMS permission denied. Tracking stays off until you grant it: App info → Permissions → SMS."
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            message = null
        }
    }
    LaunchedEffect(refresh, s.smsConsent) {
        pending = if (s.smsConsent && !c.decoy) c.smsImports.pending() else emptyList()
    }

    // In-app upgrade to the Full build (standard build only): the user picks an Sumvalo APK
    // from storage; the app verifies package + signature + version and stages Android's own
    // install confirmation. The OS security check runs as usual; nothing is disabled.
    // Session outcomes arrive on a process-scoped event flow, so a screen recreation while
    // Android's installer dialog is open can no longer strand the UI on "install in progress".
    var staging by remember { mutableStateOf(false) }
    val installEvent by com.acuranza.app.install.PackageUpgradeInstaller.events.collectAsState()
    val upgrading = staging || installEvent is com.acuranza.app.install.InstallEvent.WaitingForConfirmation
    val apkPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                staging = true
                val installer = com.acuranza.app.install.PackageUpgradeInstaller(context)
                val staged = runCatching {
                    val cache = java.io.File(context.cacheDir, "picked-update.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cache.outputStream().use { input!!.copyTo(it) }
                    } ?: return@runCatching false
                    when (val verified = installer.verify(cache)) {
                        is com.acuranza.app.install.PackageUpgradeInstaller.Verified.Ok -> {
                            com.acuranza.app.install.PackageUpgradeInstaller.reset()
                            installer.install(cache)
                        }
                        is com.acuranza.app.install.PackageUpgradeInstaller.Verified.Rejected -> {
                            message = when (verified.reason) {
                                com.acuranza.app.install.PackageUpgradeValidator.Decision.SignatureMismatch ->
                                    "That APK isn't signed with Sumvalo's key — refusing"
                                com.acuranza.app.install.PackageUpgradeValidator.Decision.WrongPackage ->
                                    "That APK isn't Sumvalo"
                                com.acuranza.app.install.PackageUpgradeValidator.Decision.Downgrade ->
                                    "That build is older than the installed one"
                                else -> "Couldn't read that file as an APK"
                            }
                            false
                        }
                    }
                }.getOrDefault(false)
                when {
                    staged == null -> Unit // committed; the event flow reports the outcome
                    staged is String -> message = "Couldn't start the install: $staged"
                    else -> Unit // validator rejection already messaged
                }
                staging = false
            }
        }
    }

    // One-tap capture: with consent and this screen open, a bank message sitting in the
    // clipboard is offered for parsing. The clipboard is read only here (foreground, user-initiated
    // screen), checked with the same pure pipeline, and never stored unless the user taps Parse.
    LaunchedEffect(refresh, s.smsConsent, reviewing) {
        clipboardHit = null
        if (s.smsConsent && !c.decoy && !reviewing) {
            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as? android.content.ClipboardManager
            val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                val result = com.acuranza.app.sms.SharedSmsPipeline.process(text, System.currentTimeMillis())
                if (result is com.acuranza.app.sms.SharedSmsPipeline.Result.Candidate) clipboardHit = text
            }
        }
    }

    SumvaloScreen(
        title = "SMS transaction tracking",
        subtitle = "Bank messages, shared by you, read on this device only",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        if (showWithdrawNudge) {
            AlertDialog(
                onDismissRequest = { showWithdrawNudge = false },
                title = { Text("Export before you withdraw?") },
                text = {
                    Text(
                        "Withdrawing stops SMS tracking and deletes everything it imported — pending bank " +
                            "messages, imported transactions and the bank accounts it created for you. Export " +
                            "your records first if you want to keep them. Exported files may contain sensitive " +
                            "financial information — store them somewhere safe; nothing is exported or shared " +
                            "automatically.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showWithdrawNudge = false
                        nav.navigate(com.acuranza.app.ui.nav.Routes.SETTINGS_DATA)
                    }) { Text("Export data first") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { showWithdrawNudge = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            showWithdrawNudge = false
                            showWithdrawDialog = true
                        }) { Text("Continue") }
                    }
                },
            )
        }

        if (showWithdrawDialog) {
            AlertDialog(
                onDismissRequest = { showWithdrawDialog = false },
                title = { Text("Withdraw SMS tracking?") },
                text = {
                    Text(
                        "All processing stops immediately. This deletes every pending bank message, every " +
                            "transaction imported from SMS (${pending.size} pending, plus imported ones) and the bank " +
                            "accounts SMS tracking created, as long as they hold nothing else. Transactions you " +
                            "entered yourself, CSV imports, budgets and every other record are untouched; balances are " +
                            "recomputed from what remains. Android does not let an app revoke its own SMS permission — " +
                            "to remove that too, revoke it in App info → Permissions → SMS.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showWithdrawDialog = false
                        scope.launch {
                            // Stop first: worker off, flags off — nothing may process while we delete.
                            com.acuranza.app.work.WorkScheduler.setSmsScanEnabled(context, false)
                            c.settings.revokeSmsConsent()
                            val removed = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                c.smsImports.deleteAll()
                            }
                            message = "SMS tracking withdrawn. Removed $removed imported transactions."
                            refresh++
                        }
                    }) { Text("Withdraw and delete", color = SumvaloTheme.colors.alert) }
                },
                dismissButton = { TextButton(onClick = { showWithdrawDialog = false }) { Text("Keep tracking") } },
            )
        }

        when {
            c.decoy -> Column(Modifier.padding(padding).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("Unavailable", style = SumvaloText.screenTitle)
                Spacer(Modifier.height(8.dp))
                Text("SMS tracking is disabled in this profile.", color = SumvaloTheme.colors.subtleText)
            }
            !s.smsConsent -> SmsConsentContent(
                modifier = Modifier.padding(padding),
                onAgree = {
                    scope.launch {
                        c.settings.setSmsConsent(true)
                        // Full build only: the runtime prompt follows the consent immediately.
                        if (com.acuranza.app.BuildConfig.SMS_FULL) {
                            smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                        }
                    }
                },
            )
            reviewing -> SmsPendingReviewContent(
                pending = pending,
                onCancel = { reviewing = false },
                onConfirm = { keep, edits ->
                    scope.launch {
                        val created = c.smsImports.confirmPending(keep, edits)
                        reviewing = false
                        message = if (created > 0) {
                            "$created transactions imported — flagged ones wait in Review"
                        } else {
                            "Nothing selected to import"
                        }
                        refresh++
                    }
                },
            )
            else -> SmsOverviewContent(
                settings = s,
                pending = pending,
                clipboardHit = clipboardHit,
                onParseClipboard = { hit ->
                    scope.launch {
                        when (val outcome = c.smsImports.addPending(hit)) {
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Added ->
                                message = "Saved for review"
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Duplicate ->
                                message = "Already added"
                            is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Rejected ->
                                message = "Not imported: ${outcome.reason}"
                        }
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as? android.content.ClipboardManager)?.setPrimaryClip(
                            android.content.ClipData.newPlainText("acuranza", ""),
                        )
                        clipboardHit = null
                        refresh++
                    }
                },
                onDismissClipboard = { clipboardHit = null },
                onMessage = { message = it },
                onChanged = { refresh++ },
                onPickFullApk = { apkPicker.launch(arrayOf("application/vnd.android.package-archive")) },
                upgrading = upgrading,
                refreshKey = refresh,
                onReview = { reviewing = true },
                onDeleteAll = {
                    scope.launch {
                        val removed = c.smsImports.deleteAll()
                        message = "Removed $removed imported transactions"
                        refresh++
                    }
                },
                onWithdraw = { showWithdrawNudge = true },
                modifier = Modifier.padding(padding),
            )
        }
            UpgradeDialog(
                visible = staging || installEvent is com.acuranza.app.install.InstallEvent.WaitingForConfirmation ||
                    installEvent is com.acuranza.app.install.InstallEvent.Failed ||
                    installEvent is com.acuranza.app.install.InstallEvent.Success,
                staging = staging,
                event = installEvent,
                onRetry = {
                    com.acuranza.app.install.PackageUpgradeInstaller.reset()
                    apkPicker.launch(arrayOf("application/vnd.android.package-archive"))
                },
                onDismiss = { com.acuranza.app.install.PackageUpgradeInstaller.reset() },
            )
    }
}

/**
 * The modal shown from the moment a Full-build APK is picked until the outcome is known:
 * preparing → waiting for Android's installer approval → verified success or a specific failure
 * with a retry. Stateless body so the screenshot fixtures render every state without a session.
 */
@Composable
internal fun UpgradeDialog(
    visible: Boolean,
    staging: Boolean,
    event: com.acuranza.app.install.InstallEvent?,
    onRetry: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (!visible) return
    val dismissible = event is com.acuranza.app.install.InstallEvent.Failed
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        confirmButton = {
            when (event) {
                                is com.acuranza.app.install.InstallEvent.Failed -> androidx.compose.material3.TextButton(onClick = onRetry) { Text("Try again") }
                                com.acuranza.app.install.InstallEvent.Success -> androidx.compose.material3.TextButton(onClick = onDismiss) { Text("OK") }
                                else -> {}
                            }
        },
        dismissButton = if (dismissible) {
            { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Not now") } }
        } else null,
        title = { Text("Updating to Full Build", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    staging -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Verifying and preparing the update…", style = MaterialTheme.typography.bodyMedium)
                        }
                        UpgradeDialogMessage()
                    }
                    event is com.acuranza.app.install.InstallEvent.WaitingForConfirmation -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Text("Android's installer is open — review and approve the update there.", style = MaterialTheme.typography.bodyMedium)
                        }
                        UpgradeDialogMessage()
                    }
                    event is com.acuranza.app.install.InstallEvent.Success -> Text(
                        "Update installed. Sumvalo is restarting as the Full build — your transactions, accounts and settings are exactly as you left them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumvaloTheme.colors.income,
                    )
                    event is com.acuranza.app.install.InstallEvent.Failed -> Text(
                        (event as com.acuranza.app.install.InstallEvent.Failed).reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SumvaloTheme.colors.alert,
                    )
                }
            }
        },
    )
}

@Composable
private fun UpgradeDialogMessage() {
    Text(
        "You're upgrading Sumvalo to the Full Build, which enables automatic SMS transaction " +
            "tracking. Your existing transactions, accounts and settings will be preserved. Android " +
            "will ask you to approve the installation before the update can proceed.",
        style = MaterialTheme.typography.bodyMedium,
        color = SumvaloTheme.colors.subtleText,
    )
}

/** What the user agrees to before anything is processed. */
@Composable
fun SmsConsentContent(modifier: Modifier, onAgree: () -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        SumvaloCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Sms, contentDescription = null, tint = SumvaloTheme.colors.accent)
                    Text("How it works", style = MaterialTheme.typography.titleMedium)
                }
                Bullet("Share a bank message from your Messages app to Sumvalo (long-press → Share → Sumvalo), or paste one here.")
                Bullet("Sumvalo extracts the amount, date, merchant and account, and turns the message into a transaction you confirm.")
                Bullet("OTP, PIN, password and authentication messages are never processed. Promotions and balance alerts are ignored.")
                Bullet("The message text is parsed and discarded. Only the extracted details are stored — in Sumvalo's private, encrypted storage.")
                Bullet("No SMS permission is requested, so Play Protect installs Sumvalo like any other app.")
                Bullet("Everything runs offline. There is no account, cloud or sync, and you can delete everything at any time.")
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("I agree — enable", onClick = onAgree, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Nothing is processed — shared, pasted or otherwise — before you agree.",
            style = MaterialTheme.typography.bodySmall,
            color = SumvaloTheme.colors.tertiaryText,
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("·", color = SumvaloTheme.colors.accent)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SumvaloTheme.colors.subtleText)
    }
}

/** The user's per-row corrections before a pending candidate becomes a transaction. */
private data class PendingEditState(
    val amount: Long? = null,
    val merchant: String? = null,
    val excluded: Boolean = false,
)

private sealed interface PendingList {
    data class Header(val text: String) : PendingList
    data class Row(val item: SmsImportEntity) : PendingList
}

/** Confirm-import review of the pending candidates. Nothing is imported until Confirm. */
@Composable
private fun SmsPendingReviewContent(
    pending: List<SmsImportEntity>,
    onCancel: () -> Unit,
    onConfirm: (keep: List<SmsImportEntity>, edits: Map<Long, SmsImportRepository.PendingEdit>) -> Unit,
) {
    val c = LocalContainer.current
    val edits = remember(pending) { pending.associate { it.id to mutableStateOf(PendingEditState()) } }
    var expanded by remember { mutableLongStateOf(-1L) }
    // Account matching: the hint says where each candidate would land; the user can override it.
    var hints by remember(pending) { mutableStateOf<Map<Long, com.acuranza.app.data.sms.SmsAccountMatcher.Match>>(emptyMap()) }
    var accounts by remember { mutableStateOf<List<com.acuranza.app.data.sms.SmsAccountMatcher.CandidateAccount>>(emptyList()) }
    var chosen by remember(pending) { mutableStateOf<Map<Long, Long>>(emptyMap()) }
    var choosing by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pending) {
        accounts = c.smsImports.pickerAccounts()
        hints = pending.associate { it.id to c.smsImports.accountHint(it) }
    }
    val needsReview = pending.filter { it.confidence != SmsConfidence.HIGH }
    val confident = pending.filter { it.confidence == SmsConfidence.HIGH }
    val rows = remember(pending) {
        buildList {
            if (needsReview.isNotEmpty()) add(PendingList.Header("Needs review — ${needsReview.size}"))
            needsReview.forEach { add(PendingList.Row(it)) }
            if (confident.isNotEmpty()) add(PendingList.Header("Confident — ${confident.size}"))
            confident.forEach { add(PendingList.Row(it)) }
        }
    }
    val included = pending.count { !edits[it.id]!!.value.excluded }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("${pending.size} bank transactions ready", style = MaterialTheme.typography.titleLarge)
        Text(
            "Check each one — edit what's wrong, switch off what isn't yours. Nothing is saved until you confirm.",
            style = MaterialTheme.typography.bodyMedium,
            color = SumvaloTheme.colors.subtleText,
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows) { row ->
                when (row) {
                    is PendingList.Header -> Text(
                        row.text.uppercase(),
                        style = SumvaloText.sectionLabel,
                        color = SumvaloTheme.colors.subtleText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    is PendingList.Row -> {
                        val item = row.item
                        val editState = edits[item.id]!!
                        val edit by editState
                        SumvaloCard(onClick = { expanded = if (expanded == item.id) -1L else item.id }) {
                            Column(Modifier.padding(vertical = 14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            edit.merchant ?: item.merchant ?: item.bank,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${item.bank}${item.accountLast4?.let { " $it" } ?: ""} · " +
                                                dateFormat.format(Instant.ofEpochMilli(item.timestamp)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SumvaloTheme.colors.tertiaryText,
                                        )
                                        val targetName = chosen[item.id]?.let { id -> accounts.firstOrNull { it.id == id }?.name }
                                            ?: when (val hint = hints[item.id]) {
                                                is com.acuranza.app.data.sms.SmsAccountMatcher.Match.Exact -> hint.account.name
                                                is com.acuranza.app.data.sms.SmsAccountMatcher.Match.ByBank -> hint.account.name
                                                com.acuranza.app.data.sms.SmsAccountMatcher.Match.None -> null
                                                null -> null
                                            }
                                        val needsAccountPick = hints[item.id] is com.acuranza.app.data.sms.SmsAccountMatcher.Match.None
                                        Text(
                                            if (targetName != null) "→ $targetName" else "→ new account: ${item.bank}${item.accountLast4?.let { " $it" } ?: ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (needsAccountPick) SumvaloTheme.colors.warning else SumvaloTheme.colors.accent,
                                            modifier = Modifier.padding(top = 1.dp).clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                role = androidx.compose.ui.semantics.Role.Button,
                                            ) { choosing = item.id },
                                        )
                                    }
                                    Text(
                                        Money.format(edit.amount ?: item.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.type == TxnType.INCOME) SumvaloTheme.colors.income else SumvaloTheme.colors.expense,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Switch(checked = !edit.excluded, onCheckedChange = { editState.value = editState.value.copy(excluded = !it) })
                                }
                                if (expanded == item.id) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = edit.merchant ?: item.merchant ?: "",
                                        onValueChange = { editState.value = editState.value.copy(merchant = it.ifBlank { null }) },
                                        label = { Text("Merchant") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = ((edit.amount ?: item.amount) / 100.0).toString(),
                                        onValueChange = { text ->
                                            editState.value = editState.value.copy(amount = Money.fromRupees(text.toDoubleOrNull() ?: 0.0))
                                        },
                                        label = { Text("Amount (₹)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(
                "Confirm import ($included)",
                onClick = {
                    val keep = pending.filter { !edits[it.id]!!.value.excluded }
                    val editMap = pending.mapNotNull { item ->
                        val e = edits[item.id]!!.value
                        if (e.excluded) null
                        else item.id to SmsImportRepository.PendingEdit(
                            amount = e.amount,
                            merchant = e.merchant,
                            accountId = chosen[item.id],
                        )
                    }.toMap()
                    onConfirm(keep, editMap)
                },
                modifier = Modifier.weight(1.6f),
            )
        }
        Spacer(Modifier.height(12.dp))
    }

    choosing?.let { rowId ->
        AlertDialog(
            onDismissRequest = { choosing = null },
            title = { Text("Which account?") },
            text = {
                Column {
                    accounts.forEach { account ->
                        TextButton(onClick = {
                            chosen = chosen + (rowId to account.id)
                            choosing = null
                        }) { Text("${account.name} · ${account.type.name.lowercase().replaceFirstChar { it.uppercase() }}") }
                    }
                    TextButton(onClick = {
                        chosen = chosen - rowId
                        choosing = null
                    }) { Text("Create a new account for ${pending.firstOrNull { it.id == rowId }?.bank ?: "this bank"}") }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun SmsOverviewContent(
    settings: com.acuranza.app.data.prefs.Settings,
    pending: List<SmsImportEntity>,
    clipboardHit: String?,
    onParseClipboard: (String) -> Unit,
    onDismissClipboard: () -> Unit,
    onMessage: (String) -> Unit,
    onChanged: () -> Unit,
    onPickFullApk: () -> Unit,
    upgrading: Boolean,
    refreshKey: Int,
    onReview: () -> Unit,
    onDeleteAll: () -> Unit,
    onWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Spacer(Modifier.height(4.dp)) }

        // One-tap: a bank message copied in the Messages app is offered for parsing
        clipboardHit?.let { hit ->
            item {
                SumvaloCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Bank message detected", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "You copied a bank message. Parse it now? The clipboard is cleared after.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton(
                                "Parse",
                                onClick = { onParseClipboard(hit) },
                                modifier = Modifier.weight(1f),
                            )
                            SecondaryButton(
                                "Not now",
                                onClick = onDismissClipboard,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // ── Full variant only: automatic sources (compile-time guarded; standard builds ship
        //    none of this and R8 strips it) ──────────────────────────────────────────────
        if (com.acuranza.app.BuildConfig.SMS_FULL) {
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()
                var scanning by remember { mutableStateOf(false) }
                fun smsPermissionNow(): Boolean = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_SMS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                // Observable permission state — the UI reacts the moment the grant lands, with
                // no refresh or screen reopen. Re-checked on every resume so grants made in system
                // settings and revocations are both reflected immediately.
                var smsGranted by remember { mutableStateOf(smsPermissionNow()) }
                val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(owner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) smsGranted = smsPermissionNow()
                    }
                    owner.lifecycle.addObserver(observer)
                    onDispose { owner.lifecycle.removeObserver(observer) }
                }
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    smsGranted = granted || smsPermissionNow()
                    if (!granted) onMessage("Without the SMS permission the inbox can't be scanned; share/paste still works")
                }
                val listenerEnabled = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
                    .contains(context.packageName)
                val hasSms = smsGranted

                SumvaloCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Automatic sources", style = MaterialTheme.typography.titleMedium)

                        if (!hasSms) {
                            Text(
                                "Inbox import needs the SMS permission. It reads bank messages on this phone only — nothing is ever uploaded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SumvaloTheme.colors.subtleText,
                            )
                            SecondaryButton(
                                "Grant SMS permission",
                                onClick = { permissionLauncher.launch(android.Manifest.permission.READ_SMS) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        if (hasSms) {
                            PrimaryButton(
                                if (scanning) "Scanning…"
                                else if (settings.smsLastScanAt == 0L) "Scan inbox (last 12 months)"
                                else "Scan inbox (new messages)",
                                enabled = !scanning,
                                onClick = {
                                    scope.launch {
                                        val runner = c.smsScanRunner ?: return@launch
                                        scanning = true
                                        // The whole walk — cursor I/O, parsing, dedup checks —
                                        // stays off the main thread; the UI stays responsive.
                                        val (result, found) = kotlinx.coroutines.withContext(
                                            kotlinx.coroutines.Dispatchers.IO,
                                        ) { runner.run(auto = false, dryRun = true) }
                                        var added = 0
                                        for (candidate in found) {
                                            val outcome = c.smsImports.addPendingParsed(candidate.graded.txn, candidate.graded.confidence)
                                            if (outcome is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Added) added++
                                        }
                                        if (added > 0) c.settings.setSmsFirstImportDone()
                                        scanning = false
                                        onMessage(
                                            when {
                                                result.status != "ok" -> "Scan: ${result.status}"
                                                added > 0 -> "$added bank transactions waiting for review"
                                                else -> "No new bank transactions found"
                                            },
                                        )
                                        onChanged()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                if (settings.smsLastScanAt == 0L) "Never scanned"
                                else "Last scan ${java.time.Instant.ofEpochMilli(settings.smsLastScanAt)} — ${settings.smsLastScanStatus}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SumvaloTheme.colors.tertiaryText,
                            )
                        }

                        run {
                            SettingsToggle(
                                title = "Scan inbox automatically",
                                subtitle = "Every 10 minutes while the app is open; background catch-up every 15 minutes (Android minimum), if SMS permission stays granted. Off stops future scans; nothing imported is deleted.",
                                checked = settings.smsAutoScan && hasSms,
                                onChecked = { on ->
                                    scope.launch {
                                        c.settings.setSmsAutoScan(on)
                                        com.acuranza.app.work.WorkScheduler.setSmsScanEnabled(
                                            context, on && hasSms,
                                        )
                                        onChanged()
                                    }
                                },
                                divider = false,
                            )
                            SettingsToggle(
                                title = "Capture from notifications",
                                subtitle = when {
                                    !listenerEnabled -> "Needs Notification Access — tap to open system settings, then enable Sumvalo there."
                                    else -> "New bank SMS in the Messages app's notifications park here for review. Off stops capture immediately."
                                },
                                checked = settings.smsAutoCapture && listenerEnabled,
                                onChecked = { on ->
                                    scope.launch { c.settings.setSmsAutoCapture(on) }
                                    if (on && !listenerEnabled) {
                                        context.startActivity(
                                            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                                        )
                                    }
                                },
                                divider = false,
                            )
                        }
                    }
                }
            }
        }

        // Standard build only: switch to the Full build from an APK file on this device.
        if (!com.acuranza.app.BuildConfig.SMS_FULL) {
            item {
                SumvaloCard {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Full build with automatic SMS", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sumvalo also ships a Full build that reads bank SMS automatically (historical inbox import, " +
                                "background scanning, notification capture). Pick its APK from your storage and this app verifies it " +
                                "(package + signature) and starts Android's own install confirmation — Google's security check " +
                                "still runs, and any prompt it raises is yours to answer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SumvaloTheme.colors.subtleText,
                        )
                        // Status lives in the modal UpgradeDialog, not inline: one place for
                        // preparing, waiting for Android's approval, failure with retry, and
                        // the verified-success message.
                        SecondaryButton(
                            if (upgrading) "Update in progress…" else "Install Full build from APK",
                            enabled = !upgrading,
                            onClick = onPickFullApk,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }


        // Pending review
        if (pending.isNotEmpty()) {
            item {
                SettingsRow(
                    title = "Pending review — ${pending.size}",
                    subtitle = "Bank messages waiting for your confirmation. Nothing is imported until you confirm.",
                    icon = Icons.Outlined.Sms,
                ) { onReview() }
            }
        }


        // Danger zone
        item {
            Column {
                SettingsRow(
                    title = "Delete all SMS data",
                    subtitle = "Removes every pending candidate and every imported SMS transaction. Everything else stays.",
                    icon = Icons.Outlined.Delete,
                    tone = RowTone.DESTRUCTIVE,
                ) { confirmDeleteAll = true }
                SettingsRow(
                    title = "Withdraw consent",
                    subtitle = "Stops all processing. Imported transactions stay until deleted.",
                    icon = Icons.Outlined.Block,
                    divider = false,
                    tone = RowTone.DESTRUCTIVE,
                ) { onWithdraw() }
            }
        }

        item {
            Text(
                "Sumvalo holds no SMS permission. Messages arrive only when you share or paste them; " +
                    "the text is parsed and discarded, and the app has no network permission at all.",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.tertiaryText,
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all SMS data?") },
            text = { Text("Every pending candidate and every transaction imported from SMS will be removed. Transactions you added yourself stay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    onDeleteAll()
                }) { Text("Delete", color = SumvaloTheme.colors.alert) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Keep") } },
        )
    }
}
