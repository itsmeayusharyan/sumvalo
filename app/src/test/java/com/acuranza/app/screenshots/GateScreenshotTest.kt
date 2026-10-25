package com.acuranza.app.screenshots

import androidx.compose.foundation.layout.fillMaxSize
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.acuranza.app.ui.onboarding.NotificationGateScreen
import org.junit.Rule
import org.junit.Test

/**
 * The notification gate is a first-launch requirement: it must render legibly in dark mode (the
 * default appearance) and in light mode, with the permission request button and, when denied,
 * the settings help card.
 */
class GateScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        theme = "Theme.Acuranza",
    )

    @Test
    fun notification_gate_dark() = paparazzi.snapshot { AcuranzaGateFrame(dark = true) }

    @Test
    fun notification_gate_light() = paparazzi.snapshot { AcuranzaGateFrame(dark = false) }
}
