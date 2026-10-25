package com.acuranza.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/**
 * Dedicated legal pages reachable from About. Every statement here describes what Acuranza
 * actually does — no invented links, no invented guarantees.
 */

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = AcuranzaTheme.colors.subtleText,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

@Composable
private fun LegalHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = AcuranzaTheme.colors.ink,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun LegalPageScaffold(body: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        body()
    }
}

@Composable
fun PrivacyPolicyScreen(nav: NavController) {
    AcuranzaScreen(
        title = "Privacy policy",
        subtitle = "Applies to every build of Sumvalo",
        onBack = { nav.popBackSafely() },
    ) { padding ->
        LegalPageScaffold {
            Column(Modifier.padding(padding)) {
                LegalHeading("What leaves the phone: nothing")
                LegalParagraph(
                    "Sumvalo requests no internet permission — not even in the build that reads bank " +
                        "messages. Android itself therefore cannot send your transactions, accounts, " +
                        "budgets, reminders or messages anywhere. There are no analytics, no ads, no " +
                        "accounts, no servers and no tracking SDKs.",
                )
                LegalHeading("Where your data lives")
                LegalParagraph(
                    "Everything you record is stored in an encrypted database on this device " +
                        "(SQLCipher, AES-256, with the key held in the Android Keystore). Encrypted " +
                        "backups use AES-256-GCM with a key derived from a password only you know. " +
                        "Cloud device backups are excluded (allowBackup is disabled).",
                )
                LegalHeading("Notifications")
                LegalParagraph(
                    "Sumvalo asks for the notification permission before the app opens on first launch, " +
                        "because reminders (card-bill due dates, scheduled payments, unusual spending) " +
                        "reach you through notifications. It is the only permission the standard build asks " +
                        "for, and the app explains why before anything is enabled.",
                )
                LegalHeading("Bank-message tracking (optional)")
                LegalParagraph(
                    "In the standard build, bank messages enter only when you share them from your " +
                        "Messages app or paste them — no SMS permission is requested. In the Full build, " +
                        "with your explicit consent, Acuranza can scan your inbox history (requires the SMS " +
                        "permission) and read incoming bank notifications (requires notification-listener " +
                        "access). Both are granted by you in system dialogs, both are revocable, and every " +
                        "access is explained on a consent screen before anything is enabled.",
                )
                LegalParagraph(
                    "Messages are parsed on the device and the text is never stored: only the extracted " +
                        "transaction fields (amount, date, merchant, account reference) are kept, as " +
                        "transactions you review and confirm. OTP, PIN and password messages, promotions " +
                        "and unrelated personal messages are excluded by the parser. Withdrawing consent " +
                        "stops all processing immediately and deletes every pending candidate and every " +
                        "SMS-imported transaction; your own entries and every other record are untouched.",
                )
                LegalHeading("Deleting your data")
                LegalParagraph(
                    "You can export your data (CSV, Beancount, Ledger), delete imported data selectively, " +
                        "or erase everything. Deleting your data or uninstalling the app removes it from " +
                        "the phone completely — there is no copy anywhere else.",
                )
                LegalHeading("Changes")
                LegalParagraph(
                    "This policy describes the current release. Material changes ship in the release notes " +
                        "on the project's GitHub repository.",
                )
            }
        }
    }
}

@Composable
fun TermsScreen(nav: NavController) {
    AcuranzaScreen(
        title = "Terms of service",
        subtitle = "The plain terms of using Sumvalo",
        onBack = { nav.popBackSafely() },
    ) { padding ->
        LegalPageScaffold {
            Column(Modifier.padding(padding)) {
                LegalHeading("What Acuranza is")
                LegalParagraph(
                    "Sumvalo is free, open-source software for personal bookkeeping, provided as-is, " +
                        "free of charge. It runs entirely on your device; there is no account, no service " +
                        "and no support contract behind it.",
                )
                LegalHeading("What Acuranza is not")
                LegalParagraph(
                    "It keeps books and draws charts; nothing in it is financial advice. Numbers it " +
                        "extracts from bank messages or statements can be wrong — review them. Decisions " +
                        "you make with its figures are your own.",
                )
                LegalHeading("Your data is your responsibility")
                LegalParagraph(
                    "Keep backups, and keep a backup's password safe — an encrypted backup without its " +
                        "password cannot be recovered, by anyone.",
                )
                LegalHeading("Licence")
                LegalParagraph(
                    "Use of the app is governed by the GNU General Public License v3.0 (see Licence on " +
                        "the About screen). You may use, study, share and modify it; there is no warranty. " +
                        "The source is published at github.com/itsmeayusharyan/sumvalo, including the " +
                        "attribution to the upstream Kosh project it derives from.",
                )
            }
        }
    }
}
