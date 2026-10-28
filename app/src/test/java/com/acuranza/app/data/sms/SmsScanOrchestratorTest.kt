package com.acuranza.app.data.sms

import com.acuranza.app.sms.SmsConfidence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scan pipeline end to end on the JVM, with anonymised messages: eligibility, OTP/promo
 * rejection, balance-only separation, dedup, ordering and confidence bands.
 */
class SmsScanOrchestratorTest {

    private val now = System.currentTimeMillis()

    private fun inboxOf(vararg messages: Pair<String, String>) = SmsInboxSource { _ ->
        messages.mapIndexed { i, (sender, body) ->
            InboxSms(sender = sender, body = body, sentMillis = now - i * 60_000, receivedMillis = now - i * 60_000)
        }
    }

    @Test
    fun upiCardAndCreditCandidatesAreGradedAndNewestFirst() = runBlocking {
        val outcome = SmsScanOrchestrator.scan(
            inboxOf(
                "AD-HDFCBK" to "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901\nNot You? Call 18002586161",
                "JD-HDFCBK-S" to "Rs.499.00 spent on HDFC Bank Credit Card xx5678 at NETFLIX on 2025-01-04:09:00:00. Avl Lmt: Rs.85,000.00",
                "VM-HDFCBK" to "Rs.1,299.00 debited from a/c **1234 on 03-01-25 to VPA blinkit@hdfcbank(UPI Ref No 501234567890). Not you? Call on 18002586161",
            ),
            sinceMillis = 0,
            alreadyImported = emptySet(),
            threshold = SmsConfidence.MEDIUM,
        )
        assertEquals(3, outcome.candidates.size)
        assertTrue(outcome.candidates.all { it.confidence != SmsConfidence.LOW })
        // The fixture sends the first message most recently (now - 0), the last one 2 minutes
        // older, so newest-first sorting puts the Swiggy debit on top.
        assertEquals("412345678901", outcome.candidates.first().txn.referenceNo)
        assertEquals("501234567890", outcome.candidates.last().txn.referenceNo)
    }

    @Test
    fun otpsPromosAndBalanceAlertsNeverBecomeCandidates() = runBlocking {
        val outcome = SmsScanOrchestrator.scan(
            inboxOf(
                "JD-HDFCBK" to "Your OTP is 458712. Valid for 10 minutes. Do not share with anyone.",
                "VM-HDFCBK" to "Get pre-approved for a personal loan of Rs.5,00,000! Apply now. T&C apply.",
                "AD-HDFCBK" to "Avl Bal in A/c XX1234 is Rs.12,345.67 as of 05-01-25.",
            ),
            sinceMillis = 0,
            alreadyImported = emptySet(),
            threshold = SmsConfidence.MEDIUM,
        )
        assertEquals(0, outcome.candidates.size)
        // The balance alert is separated out, not counted as a transaction.
        assertEquals(1, outcome.balances.size)
        assertTrue(outcome.skippedByReason.contains("otp"))
        assertTrue(outcome.skippedByReason.values.sum() >= 2)
    }

    @Test
    fun alreadyImportedFingerprintsAreSkipped() = runBlocking {
        val body = "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901\nNot You? Call 18002586161"
        val first = SmsScanOrchestrator.scan(inboxOf("AD-HDFCBK" to body), 0, emptySet(), threshold = SmsConfidence.MEDIUM)
        assertEquals(1, first.candidates.size)
        val second = SmsScanOrchestrator.scan(inboxOf("AD-HDFCBK" to body), 0, setOf(first.candidates.single().txn.fingerprint), threshold = SmsConfidence.MEDIUM)
        assertEquals(0, second.candidates.size)
        assertEquals(1, second.skippedByReason["already imported"])
    }

    @Test
    fun nonBankSendersAreIgnored() = runBlocking {
        val outcome = SmsScanOrchestrator.scan(
            inboxOf("TM-FLIPKT" to "Your Flipkart order is out for delivery. Rs.250.00 paid."),
            sinceMillis = 0,
            alreadyImported = emptySet(),
            threshold = SmsConfidence.MEDIUM,
        )
        assertEquals(0, outcome.candidates.size)
        assertTrue(outcome.skippedByReason.isNotEmpty())
    }

    @Test
    fun failedAndFutureTransactionsAreExcluded() = runBlocking {
        val outcome = SmsScanOrchestrator.scan(
            inboxOf(
                "AD-HDFCBK" to "Your transaction of Rs.1,200.00 on card 5678 has been declined.",
                "VM-HDFCBK" to "UPI AutoPay: Rs.299.00 will be debited from A/c XX1234 on 10-02-25.",
            ),
            sinceMillis = 0,
            alreadyImported = emptySet(),
            threshold = SmsConfidence.MEDIUM,
        )
        assertEquals(0, outcome.candidates.size)
    }
}

/** The gate is the single place that decides whether a scan may run at all. */
class SmsScanGateTest {

    @Test
    fun autoScanNeedsConsentPermissionAndToggle() {
        assertTrue(SmsScanGate.check(true, true, true).allowed)
        assertTrue(!SmsScanGate.check(false, true, true).allowed)
        assertTrue(!SmsScanGate.check(true, false, true).allowed)
        assertTrue(!SmsScanGate.check(true, true, false).allowed)
        assertEquals("no permission", SmsScanGate.check(true, false, true).reason)
        assertEquals("scanning off", SmsScanGate.check(true, true, false).reason)
    }

    @Test
    fun manualScanNeedsConsentAndPermissionOnly() {
        assertTrue(SmsScanGate.checkManual(true, true).allowed)
        assertTrue(!SmsScanGate.checkManual(false, true).allowed)
        assertTrue(!SmsScanGate.checkManual(true, false).allowed)
        assertEquals("no consent", SmsScanGate.checkManual(false, true).reason)
    }
}
