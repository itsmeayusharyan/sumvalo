package com.acuranza.app

import android.app.Application
import com.acuranza.app.data.DefaultCategories
import com.acuranza.app.di.AppContainer
import com.acuranza.app.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SumvaloApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Repositories and services; see [AppContainer]. */
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // Channels need only the context: building the container here would open the database on the main thread.
        com.acuranza.app.notify.Notifications(this).createChannels()
        appScope.launch {
            com.acuranza.app.repo.PaydayLearner.start(appScope, container.db, container.settings)
            // Seeding with IGNORE adds categories introduced by an update without touching existing ones.
            container.db.categories().insertAll(DefaultCategories.all)
            runCatching {
                com.acuranza.app.repo.DataRepairs.run(container.db, container.settings, container.netWorth)
            }
            if (com.acuranza.app.BuildConfig.SMS_FULL) {
                runCatching {
                    val s = container.settings.current()
                    com.acuranza.app.work.WorkScheduler.setSmsScanEnabled(this@SumvaloApp, s.smsConsent && s.smsAutoScan)
                }
            }
            runCatching { com.acuranza.app.repo.AutoCategoriser.run(container.db) }
            runCatching { container.settings.clearLegacyGmail() }
        }
        WorkScheduler.schedule(this)
        com.acuranza.app.work.SmsForegroundTicker.start(this)
        com.acuranza.app.tools.WidgetUpdater.start(this)
        appScope.launch { runCatching { com.acuranza.app.tools.DigestSchedule.sync(this@SumvaloApp) } }
    }

}
