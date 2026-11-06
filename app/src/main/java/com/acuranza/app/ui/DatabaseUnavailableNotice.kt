package com.acuranza.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.AccentColor

/**
 * Blocking notice shown when the encrypted database cannot be opened securely (the Keystore key was
 * reset). Deliberately fail-closed: the app does nothing rather than storing new financial data
 * unencrypted. The quarantined data file — and its WAL/SHM side files — are preserved untouched.
 */
@Composable
fun DatabaseUnavailableNotice(detail: String?, icon: ImageVector = Icons.Outlined.Lock) {
    SumvaloTheme(darkTheme = isSystemInDarkTheme(), accent = AccentColor.BLUE) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp),
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(4.dp))
                Text("Sumvalo can't open", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    detail ?: "Your encrypted data couldn't be unlocked. Nothing was stored unprotected; " +
                        "the data file is preserved on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(280.dp),
                )
            }
        }
    }
}
