package com.acuranza.app.install

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pure gate before any picked APK is handed to the package installer: it must be this
 * package, signed with this key, and not older than the installed build.
 */
class PackageUpgradeValidatorTest {

    private val current = "com.acuranza.app"
    private val code = 31

    private fun decide(
        pkg: String? = current,
        version: Long? = code.toLong(),
        signaturesMatch: Boolean = true,
    ) = PackageUpgradeValidator.decide(pkg, version, current, code, signaturesMatch)

    @Test
    fun matchingApkPasses() {
        assertTrue(decide() == PackageUpgradeValidator.Decision.Ok)
        assertTrue(decide(version = code.toLong() + 1) == PackageUpgradeValidator.Decision.Ok)
    }

    @Test
    fun otherPackagesAreRejected() {
        assertTrue(decide(pkg = "com.other.app") == PackageUpgradeValidator.Decision.WrongPackage)
    }

    @Test
    fun foreignSignaturesAreRejected() {
        assertTrue(decide(signaturesMatch = false) == PackageUpgradeValidator.Decision.SignatureMismatch)
    }

    @Test
    fun olderBuildsAreRejectedAsDowngrades() {
        assertTrue(decide(version = code.toLong() - 1) == PackageUpgradeValidator.Decision.Downgrade)
    }

    @Test
    fun unreadableFilesAreRejected() {
        assertTrue(decide(pkg = null, version = null) == PackageUpgradeValidator.Decision.NotAnApk)
        assertTrue(decide(version = null) == PackageUpgradeValidator.Decision.NotAnApk)
    }
}
