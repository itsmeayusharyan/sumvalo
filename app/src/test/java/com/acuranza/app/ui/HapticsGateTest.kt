package com.acuranza.app.ui

import com.acuranza.app.data.prefs.Settings
import com.acuranza.app.ui.common.AppHaptics
import com.acuranza.app.ui.common.HapticIntensity
import com.acuranza.app.ui.common.HapticSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gates and the strength mapping of app-generated haptics, verified against a recording
 * fake trigger. The old suite asserted the BUG (Light and Standard mapping to the same View
 * feedback constant); this one pins the fix: every strength produces a distinct vibration.
 */
class HapticsGateTest {

    /** Records the specs the app would hand the Vibrator. */
    private class Recording : MutableList<HapticSpec> by mutableListOf() {
        val fire: (HapticSpec) -> Unit = { add(it) }
    }

    private fun haptics(
        rec: Recording,
        enabled: Boolean = true,
        keypad: Boolean = true,
        actions: Boolean = true,
        tabsAndButtons: Boolean = true,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        amplitudeSupported: Boolean = true,
    ) = AppHaptics(enabled, keypad, actions, tabsAndButtons, intensity, amplitudeSupported, rec.fire)

    @Test fun disabledSuppressesEverything() {
        val rec = Recording()
        val haptics = haptics(rec, enabled = false)
        haptics.tick()
        haptics.confirm()
        haptics.keypadTick()
        haptics() // legacy call-site form
        assertEquals(0, rec.size)
    }

    @Test fun masterOffKeepsCategoriesSilentButChoicesAreOnlyAGateAway() {
        // Individual preferences are stored independently; the master switch suppresses only.
        val rec = Recording()
        val haptics = haptics(rec, enabled = false, keypad = true, actions = true, tabsAndButtons = true)
        haptics.tick(); haptics.confirm(); haptics.keypadTick()
        assertEquals(0, rec.size)
        val restored = haptics(rec, enabled = true, keypad = true, actions = true, tabsAndButtons = true)
        restored.tick(); restored.confirm(); restored.keypadTick()
        assertEquals(3, rec.size)
    }

    @Test fun categorySwitchesSuppressOnlyTheirOwnFeedback() {
        val rec = Recording()
        val haptics = haptics(rec, enabled = true, keypad = false, actions = false, tabsAndButtons = true)
        haptics.tick(); haptics.confirm(); haptics.keypadTick()
        assertEquals(1, rec.size)
    }

    @Test fun everyStrengthVibratesDifferentlyWithAmplitudeControl() {
        // THE FIX: three strengths, three distinct (amplitude, duration) specs — the old
        // implementation made Light and Standard byte-identical.
        val specs = HapticIntensity.entries.map { HapticSpec.tick(it, amplitudeSupported = true) }
        assertEquals(3, specs.map { it.amplitude to it.durationMs }.distinct().size)
        assertEquals(listOf(36, 128, 255), specs.map { it.amplitude })
    }

    @Test fun everyStrengthVibratesDifferentlyWithoutAmplitudeControl() {
        // Devices without amplitude control still feel the strength: duration carries it.
        val specs = HapticIntensity.entries.map { HapticSpec.tick(it, amplitudeSupported = false) }
        assertEquals(3, specs.map { it.durationMs }.distinct().size)
        assertTrue(specs.all { it.amplitude == android.os.VibrationEffect.DEFAULT_AMPLITUDE })
    }

    @Test fun confirmIsFirmerThanTickAtEveryStrength() {
        for (i in HapticIntensity.entries) {
            for (amp in listOf(true, false)) {
                val tick = HapticSpec.tick(i, amp)
                val confirm = HapticSpec.confirm(i, amp)
                assertEquals(tick.amplitude, confirm.amplitude)
                assertTrue("confirm should outlast tick for $i", confirm.durationMs > tick.durationMs)
                assertTrue(confirm.durationMs <= 90) // never a long buzz
            }
        }
    }

    @Test fun theAppAppliesTheSelectedStrengthEverywhereItVibrates() {
        // tick, confirm and keypadTick all carry the same strength — no call site bypasses it.
        for (i in HapticIntensity.entries) {
            val rec = Recording()
            val haptics = haptics(rec, intensity = i, amplitudeSupported = true)
            haptics.tick(); haptics.confirm(); haptics.keypadTick()
            val ticks = listOf(rec[0], rec[2]).map { it.amplitude }
            assertEquals(listOf(HapticSpec.tick(i, true).amplitude, HapticSpec.tick(i, true).amplitude), ticks)
            assertEquals(HapticSpec.confirm(i, true).amplitude, rec[1].amplitude)
        }
    }

    @Test fun unknownIntensityFallsBackToStandard() {
        assertEquals(HapticIntensity.STANDARD, HapticIntensity.of("NOPE"))
        assertEquals(HapticIntensity.LIGHT, HapticIntensity.of("LIGHT"))
        assertEquals(HapticIntensity.STANDARD, HapticIntensity.of(null))
    }

    @Test fun canVibrateReflectsTheHardwareAndTheMasterSwitch() {
        val rec = Recording()
        assertEquals(true, haptics(rec, enabled = true).canVibrate())
        assertEquals(false, haptics(rec, enabled = false).canVibrate())
        val noMotor = AppHaptics(enabled = true, fire = rec.fire, canFire = { false })
        assertEquals(false, noMotor.canVibrate())
    }

    @Test fun hapticsDefaultToOn() {
        assertTrue(Settings().hapticsEnabled)
    }
}
