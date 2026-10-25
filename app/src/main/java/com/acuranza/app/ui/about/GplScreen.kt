package com.acuranza.app.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.navigation.NavController
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.nav.popBackSafely
import kotlinx.coroutines.launch

/** The app's own licence, as a dedicated page. The full text ships with the source distribution. */
/** The full licence text, as committed in the repository (verified reachable). */
const val LICENCE_URL = "https://github.com/itsmeayusharyan/sumvalo/blob/acuranza/LICENSE"

@Composable
fun GplScreen(nav: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = androidx.compose.runtime.remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    AcuranzaScreen(
        title = "Licence",
        subtitle = "GNU General Public License v3.0",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Sumvalo is free and open-source software under the GNU General Public License v3.0.",
                style = MaterialTheme.typography.bodyMedium,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                "You are free to use, study, share and modify it. Under GPL-3.0, any redistribution of " +
                    "the app — modified or not — must keep the same licence and carry its source. There " +
                    "is no warranty: the app is provided as-is, without even the implied warranty of " +
                    "merchantability or fitness for a particular purpose.",
                style = MaterialTheme.typography.bodyMedium,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                "The full licence text accompanies the app's source distribution (the LICENSE file in " +
                    "the repository), alongside the attribution to the upstream Kosh project this app " +
                    "derives from. Each source file keeps the copyright notice it carried.",
                style = MaterialTheme.typography.bodyMedium,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            com.acuranza.app.ui.settings.SettingsRow(
                "View the full licence text",
                "github.com/itsmeayusharyan/sumvalo · LICENSE",
                androidx.compose.material.icons.Icons.Outlined.Description,
                divider = false,
            ) {
                if (!openUrl(context, LICENCE_URL)) {
                    scope.launch { snackbar.showSnackbar("No browser found on this device") }
                }
            }
        }
    }
}
