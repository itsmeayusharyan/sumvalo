package com.acuranza.app.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Whether app-generated haptic feedback is on. The app shell provides this from the Appearance
 * setting; design-system components can't read the app's settings store directly. Defaults to
 * true so previews and tests compose unchanged (they never perform a haptic anyway).
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * The calculator keypad's own haptic preference, provided by the app shell (master AND keypad
 * switches). Defaults to true so previews and tests compose unchanged and never vibrate anyway.
 */
val LocalKeypadHapticsEnabled = compositionLocalOf { true }

/**
 * The keypad's vibration action. Default is a no-op so previews/tests stay silent; the app shell
 * provides the real one, so the keypad vibrates through the SAME strength implementation as
 * every other interaction (it honored no strength before — hardcoded to the weakest effect).
 */
val LocalKeypadHapticTick = compositionLocalOf<() -> Unit> { {} }

/**
 * The shared tick for design-system controls (segmented tabs, chips). Default no-op for
 * previews; the shell provides the real one so these controls respect the selected strength
 * and the tabs-and-buttons preference instead of a hardcoded weakest-effect vibration.
 */
val LocalHapticTick = compositionLocalOf<() -> Unit> { {} }
