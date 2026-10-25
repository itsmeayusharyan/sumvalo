package com.acuranza.app.ui

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-lock state for the running process.
 *
 * It lives outside composition so rotation, a theme change or navigation never re-locks the app or replays the
 * splash, and it is deliberately not saved: after the process dies, Acuranza always starts locked. Time away is
 * measured with [SystemClock.elapsedRealtime], which the user can't change, unlike the wall clock.
 */
object AppLock {
    const val RELOCK_AFTER_MILLIS = 60_000L

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** Whether the launch splash already played in this process. */
    @Volatile var splashShown: Boolean = false

    @Volatile private var backgroundedAt: Long = 0L

    fun markUnlocked() {
        _unlocked.value = true
    }

    /** True between the last ON_START and ON_STOP — used by the in-app SMS scan cadence. */
    @Volatile
    var isForeground: Boolean = false
        private set

    fun onBackground(now: Long = SystemClock.elapsedRealtime()) {
        backgroundedAt = now
        isForeground = false
    }

    fun onForeground(lockEnabled: Boolean, now: Long = SystemClock.elapsedRealtime()) {
        isForeground = true
        if (lockEnabled && shouldRelock(backgroundedAt, now)) _unlocked.value = false
        backgroundedAt = 0L
    }

    /** True when the app was in the background for at least [timeout] on the monotonic clock. */
    fun shouldRelock(backgroundedAt: Long, now: Long, timeout: Long = RELOCK_AFTER_MILLIS): Boolean =
        backgroundedAt > 0L && now - backgroundedAt >= timeout
}
