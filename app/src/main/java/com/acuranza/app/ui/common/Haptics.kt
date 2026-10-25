package com.acuranza.app.ui.common

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Feedback strength as the device can actually express it. The View haptic-feedback constants
 * used before could not vary intensity at all (Light and Standard mapped to the same effect),
 * so strength now drives a real VibrationEffect: amplitude where the device supports amplitude
 * control, duration where it does not. Both are perceivable on real hardware.
 */
enum class HapticIntensity(val id: String) {
    LIGHT("LIGHT"), STANDARD("STANDARD"), STRONG("STRONG");

    companion object {
        fun of(id: String?): HapticIntensity = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

/**
 * The pure mapping from a requested strength to concrete vibration parameters — unit-testable
 * without a device. When the vibrator has no amplitude control the amplitude must be
 * [VibrationEffect.DEFAULT_AMPLITUDE] and only the duration carries the difference.
 */
data class HapticSpec(val durationMs: Long, val amplitude: Int) {
    companion object {
        /** Tick: the subtle feel for ordinary taps. */
        fun tick(i: HapticIntensity, amplitudeSupported: Boolean): HapticSpec = when (i) {
            HapticIntensity.LIGHT -> if (amplitudeSupported) HapticSpec(18, 36) else HapticSpec(14, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticIntensity.STANDARD -> if (amplitudeSupported) HapticSpec(26, 128) else HapticSpec(30, VibrationEffect.DEFAULT_AMPLITUDE)
            HapticIntensity.STRONG -> if (amplitudeSupported) HapticSpec(34, 255) else HapticSpec(55, VibrationEffect.DEFAULT_AMPLITUDE)
        }

        /** Confirm: the firmer feel for saves and approvals — same strength, held longer. */
        fun confirm(i: HapticIntensity, amplitudeSupported: Boolean): HapticSpec =
            tick(i, amplitudeSupported).let { it.copy(durationMs = (it.durationMs * 2).coerceAtMost(90)) }
    }
}

/**
 * The one haptic path the whole app vibrates through — ticks, confirms and the keypad all share
 * it, so the selected strength means the same thing everywhere. Gated by the app's master
 * switch and per-category preferences (the [fire] lambda only runs when a gate lets it
 * through) and, in the real factory, by the device having a vibrator and the system's
 * haptic-feedback setting. A new vibration replaces any in-flight one, so rapid taps can't
 * stack overlapping effects. The trigger is injected so the gates and strength mapping are
 * unit-testable on the JVM; the Vibrator itself is a device-only path.
 */
class AppHaptics internal constructor(
    private val enabled: Boolean,
    private val keypad: Boolean = true,
    private val actions: Boolean = true,
    private val tabsAndButtons: Boolean = true,
    private val intensity: HapticIntensity = HapticIntensity.STANDARD,
    private val amplitudeSupported: Boolean = false,
    private val fire: (HapticSpec) -> Unit = {},
    private val canFire: () -> Boolean = { true },
) {
    /** True when a vibration would actually fire — Test feedback uses this to say so honestly. */
    fun canVibrate(): Boolean = enabled && canFire()

    /** Subtle tick for ordinary taps: bottom navigation, chips, rows, icon buttons, FABs. */
    fun tick() {
        if (enabled && tabsAndButtons) fire(HapticSpec.tick(intensity, amplitudeSupported))
    }

    /** Firmer tick for meaningful moments: saves, dialog confirmations, destructive prompts. */
    fun confirm() {
        if (enabled && actions) fire(HapticSpec.confirm(intensity, amplitudeSupported))
    }

    /** The keypad's own press feedback, gated by its dedicated preference. */
    fun keypadTick() {
        if (enabled && keypad) fire(HapticSpec.tick(intensity, amplitudeSupported))
    }

    /** Existing call sites read `haptic()` = confirm. */
    operator fun invoke() = confirm()

    companion object {
        /**
         * The real construction: wires the device Vibrator (amplitude control where present),
         * the system haptic-feedback setting, and the vibrator's presence.
         */
        fun of(
            context: Context,
            enabled: Boolean,
            keypad: Boolean = true,
            actions: Boolean = true,
            tabsAndButtons: Boolean = true,
            intensity: HapticIntensity = HapticIntensity.STANDARD,
        ): AppHaptics {
            val vibrator = context.getSystemService(Vibrator::class.java)?.takeIf { it.hasVibrator() }
            val amplitude = vibrator?.hasAmplitudeControl() == true
            return AppHaptics(
                enabled = enabled, keypad = keypad, actions = actions, tabsAndButtons = tabsAndButtons, intensity = intensity,
                amplitudeSupported = amplitude,
                fire = { spec ->
                    val v = vibrator
                    if (v != null && systemHapticsOn(context)) v.vibrate(VibrationEffect.createOneShot(spec.durationMs, spec.amplitude))
                },
                canFire = { vibrator != null && systemHapticsOn(context) },
            )
        }

        private fun systemHapticsOn(context: Context): Boolean = try {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        } catch (_: Exception) {
            true // Some OEMs throw on this read; vibrating is the safer default there.
        }
    }
}

/** The app-wide haptic entry point; re-created when any preference changes, so changes apply immediately. */
@Composable
fun rememberHaptic(): AppHaptics {
    val context = LocalContext.current
    val s = com.acuranza.app.ui.common.LocalSettings.current
    return remember(context, s.hapticsEnabled, s.hapticKeypad, s.hapticActions, s.hapticTabsAndButtons, s.hapticIntensity) {
        AppHaptics.of(context, s.hapticsEnabled, s.hapticKeypad, s.hapticActions, s.hapticTabsAndButtons, HapticIntensity.of(s.hapticIntensity))
    }
}
