package com.acuranza.app.work

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.acuranza.app.ui.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The 10-minute scan cadence the user chose, delivered where Android honestly permits it: while
 * the app is in the foreground (its own process, no wake locks, no JobScheduler abuse) the ticker
 * runs the incremental scan every 10 minutes. Background catch-up remains the WorkManager
 * schedule, whose platform floor is 15 minutes — that limit is Android's, not a setting.
 *
 * Every tick re-checks consent, the SMS permission and foreground state, so withdrawal or
 * permission loss stops processing within one tick at most. The incremental watermark inside the
 * scan runner makes each run cheap and duplicate-free.
 */
object SmsForegroundTicker {

    private const val INTERVAL_MS = 10L * 60 * 1000

    fun start(app: com.acuranza.app.SumvaloApp) {
        if (!com.acuranza.app.BuildConfig.SMS_FULL) return
        app.appScope.launch {
            while (isActive) {
                delay(INTERVAL_MS)
                runCatching { tick(app) }
            }
        }
    }

    private suspend fun tick(app: com.acuranza.app.SumvaloApp) {
        // Foreground only: this cadence is an in-app experience, not background polling.
        if (!AppLock.isForeground) return
        val settings = app.container.settings.current()
        if (!settings.smsConsent || !settings.smsAutoScan) return
        val granted = ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val runner = app.container.smsScanRunner ?: return
        withContext(Dispatchers.IO) { runner.run(auto = true, dryRun = false) }
    }
}
