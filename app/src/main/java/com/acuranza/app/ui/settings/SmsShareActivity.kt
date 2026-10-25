package com.acuranza.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.acuranza.app.SumvaloApp

/**
 * Share target for SMS transaction tracking — the permissionless capture path. The user shares a
 * bank message from their Messages app; it is parsed here and parked as a pending candidate the
 * user confirms inside Settings → Financial tools. No SMS permission exists anywhere in the app.
 *
 * Privacy: the shared text is parsed in memory and discarded; only the extracted transaction
 * fields are stored, and nothing is imported before the user reviews and confirms it. Without
 * consent, nothing is processed at all.
 */
class SmsShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }?.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val container = (applicationContext as SumvaloApp).container
        when {
            text.isNullOrBlank() -> toast("Nothing to add")
            container.decoy -> toast("Not available in this profile")
            else -> {
                // Share targets must not touch storage on the main thread; the read+parse is
                // tiny, so we wait for it (accurate toast), but the I/O runs on the IO dispatcher.
                val outcome = kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { container.settings.current().smsConsent }.getOrDefault(false).let { consent ->
                            if (!consent) null else container.smsImports.addPending(text)
                        }
                    }
                }
                when (outcome) {
                    null -> toast("Enable SMS tracking in Settings → Financial tools first")
                    is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Added ->
                        toast("Bank message saved for review in Sumvalo")
                    is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Duplicate ->
                        toast("Already added — it's waiting in Sumvalo's review")
                    is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Rejected ->
                        toast("Not a bank transaction: ${outcome.reason.take(60)}")
                }
            }
        }
        finish()
    }

    private fun toast(message: String) = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}
