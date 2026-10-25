package com.acuranza.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TheaterComedy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.acuranza.app.data.DatabaseEncryption
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.security.BiometricGate
import com.acuranza.app.security.DecoySession
import com.acuranza.app.security.PinHasher
import com.acuranza.app.security.UnlockResult
import com.acuranza.app.ui.AppLock
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.tools.ToolsPrefs
import com.acuranza.app.tools.ToolsSettings
import com.acuranza.app.ui.common.LocalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val DECOY_PLACEHOLDER = "decoy"

/**
 * Privacy & security settings: the app lock, screenshot blocking, database encryption status,
 * the app PIN that unlocks Sumvalo, the decoy PIN that opens a sample profile, and recovery
 * options — what still works if a PIN is forgotten or the phone is lost.
 */
@Composable
fun PrivacySecuritySection(onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val s = LocalSettings.current
    val c = LocalContainer.current
    val scope = rememberCoroutineScope()
    val store = remember { ToolsSettings.get(context) }
    val realPrefs by remember { store.prefs }.collectAsStateWithLifecycle(initialValue = ToolsPrefs())

    // The decoy profile shows ordinary-looking settings but never changes the real ones.
    val decoy = DecoySession.isActive
    var decoyPrefs by remember { mutableStateOf(ToolsPrefs(pinHash = DECOY_PLACEHOLDER)) }
    val prefs = if (decoy) decoyPrefs else realPrefs

    var pinDialog by remember { mutableStateOf<PinAction?>(null) }
    var recoveryOpen by remember { mutableStateOf(false) }

    SettingsSection(null) {
        val canLock = remember { BiometricGate.isAvailable(context) }
        SettingsToggle(
            "App lock",
            if (canLock) "Fingerprint, face or screen lock when opening Sumvalo" else "Set a screen lock on your phone to use this",
            s.biometricLockEnabled,
            { v ->
                val activity = context as? FragmentActivity
                if (!v) {
                    scope.launch { c.settings.setBiometricLock(false) }
                } else if (activity != null) {
                    scope.launch {
                        if (BiometricGate.authenticate(activity, title = "Turn on app lock") == UnlockResult.Success) {
                            // The user just proved it's them: keep this session unlocked.
                            AppLock.markUnlocked()
                            c.settings.setBiometricLock(true)
                        }
                    }
                }
            },
            Icons.Outlined.Fingerprint,
            enabled = canLock,
        )
        SettingsToggle(
            "Block screenshots",
            "Hide Sumvalo in the recents preview and block screenshots",
            s.blockScreenshots,
            { v -> scope.launch { c.settings.setBlockScreenshots(v) } },
            Icons.Outlined.ScreenLockPortrait,
        )
        val encrypted = DatabaseEncryption.status == DatabaseEncryption.Status.ENCRYPTED
        SettingsRow(
            "Database encryption",
            if (encrypted) "AES-256 (SQLCipher), key kept in the phone's secure hardware keystore" else "Sumvalo will encrypt it on the next launch",
            Icons.Outlined.Lock,
            trailing = {
                Pill(if (encrypted) "On" else "Pending", if (encrypted) SumvaloTheme.colors.good else SumvaloTheme.colors.warning)
            },
        )
        SettingsRow(
            "App PIN",
            when {
                !prefs.pinSet -> "Off · unlock with a PIN instead of fingerprint"
                s.biometricLockEnabled -> "On"
                else -> "On · used when app lock is on"
            },
            Icons.Outlined.Password,
        ) { pinDialog = if (prefs.pinSet) PinAction.MANAGE else PinAction.SET }
        val decoyReady = prefs.pinSet && s.biometricLockEnabled
        SettingsRow(
            "Decoy PIN",
            when {
                prefs.decoySet -> "On · opens a separate sample profile"
                !decoyReady -> "Needs app lock and an app PIN"
                else -> "Off · a second PIN that opens a sample profile"
            },
            Icons.Outlined.TheaterComedy,
            tone = if (decoyReady) RowTone.NORMAL else RowTone.MUTED,
            enabled = decoyReady,
        ) { pinDialog = if (prefs.decoySet) PinAction.REMOVE_DECOY else PinAction.SET_DECOY }
        SettingsRow(
            "Recovery options",
            "What still works if a PIN is forgotten or the phone is lost",
            Icons.Outlined.Shield,
            divider = false,
        ) { recoveryOpen = true }
    }

    if (recoveryOpen) {
        AlertDialog(
            onDismissRequest = { recoveryOpen = false },
            title = { Text("Recovery options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Forgotten your app PIN? Biometrics always open your real profile — unlock with fingerprint, face or your phone's screen lock instead.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "No biometrics set up? A phone screen lock (PIN, pattern or password) is all Sumvalo needs; set one in Android's security settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Lost or erased your data? The only recovery is an encrypted backup made from Data management below — keep the file and its password safe.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { recoveryOpen = false }) { Text("Done") } },
        )
    }

    pinDialog?.let { action ->
        PinDialog(
            action = action,
            // In the decoy profile the decoy PIN plays the part of the app PIN.
            currentHash = if (decoy) realPrefs.decoyPinHash else realPrefs.pinHash,
            otherHash = if (decoy) null else realPrefs.decoyPinHash,
            onDismiss = { pinDialog = null },
            onAction = { next -> pinDialog = next },
            onSave = { result ->
                pinDialog = null
                if (decoy) {
                    decoyPrefs = when (result) {
                        is PinResult.Real -> decoyPrefs.copy(pinHash = result.hash?.let { DECOY_PLACEHOLDER }, decoyPinHash = null)
                        is PinResult.Decoy -> decoyPrefs.copy(decoyPinHash = result.hash?.let { DECOY_PLACEHOLDER })
                    }
                } else {
                    scope.launch {
                        when (result) {
                            is PinResult.Real -> store.setPinHash(result.hash)
                            is PinResult.Decoy -> store.setDecoyPinHash(result.hash)
                        }
                    }
                }
                onMessage(
                    when (result) {
                        is PinResult.Real -> if (result.hash == null) "App PIN removed" else "App PIN saved"
                        is PinResult.Decoy -> if (result.hash == null) "Decoy PIN removed" else "Decoy PIN saved"
                    },
                )
            },
        )
    }
}

private enum class PinAction { SET, MANAGE, CHANGE, REMOVE, SET_DECOY, REMOVE_DECOY }

private sealed interface PinResult {
    /** A new app PIN hash, or null to remove it (and the decoy PIN with it). */
    data class Real(val hash: String?) : PinResult
    data class Decoy(val hash: String?) : PinResult
}

@Composable
private fun PinDialog(
    action: PinAction,
    currentHash: String?,
    otherHash: String?,
    onDismiss: () -> Unit,
    onAction: (PinAction) -> Unit,
    onSave: (PinResult) -> Unit,
) {
    if (action == PinAction.MANAGE) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("App PIN") },
            text = { Text("Change your PIN, or remove it to unlock with fingerprint or screen lock only. Removing it also removes the decoy PIN.") },
            confirmButton = { TextButton(onClick = { onAction(PinAction.CHANGE) }) { Text("Change") } },
            dismissButton = { TextButton(onClick = { onAction(PinAction.REMOVE) }) { Text("Remove", color = SumvaloTheme.colors.expense) } },
        )
        return
    }
    val scope = rememberCoroutineScope()
    val needsCurrent = action != PinAction.SET
    val needsNew = action == PinAction.SET || action == PinAction.CHANGE || action == PinAction.SET_DECOY
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    val title = when (action) {
        PinAction.SET -> "Set app PIN"
        PinAction.CHANGE -> "Change app PIN"
        PinAction.REMOVE -> "Remove app PIN"
        PinAction.SET_DECOY -> "Set decoy PIN"
        PinAction.REMOVE_DECOY -> "Remove decoy PIN"
        PinAction.MANAGE -> ""
    }

    fun submit() {
        if (needsNew) {
            if (!PinHasher.isValidPin(new)) { error = "Use ${PinHasher.MIN_LENGTH}–${PinHasher.MAX_LENGTH} digits"; return }
            if (new != confirm) { error = "The PINs don't match"; return }
        }
        working = true
        scope.launch {
            val outcome = withContext(Dispatchers.Default) {
                // Changes need the current app PIN, so a borrowed unlocked phone can't change or reveal the setup.
                if (needsCurrent && !PinHasher.verify(current, currentHash)) return@withContext "Wrong current PIN"
                when (action) {
                    PinAction.SET, PinAction.CHANGE -> {
                        if (otherHash != null && PinHasher.verify(new, otherHash)) "Use a PIN different from the decoy PIN"
                        else PinResult.Real(PinHasher.hash(new))
                    }
                    PinAction.SET_DECOY -> {
                        if (action == PinAction.SET_DECOY && PinHasher.verify(new, currentHash)) "The decoy PIN must differ from the app PIN"
                        else PinResult.Decoy(PinHasher.hash(new))
                    }
                    PinAction.REMOVE -> PinResult.Real(null)
                    PinAction.REMOVE_DECOY -> PinResult.Decoy(null)
                    PinAction.MANAGE -> null
                }
            }
            working = false
            when (outcome) {
                is PinResult -> onSave(outcome)
                is String -> error = outcome
                else -> onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (action == PinAction.SET_DECOY) {
                    Text(
                        "Entering this PIN on the lock screen opens a separate profile with sample data. Your real data stays hidden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SumvaloTheme.colors.subtleText,
                    )
                }
                if (needsCurrent) PinField(current, "Current app PIN") { current = it; error = null }
                if (needsNew) {
                    PinField(new, if (action == PinAction.SET_DECOY) "Decoy PIN" else "New PIN") { new = it; error = null }
                    PinField(confirm, "Repeat PIN") { confirm = it; error = null }
                }
                error?.let { Text(it, color = SumvaloTheme.colors.expense, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = !working) {
                Text(if (working) "…" else if (needsNew) "Save" else "Remove", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("Cancel") } },
    )
}

@Composable
private fun PinField(value: String, label: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValue(v.filter { it.isDigit() }.take(PinHasher.MAX_LENGTH)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
