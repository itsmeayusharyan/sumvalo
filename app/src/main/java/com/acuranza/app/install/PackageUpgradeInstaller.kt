package com.acuranza.app.install

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The standard build's path to the full build: the user picks an Sumvalo APK from storage,
 * the app verifies it is genuinely this app (package name + signing certificate + version),
 * and stages it through Android's PackageInstaller session — the same installer-session flow
 * third-party installers use, where Android's own install confirmation runs and the user
 * stays in control of every step, including any security-check prompt the system raises.
 *
 * Nothing here disables or evades any check: the picked APK's permissions are declared in it,
 * the user confirms the install through Android's dialog, and the security scan runs as usual.
 *
 * Session results are published to a process-scoped [events] flow rather than a screen-owned
 * callback: the screen can be recreated (rotation, memory pressure) or the process killed
 * while Android's installer dialog is open, and the outcome must still reach whatever UI is
 * alive afterwards instead of dying with one composition.
 */
class PackageUpgradeInstaller(private val context: Context) {

    /** What the picked file turned out to be, before anything is staged. */
    sealed interface Verified {
        data class Ok(val versionCode: Long, val versionName: String?) : Verified
        data class Rejected(val reason: PackageUpgradeValidator.Decision) : Verified
    }

    /**
     * The signing certificates of a PackageInfo across API levels: SigningInfo exists only from
     * 28, so 26–27 fall back to the deprecated-but-functional signatures field, with the matching
     * request flag. Without this the in-app installer would see zero certificates on Android 8.x
     * and reject its own legitimate APK.
     */
    @Suppress("DEPRECATION")
    private fun signingCertsOf(info: android.content.pm.PackageInfo): Array<out android.content.pm.Signature> =
        if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners.orEmpty()
        else info.signatures.orEmpty()

    private val signingFlags: Int =
        if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    /** Reads the picked APK's manifest and checks it against this app. */
    fun verify(apkFile: File): Verified {
        val info = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, signingFlags)
            ?: return Verified.Rejected(PackageUpgradeValidator.Decision.NotAnApk)
        val own = runCatching {
            context.packageManager.getPackageInfo(context.packageName, signingFlags)
        }.getOrNull() ?: return Verified.Rejected(PackageUpgradeValidator.Decision.NotAnApk)

        val pickedCerts = signingCertsOf(info)
        val ownCerts = signingCertsOf(own)
        val signaturesMatch = pickedCerts.isNotEmpty() && pickedCerts.size == ownCerts.size &&
            pickedCerts.zip(ownCerts).all { (a, b) -> a.toByteArray().contentEquals(b.toByteArray()) }

        val decision = PackageUpgradeValidator.decide(
            pickedPackage = info.packageName,
            pickedVersionCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
            currentPackage = context.packageName,
            currentVersionCode = own.versionCode,
            signaturesMatch = signaturesMatch,
        )
        return if (decision == PackageUpgradeValidator.Decision.Ok) {
            Verified.Ok(
                if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong(),
                info.versionName,
            )
        } else {
            Verified.Rejected(decision)
        }
    }

    /**
     * Stages the verified APK as an in-place update through a PackageInstaller session and
     * commits it. Android answers on [InstallResultReceiver], which publishes to [events];
     * a pending user action launches the system's install confirmation, where the user —
     * and any security check — has the final word.
     *
     * @return null when the session was committed and the outcome will arrive via [events],
     *   or a human-readable staging error otherwise (session creation, disk write).
     */
    fun install(apkFile: File): String? {
        var sessionId = -1
        val stagingError = runCatching {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)
            session.openWrite("acuranza-update", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { it.copyTo(out) }
            }
            val confirm = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java).putExtra(EXTRA_SESSION, sessionId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(confirm.intentSender)
            null
        }.getOrElse { it.message ?: it.javaClass.simpleName }
        // The session owns its bytes after commit; the picked copy is dead weight either way.
        if (stagingError == null) runCatching { apkFile.delete() }
        else if (sessionId != -1) runCatching { context.packageManager.packageInstaller.abandonSession(sessionId) }
        return stagingError
    }

    companion object {
        const val EXTRA_SESSION = "acuranza.session"

        /**
         * Latest installer-session event, process-scoped so it outlives any single screen
         * instance. Null when no session is pending or the last event was dismissed.
         */
        internal val _events = MutableStateFlow<InstallEvent?>(null)
        val events: StateFlow<InstallEvent?> = _events

        /** Clears the current event; used to dismiss a finished failure before retrying. */
        fun reset() { _events.value = null }
    }
}

/** The outcome of an in-place upgrade session, as it should be shown to the user. */
sealed interface InstallEvent {
    /** The session is committed and Android's confirmation dialog was launched. */
    data object WaitingForConfirmation : InstallEvent

    /** The update installed; the app process is replaced moments later. */
    data object Success : InstallEvent

    /**
     * The session failed or the user cancelled. [reason] is human-readable and specific;
     * the user may pick the APK again and retry.
     */
    data class Failed(val status: Int, val reason: String) : InstallEvent
}

/**
 * Relaunches the app after a verified in-place update, from the single success callback only —
 * never on startup, so there is no restart loop. The system is about to replace this process;
 * starting the launcher activity fresh and exiting is the standard, supported self-restart.
 * If the system has already killed the process, nothing here runs and the user simply reopens
 * the app, which then comes up as the Full build on its own.
 */
private fun restartUpdatedApp(context: Context) {
    runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (intent != null) context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

/** Receives the session results, launches the user-action intent when the system asks. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        fun fail(why: String) { PackageUpgradeInstaller._events.value = InstallEvent.Failed(status, why) }
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm == null) {
                    fail("Android didn't return its install confirmation dialog")
                    return
                }
                val launched = runCatching {
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                if (launched.isFailure) {
                    fail(
                        "Couldn't open Android's install dialog (${launched.exceptionOrNull()?.javaClass?.simpleName}) " +
                            "- allow install requests for Sumvalo in system settings, then try again",
                    )
                } else {
                    PackageUpgradeInstaller._events.value = InstallEvent.WaitingForConfirmation
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // Report success only when the installed package really did change: Android says
                // SUCCESS when the session commits, so confirm the running app is now the old
                // version before telling the user (and before any restart).
                val upgraded = runCatching {
                    val installed = context.packageManager.getPackageInfo(context.packageName, 0)
                    val installedCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
                    installedCode > com.acuranza.app.BuildConfig.VERSION_CODE.toLong()
                }.getOrDefault(false)
                if (upgraded) {
                    PackageUpgradeInstaller._events.value = InstallEvent.Success
                    restartUpdatedApp(context)
                } else {
                    fail("The install reported success, but the installed version didn't change")
                }
            }
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                fail("Install cancelled — nothing was changed")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                fail("Android blocked this install${message?.let { " ($it)" } ?: ""}")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                fail("Not enough storage to install the Full build")
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                fail("This build isn't compatible with your device")
            else ->
                fail("Install failed${message?.let { " ($it)" } ?: " (status $status)"}")
        }
    }
}
