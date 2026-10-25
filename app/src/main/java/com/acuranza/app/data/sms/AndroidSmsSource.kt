package com.acuranza.app.data.sms

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.acuranza.app.repo.ApprovedSmsImport
import com.acuranza.app.repo.SmsImportRepository
import com.acuranza.app.sms.MerchantNormalizer
import com.acuranza.app.sms.SmsConfidence

/** Android side of scanning: the inbox source and the runner that ties everything together. */

/**
 * Reads the device inbox through the SMS content provider. Only the four fields Sumvalo needs
 * are projected; the body is used to parse and then dropped — it is never written anywhere.
 */
class AndroidSmsInboxSource(private val resolver: ContentResolver) : SmsInboxSource {
    /** Page size for inbox scans; a very large inbox streams through in batches. */
    private val page = 500

    override suspend fun query(sinceMillis: Long): List<InboxSms> {
        val out = mutableListOf<InboxSms>()
        var offset = 0
        val seenFirsts = HashSet<Triple<Long, String, String>>()
        // DESC order + date > since: walk in pages until a short page signals the end.
        while (true) {
            val got = resolver.query(
                Telephony.Sms.Inbox.CONTENT_URI.buildUpon().encodedQuery(
                    "limit=$offset,$page",
                ).build(),
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.DATE_SENT),
                "${Telephony.Sms.DATE} > ?",
                arrayOf(sinceMillis.toString()),
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val dateSent = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE_SENT)
                val pageRows = mutableListOf<InboxSms>()
                while (cursor.moveToNext()) {
                    val sender = cursor.getString(address) ?: continue
                    val text = cursor.getString(body) ?: continue
                    val received = cursor.getLong(date)
                    val sent = cursor.getLong(dateSent).takeIf { it > 0 } ?: received
                    pageRows += InboxSms(sender = sender, body = text, sentMillis = sent, receivedMillis = received)
                }
                pageRows
            } ?: emptyList()
            out += got
            // A page came up short: end of results. A page came up LONG: this provider ignores
            // the limit parameter and already returned everything — stop rather than re-read.
            if (got.size != page) break
            // Each page must start further down the result than the one before; an identical
            // page-first row means this provider repeats page one for every offset — stop.
            val pageFirst = Triple(got.first().receivedMillis, got.first().sender, got.first().body)
            if (!seenFirsts.add(pageFirst)) break
            offset += page
        }
        return out
    }

}

/** Whether the SMS permission is currently granted; a revoke flips this and every scan stops. */
fun Context.hasSmsPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

/** A candidate as the review screen sees it: the graded parse plus Sumvalo's suggestions. */
data class ScanCandidate(
    val graded: GradedCandidate,
    /** The account that already matches, if any; resolved (and created, when needed) at import. */
    val matchedAccountId: Long?,
    val suggestedCategory: String,
)

/**
 * One place that runs a scan end to end: gate checks, inbox read, parse, dedup, import and the
 * settings bookkeeping. The background worker and the UI's buttons all call this, so there is
 * exactly one behaviour to test and audit.
 */
