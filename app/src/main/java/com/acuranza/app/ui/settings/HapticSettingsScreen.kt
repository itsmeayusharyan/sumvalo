package com.acuranza.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * The haptic control panel: one master switch, a strength choice, and a preference per real
 * interaction category — every toggle wired to the app's actual haptic call sites. The system's
 * own touch-feedback setting rules underneath all of it, and devices without a vibrator simply
 * ignore everything, so there is no permission and no failure path. Turning the master switch
 * off suppresses all app haptics but keeps the individual choices, so switching it back on
 * restores exactly what the user had.
 */
@Composable
fun HapticSettingsScreen(nav: NavController) {
    val c = LocalContainer.current
    val s = LocalSettings.current
    val scope = rememberCoroutineScope()
    val haptic = com.acuranza.app.ui.common.rememberHaptic()
    val context = androidx.compose.ui.platform.LocalContext.current
    val noVibratorToast = {
        android.widget.Toast.makeText(context, "This device has no vibration motor", android.widget.Toast.LENGTH_SHORT).show()
    }

    SettingsPage("Haptic feedback", "Strength and what vibrates", nav) {
        SettingsSection("Master") {
            SettingsToggle(
                "Haptic feedback",
                "Off suppresses every app vibration; your choices below are kept",
                s.hapticsEnabled,
                { v -> scope.launch { c.settings.setHapticsEnabled(v) } },
                Icons.Outlined.Vibration,
                divider = false,
            )
        }
        SettingsSection("Strength") {
            // Honest about the mechanism: amplitude where the vibrator supports it, duration
            // otherwise — both change perceptibly between Light, Standard and Strong.
            Text(
                "Amplitude where your device supports it, otherwise pulse length. Devices without a vibration motor stay still.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            SettingsPickerRow(
                "Feedback strength",
                listOf("LIGHT", "STANDARD", "STRONG"),
                s.hapticIntensity,
                { it.replaceFirstChar { ch -> ch.uppercase() } },
                Icons.Outlined.Bolt,
                enabled = s.hapticsEnabled,
            ) { strength -> scope.launch { c.settings.setHapticIntensity(strength) } }
            SettingsRow(
                "Test feedback",
                "Preview the ${s.hapticIntensity.lowercase()} feel without touching your data",
                Icons.Outlined.Dialpad,
                enabled = s.hapticsEnabled,
                divider = false,
            ) {
                // The SAME Vibrator path every app action uses — the real feel, nothing financial.
                if (haptic.canVibrate()) haptic.confirm() else noVibratorToast()
            }
        }
        SettingsSection("What vibrates") {
            SettingsToggle(
                "Calculator keypad",
                "Key presses while entering amounts",
                s.hapticKeypad,
                { v -> scope.launch { c.settings.setHapticKeypad(v) } },
                Icons.Outlined.Dialpad,
                enabled = s.hapticsEnabled,
            )
            SettingsToggle(
                "Saves & confirmations",
                "Adding, editing and deleting transactions and other confirmations",
                s.hapticActions,
                { v -> scope.launch { c.settings.setHapticActions(v) } },
                Icons.Outlined.Bolt,
                enabled = s.hapticsEnabled,
            )
            SettingsToggle(
                "Tabs & buttons",
                "Switching tabs and tapping chips, rows and buttons",
                s.hapticTabsAndButtons,
                { v -> scope.launch { c.settings.setHapticTabsAndButtons(v) } },
                Icons.Outlined.Vibration,
                enabled = s.hapticsEnabled,
                divider = false,
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Strength maps to the two feedback types Android exposes to apps; the system's own " +
                    "touch-feedback setting always has the final word.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
            )
        }
    }
}
