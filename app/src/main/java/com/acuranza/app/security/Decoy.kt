package com.acuranza.app.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.acuranza.app.data.DefaultCategories
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.prefs.SettingsStore
import com.acuranza.app.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.decoySettingsStore: DataStore<Preferences> by preferencesDataStore(name = "acuranza_profile_b")

/** What a sandboxed (decoy) [AppContainer] uses instead of the real database and settings. */
class DecoySandbox(
    val db: SumvaloDatabase,
    val settings: SettingsStore,
)

/**
 * The decoy profile opened by the decoy PIN. It lives only in memory: a throwaway in-memory database filled with
 * sample data and its own settings file. The real database is never opened through it.
 */
object DecoySession {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    @Volatile var container: AppContainer? = null
        private set

    private val mutex = Mutex()

    val isActive: Boolean get() = _active.value

    /** Opens the decoy profile, building and seeding it on first use in this process. */
    suspend fun activate(context: Context, themeOf: com.acuranza.app.data.prefs.ThemeMode? = null) {
        val decoy = mutex.withLock { container ?: build(context.applicationContext).also { container = it } }
        // Looks like an ordinary, set-up and locked profile.
        decoy.settings.setOnboardingDone(true)
        decoy.settings.setBiometricLock(true)
        if (themeOf != null) decoy.settings.setThemeMode(themeOf)
        _active.value = true
    }

    /** Returns to the real profile (real PIN, biometrics, or a fresh process). */
    fun deactivate() {
        _active.value = false
    }

    private suspend fun build(app: Context): AppContainer {
        val db = Room.inMemoryDatabaseBuilder(app, SumvaloDatabase::class.java).build()
        val sandbox = DecoySandbox(db, SettingsStore(app, app.decoySettingsStore))
        val c = AppContainer(app, sandbox)
        db.categories().insertAll(DefaultCategories.all)
        runCatching {
            if (c.demoData.load()) {
                c.netWorth.snapshot()
            }
        }
        return c
    }
}

