package com.acuranza.app.install

/**
 * Pure validation for the in-app "install the full build" flow: is the APK the user picked
 * genuinely a newer-or-equal build of THIS app, signed with THIS app's key? The checks here
 * are pure so they are unit-testable; the Android-facing plumbing lives in
 * [PackageUpgradeInstaller].
 */
object PackageUpgradeValidator {

    sealed interface Decision {
        /** Safe to hand to the package installer. */
        data object Ok : Decision
        data object NotAnApk : Decision
        data object WrongPackage : Decision
        data object Downgrade : Decision
        /** The picked APK is signed by a different key — never install over this app. */
        data object SignatureMismatch : Decision
    }

    /**
     * @param pickedPackage the packageName read from the picked APK's manifest, null when unreadable
     * @param pickedVersionCode its versionCode, null when unreadable
     * @param currentPackage this app's packageName
     * @param currentVersionCode this app's versionCode
     * @param signaturesMatch true when the picked APK's signing certificate matches this app's
     */
    fun decide(
        pickedPackage: String?,
        pickedVersionCode: Long?,
        currentPackage: String,
        currentVersionCode: Int,
        signaturesMatch: Boolean,
    ): Decision = when {
        pickedPackage == null || pickedVersionCode == null -> Decision.NotAnApk
        pickedPackage != currentPackage -> Decision.WrongPackage
        !signaturesMatch -> Decision.SignatureMismatch
        pickedVersionCode < currentVersionCode -> Decision.Downgrade
        else -> Decision.Ok
    }
}
