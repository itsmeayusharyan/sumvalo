package com.acuranza.app.sms

import com.acuranza.app.model.Bank
import com.acuranza.app.model.TxnChannel
import com.acuranza.app.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Confidence grading, using the same anonymised formats as the parser suites. The score decides
 * what a background scan may import silently: HIGH goes straight in, MEDIUM lands flagged for
 * Review, LOW is only ever offered to the user.
 */
class SmsConfidenceScorerTest {

    /** A plausible receive time for bodies dated early January 2025. */
    private val received = java.time.LocalDateTime.of(2025, 1, 6, 12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun txn(
        sender: String,
        body: String,
        merchant: String? = null,
        reference: String? = null,
        channel: TxnChannel = TxnChannel.OTHER,
        balance: Long? = null,
        amount: Long = 25000L,
    ): ParsedTransaction {
        val parsed = SmsParser.parse(SmsMessage(sender, body, received))
        return (parsed as SmsParseResult.Transaction).txn.copy(
            merchant = merchant ?: (parsed.txn.merchant),
            referenceNo = reference ?: parsed.txn.referenceNo,
            channel = channel,
            amount = amount,
            availableBalance = balance,
        )
    }

    @Test
    fun upiDebitWithEverythingIsHigh() {
        val body = "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901\n" +
            "Not You? Call 18002586161"
        val txn = txn("AD-HDFCBK", body, merchant = "Swiggy", reference = "412345678901", channel = TxnChannel.UPI)
        assertEquals(SmsConfidence.HIGH, SmsConfidenceScorer.score(txn, body))
    }

    @Test
    fun cardSpendWithReportedLimitIsHigh() {
        val body = "Rs.499.00 spent on HDFC Bank Credit Card xx5678 at NETFLIX on 2025-01-04:09:00:00. Avl Lmt: Rs.85,000.00"
        val txn = txn("JD-HDFCBK-S", body, merchant = "Netflix", channel = TxnChannel.CARD, balance = null)
        assertEquals(SmsConfidence.HIGH, SmsConfidenceScorer.score(txn, body))
    }

    @Test
    fun missingMerchantAndReferenceAndDateIsLow() {
        // Only an amount was understood; nothing anchors the reading.
        val body = "Rs.250.00 debited from your account"
        val txn = txn("AD-HDFCBK", body, merchant = null, reference = null, channel = TxnChannel.OTHER)
        assertEquals(SmsConfidence.LOW, SmsConfidenceScorer.score(txn, body))
    }

    @Test
    fun largeRoundNumberWithoutAnchorsIsPenalised() {
        // A big bare round number is more likely a reference misread as rupees.
        val body = "Rs.100000.00 debited from your account"
        val txn = txn("AD-HDFCBK", body, merchant = null, reference = null, channel = TxnChannel.OTHER, amount = 10_000_000L)
        assertEquals(SmsConfidence.LOW, SmsConfidenceScorer.score(txn, body))
    }

    @Test
    fun thresholdMathMatchesTheBands() {
        assertEquals(SmsConfidence.HIGH, SmsConfidence.fromPoints(SmsConfidence.HIGH_AT))
        assertEquals(SmsConfidence.MEDIUM, SmsConfidence.fromPoints(SmsConfidence.MEDIUM_AT))
        assertEquals(SmsConfidence.LOW, SmsConfidence.fromPoints(SmsConfidence.MEDIUM_AT - 1))
        assertEquals(SmsConfidence.HIGH, SmsConfidence.fromPoints(SmsConfidence.HIGH_AT + 3))
    }

    @Test
    fun incomeTypeDoesNotAffectTheScore() {
        val debit = txn(
            "AD-HDFCBK",
            "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901",
            merchant = "Swiggy", reference = "412345678901", channel = TxnChannel.UPI,
        )
        val credit = debit.copy(type = TxnType.INCOME)
        val body = "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901"
        assertEquals(SmsConfidenceScorer.score(debit, body), SmsConfidenceScorer.score(credit, body))
    }

    @Test
    fun bankFieldIsPreservedThroughCopy() {
        val body = "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901"
        val txn = txn("AD-HDFCBK", body)
        assertEquals(Bank.HDFC, txn.bank)
        assertEquals(LocalDate.of(2025, 1, 5), LocalDate.ofInstant(java.time.Instant.ofEpochMilli(txn.timestampMillis), ZoneId.systemDefault()))
    }
}
