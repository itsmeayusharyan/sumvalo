package com.acuranza.app.data.sms

import com.acuranza.app.sms.SmsConfidence
import com.acuranza.app.sms.SmsConfidenceScorer
import com.acuranza.app.sms.SmsMessage
import com.acuranza.app.sms.SmsParseResult
import com.acuranza.app.sms.SmsParser
import com.acuranza.app.sms.SmsTime

/** One message as the Android inbox stores it. */
data class InboxSms(
    val sender: String,
    val body: String,
    /** When the network sent the message; 0 when the provider omitted it. */
    val sentMillis: Long,
    /** When the phone stored the message. */
    val receivedMillis: Long,
)

/** Where scans read messages from; the ContentResolver adapter in production, a list in tests. */
fun interface SmsInboxSource {
    /** Messages received at or after [sinceMillis], newest last; empty when there is nothing new. */
    suspend fun query(sinceMillis: Long): List<InboxSms>
}

/** A scan's outcome, before anything is saved. */
data class SmsScanOutcome(
    /** Parsed transactions worth importing, newest first, each graded. */
    val candidates: List<GradedCandidate>,
    /** Balance statements the banks sent; applied to matching accounts, never transactions. */
    val balances: List<com.acuranza.app.sms.ParsedBalance>,
    /** Credit-card bill announcements; shown as a count only (bills are managed in Card bills). */
    val bills: Int,
    /** How many eligible-looking messages were skipped and why (duplicates, OTPs, promos…). */
    val skippedByReason: Map<String, Int>,
) {
    val skipped: Int get() = skippedByReason.values.sum()
}

/** A parsed transaction with its confidence and the sender id; the body is dropped here. */
data class GradedCandidate(
    val txn: com.acuranza.app.sms.ParsedTransaction,
    val sender: String,
    val confidence: SmsConfidence,
)

/**
 * Pure scan orchestration: inbox messages in, graded candidates out. No Android imports, so the
 * whole pipeline (eligibility, parsing, OTP/promo rejection, dedup, confidence) is unit-tested on
 * the JVM against anonymised formats.
 */
object SmsScanOrchestrator {

    /** How far back a first import scans; older messages are rarely worth the noise. */
    const val FIRST_IMPORT_WINDOW_MILLIS = 365L * 24 * 60 * 60 * 1000

    suspend fun scan(
        source: SmsInboxSource,
        sinceMillis: Long,
        alreadyImported: Set<String>,
        threshold: SmsConfidence = SmsConfidence.MEDIUM,
    ): SmsScanOutcome {
        val messages = source.query(sinceMillis)
        val candidates = mutableListOf<GradedCandidate>()
        val balances = mutableListOf<com.acuranza.app.sms.ParsedBalance>()
        var bills = 0
        val skipReasons = mutableMapOf<String, Int>()

        fun skip(reason: String) {
            skipReasons[reason] = (skipReasons[reason] ?: 0) + 1
        }

        for (raw in messages) {
            val msg = SmsTime.message(
                sender = raw.sender,
                body = raw.body,
                sentMillis = raw.sentMillis,
                receivedMillis = raw.receivedMillis,
                keepReceived = true,
            )
            when (val result = SmsParser.parse(msg)) {
                is SmsParseResult.Transaction -> {
                    val graded = SmsConfidenceScorer.score(result.txn, msg.body)
                    when {
                        result.txn.fingerprint in alreadyImported -> skip("already imported")
                        graded.points < threshold.points && sinceMillis > 0 -> skip("below confidence threshold")
                        else -> candidates += GradedCandidate(result.txn, raw.sender, graded)
                    }
                }
                is SmsParseResult.BalanceUpdate -> {
                    if (result.update.availableBalance != null) balances += result.update else skip("balance alert")
                }
                is SmsParseResult.CardBill -> bills++
                is SmsParseResult.Ignored -> skip(result.reason)
            }
        }
        return SmsScanOutcome(
            candidates = candidates.sortedByDescending { it.txn.timestampMillis },
            balances = balances,
            bills = bills,
            skippedByReason = skipReasons,
        )
    }
}

/**
 * The scan gate: every path to the inbox — first import, manual refresh, background worker —
 * goes through the same three checks, in the same order. Pure, so the permission-revoked and
 * consent-withdrawn behaviours are unit-testable without a device.
 */
object SmsScanGate {
    data class Decision(val allowed: Boolean, val reason: String)

    fun check(consent: Boolean, permissionGranted: Boolean, autoScanEnabled: Boolean): Decision = when {
        !consent -> Decision(false, "no consent")
        !permissionGranted -> Decision(false, "no permission")
        !autoScanEnabled -> Decision(false, "scanning off")
        else -> Decision(true, "ok")
    }

    /** First import and manual refresh need consent + permission but not the auto-scan switch. */
    fun checkManual(consent: Boolean, permissionGranted: Boolean): Decision = when {
        !consent -> Decision(false, "no consent")
        !permissionGranted -> Decision(false, "no permission")
        else -> Decision(true, "ok")
    }
}
