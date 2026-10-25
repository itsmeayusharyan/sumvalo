package com.acuranza.app.security

import android.net.Uri
import com.acuranza.app.SumvaloApp
import com.acuranza.app.di.AppContainer
import com.acuranza.app.repo.DataRepairs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

/**
 * Runs export and restore in the application scope, so leaving the screen, rotating the phone or switching tabs
 * mid-way can't cancel them, and finishes a restore by bringing the rest of the app in line with the restored data.
 */
class BackupFlow(private val app: SumvaloApp) {
    private val container: AppContainer get() = app.container

    suspend fun export(uri: Uri, password: CharArray): BackupStats = inAppScope {
        container.backup.export(uri, password, repairVersion = container.settings.repairVersion())
    }

    suspend fun restore(uri: Uri, password: CharArray): BackupStats = inAppScope {
        val result = container.backup.restore(uri, password)
        afterRestore(result.meta)
        result.stats
    }

    private suspend fun afterRestore(meta: BackupMeta) {
        val settings = container.settings
        // A backup made before a one-time data repair needs that repair applied to the restored data.
        val backupRepairs = meta.repairVersion ?: 0
        if (backupRepairs < settings.repairVersion()) {
            runCatching {
                settings.setRepairVersion(backupRepairs)
                DataRepairs.run(container.db, settings, container.netWorth)
            }
        }
        runCatching { container.insights.refresh() }
    }

    /** Runs [block] in the app scope and waits for it; the caller being cancelled doesn't stop the work. */
    private suspend fun <T> inAppScope(block: suspend () -> T): T {
        val result = CompletableDeferred<T>()
        app.appScope.launch {
            runCatching { block() }.fold(onSuccess = { result.complete(it) }, onFailure = { result.completeExceptionally(it) })
        }
        return result.await()
    }

    companion object {
    }
}
