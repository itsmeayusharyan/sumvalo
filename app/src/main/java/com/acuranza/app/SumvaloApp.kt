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

    /** Repositories and services; see [AppContainer]. Throws [DatabaseUnavailableException] while
     *  the database cannot be opened securely — use [acquireContainer] on background paths. */
    val container: AppContainer by lazy { AppContainer(this) }

    /** Set once the database is known to be unavailable; the UI shows a blocking notice. */
    var dbError: String? = null
        private set

    /** [container], or null (recording [dbError]) when the database can't be opened securely. */
    fun acquireContainer(): AppContainer? = try {
        container
    } catch (e: com.acuranza.app.data.DatabaseUnavailableException) {
        if (dbError == null) dbError = e.message
        null
    }

    override fun onCreate() {
        super.onCreate()
        // Channels need only the context: building the container here would open the database on the main thread.
        com.acuranza.app.notify.Notifications(this).createChannels()
        appScope.launch {
            // Fail closed on an unavailable database: skip all startup work; MainActivity shows the notice.
            val c = acquireContainer() ?: return@launch
            com.acuranza.app.repo.PaydayLearner.start(appScope, c.db, c.settings)
            // Seeding with IGNORE adds categories introduced by an update without touching existing ones.
            c.db.categories().insertAll(DefaultCategories.all)
            runCatching {
                com.acuranza.app.repo.DataRepairs.run(c.db, c.settings, c.netWorth)
            }
            if (com.acuranza.app.BuildConfig.SMS_FULL) {
                runCatching {
                    val s = c.settings.current()
                    com.acuranza.app.work.WorkScheduler.setSmsScanEnabled(this@SumvaloApp, s.smsConsent && s.smsAutoScan)
                }
            }
            runCatching { com.acuranza.app.repo.AutoCategoriser.run(c.db) }
            runCatching { container.settings.clearLegacyGmail() }
        }
        WorkScheduler.schedule(this)
        com.acuranza.app.work.SmsForegroundTicker.start(this)
        com.acuranza.app.tools.WidgetUpdater.start(this)
        appScope.launch { runCatching { com.acuranza.app.tools.DigestSchedule.sync(this@SumvaloApp) } }
    }

}
