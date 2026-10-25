package com.acuranza.app.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.ui.onboarding.NotificationGateBody

/**
 * Renders the gate's content directly — the denied state, which is what a user who has not granted
 * the permission must always see, in both appearances.
 */
@Composable
fun AcuranzaGateFrame(dark: Boolean) {
    AcuranzaTheme(darkTheme = dark) {
        Surface(Modifier.fillMaxSize()) {
            NotificationGateBody(granted = false, onRequest = {})
        }
    }
}
