package com.acuranza.app.notify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.acuranza.app.SumvaloApp
import com.acuranza.app.data.sms.SmsNotificationFilter

/**
 * Automatic SMS capture, permissionless by design: when the user enables it (explicit consent +
 * the system's Notification Access grant), Sumvalo reads the notifications the SMS app posts for
 * incoming messages and runs them through the exact same pipeline as a shared message. This is
 * the only Android mechanism that detects new bank SMS on arrival without the SMS permission —
 * which Play Protect hard-blocks on sideloaded installs.
 *
 * The service is deliberately inert unless all three hold: consent, the in-app toggle, and the
 * system grant. Anything the pipeline rejects is discarded immediately; accepted candidates are
 * parked for the user's review. Message text is never stored.
 */
class SmsNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val container = (applicationContext as? SumvaloApp)?.container ?: return
        if (container.decoy || sbn == null) return
        if (!SmsNotificationFilter.isSmsPackage(sbn.packageName)) return

        // No consent, no toggle, no processing — checked on every notification, so withdrawing
        // consent stops capture instantly without waiting for the binding to change.
        val settings = kotlinx.coroutines.runBlocking { container.settings.current() }
        if (!settings.smsConsent || !settings.smsAutoCapture) {
            // Best effort: release the binding too, so the system stops delivering entirely.
            runCatching { requestUnbind() }
            return
        }

        val text = SmsNotificationFilter.bodyOf(
            sbn.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT),
            sbn.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT),
        ) ?: return

        // Same pipeline as share/paste: reject auth/promo/personal/ambiguous text, keep only
        // extracted transaction fields, park for review. A genuinely NEW detected transaction
        // raises Sumvalo's own alert — duplicates and rejects stay silent. This alert fires
        // from this path alone: manual entries, CSV imports and inbox scans never post it.
        kotlinx.coroutines.runBlocking {
            runCatching {
                when (val outcome = container.smsImports.addPending(text, sbn.postTime)) {
                    is com.acuranza.app.repo.SmsImportRepository.PendingOutcome.Added ->
                        Notifications(this@SmsNotificationListenerService).detectedTransaction(
                            outcome.id.toInt(),
                            alertTitle(outcome.type, outcome.amount),
                            alertText(outcome.amount, outcome.type, outcome.merchant, outcome.bank),
                        )
                    else -> Unit
                }
            }
        }
    }

    companion object {
        /** Pure presentation logic so the wording is unit-testable without Android classes. */
        internal fun alertTitle(type: com.acuranza.app.model.TxnType, amount: Long): String =
            when (type) {
                com.acuranza.app.model.TxnType.INCOME -> "Money received: ${com.acuranza.app.model.Money.format(amount)}"
                com.acuranza.app.model.TxnType.EXPENSE -> "Spent: ${com.acuranza.app.model.Money.format(amount)}"
                com.acuranza.app.model.TxnType.TRANSFER -> "Transfer: ${com.acuranza.app.model.Money.format(amount)}"
            }

        internal fun alertText(amount: Long, type: com.acuranza.app.model.TxnType, merchant: String?, bank: String): String =
            listOfNotNull(merchant, bank).joinToString(" · ").ifEmpty { "Tap to review this transaction" }
    }
}