class SmsScanRunner(
    private val context: Context,
    private val repo: SmsImportRepository,
    private val settings: com.acuranza.app.data.prefs.SettingsStore,
) {
    /** How far back each background scan re-reads so interrupted runs never skip messages. */
    private val OVERLAP_MILLIS = 24L * 60 * 60 * 1000

    private val source = AndroidSmsInboxSource(context.contentResolver)

    data class Result(
        val decision: SmsScanGate.Decision,
        val imported: Int,
        val skipped: Int,
        val candidates: Int,
        /** The gate's outcome, for the status line ("ok", "no permission", "no consent"…). */
        val status: String,
    )

    /**
     * Scans for new messages. With [dryRun] the candidates are returned and nothing is saved —
     * the first import shows them for review before anything is imported. Background and manual
     * scans import HIGH and MEDIUM confidence directly (MEDIUM lands as `suspicious`, so Review
     * still guards balances); LOW is only ever offered to the user, never imported silently.
     */
    suspend fun run(
        auto: Boolean,
        dryRun: Boolean = false,
        windowMillis: Long = SmsScanOrchestrator.FIRST_IMPORT_WINDOW_MILLIS,
    ): Pair<Result, List<ScanCandidate>> {
        val startedAt = System.currentTimeMillis()
        val current = settings.current()
        val decision = if (auto) {
            SmsScanGate.check(current.smsConsent, context.hasSmsPermission(), current.smsAutoScan)
        } else {
            SmsScanGate.checkManual(current.smsConsent, context.hasSmsPermission())
        }
        if (!decision.allowed) {
            if (current.smsLastScanStatus != decision.reason) settings.recordSmsScan(decision.reason)
            return Result(decision, 0, 0, 0, decision.reason) to emptyList()
        }

        // Overlap the window: a scan interrupted mid-run never loses messages to a moved
        // watermark, because the next scan re-reads the last day and fingerprints dedup it.
        val since = if (dryRun || current.smsLastScanAt == 0L) {
            System.currentTimeMillis() - windowMillis
        } else {
            current.smsLastScanAt - OVERLAP_MILLIS
        }
        val outcome = SmsScanOrchestrator.scan(source, since, repo.importedFingerprints())

        if (dryRun) {
            val candidates = outcome.candidates.map {
                ScanCandidate(
                    graded = it,
                    matchedAccountId = repo.matchAccount(it.txn),
                    suggestedCategory = repo.suggestCategory(it.txn, it.txn.merchant),
                )
            }
            // Record the scan so the next one reads only new messages plus the overlap window —
            // a dry run is a real scan for bookkeeping purposes, or every tap would re-walk
            // the entire 12-month inbox.
            settings.recordSmsScan("ok")
            if (com.acuranza.app.BuildConfig.DEBUG) {
                android.util.Log.d(
                    "SumvaloSms",
                    "scan dry=true since=$since skipped=${outcome.skipped} candidates=${candidates.size} balances=${outcome.balances.size} millis=${System.currentTimeMillis() - startedAt}",
                )
            }
            return Result(decision, 0, outcome.skipped, candidates.size, "ok") to candidates
        }

        val label = when {
            !current.smsFirstImportDone -> "First import"
            auto -> "Automatic scan"
            else -> "Manual refresh"
        }
        val importable = outcome.candidates.filter { !auto || it.confidence != SmsConfidence.LOW }
        var imported = 0
        if (importable.isNotEmpty()) {
            val items = importable.mapNotNull { graded ->
                val txn = graded.txn
                val merchant = txn.merchant?.let { runCatching { MerchantNormalizer.normalize(it) }.getOrNull() } ?: txn.merchant
                val accountId = repo.resolveAccount(txn)
                // Cross-source dedup for messages whose fingerprint can vary (no reference, no
                // in-body date): the same payment captured by the listener and by the scan.
                if (repo.hasNearDuplicateSms(accountId, txn.amount, txn.type, txn.timestampMillis)) return@mapNotNull null
                ApprovedSmsImport(
                    parsed = txn,
                    sender = graded.sender,
                    amount = txn.amount,
                    categoryKey = repo.suggestCategory(txn, merchant),
                    merchant = merchant,
                    timestampMillis = txn.timestampMillis,
                    accountId = accountId,
                    confidence = graded.confidence,
                )
            }
            imported = repo.importBatch(label, items, outcome.balances).second
        } else {
            outcome.balances.forEach { repo.applyBalance(it) }
        }

        if (!current.smsFirstImportDone && imported > 0) settings.setSmsFirstImportDone()
        settings.recordSmsScan("ok", outcome.skipped)
        if (com.acuranza.app.BuildConfig.DEBUG) {
            android.util.Log.d(
                "SumvaloSms",
                "scan dry=false auto=$auto since=$since skipped=${outcome.skipped} candidates=${importable.size} imported=$imported millis=${System.currentTimeMillis() - startedAt}",
            )
        }
        return Result(decision, imported, outcome.skipped, importable.size, "ok") to emptyList()
    }
}
