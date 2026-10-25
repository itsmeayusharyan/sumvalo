package com.acuranza.app.sms

import com.acuranza.app.model.TxnChannel

/**
 * Confidence that a parsed SMS really is the transaction it claims to be, and not a misread.
 *
 * The parser only emits a [ParsedTransaction] when a bank sender and a money-movement pattern both
 * matched, so every candidate is plausible. The score grades how much of the message was actually
 * understood: an amount, a date inside the message, a merchant or counterparty, a reference number
 * and a recognised channel are each evidence; a candidate missing several of them may have picked
 * the wrong number as the amount, or the right amount with the wrong type, and belongs in front
 * of the user.
 *
 * [HIGH]   — the message was understood end to end; safe to import silently.
 * [MEDIUM] — mostly understood; import, but flag the row `suspicious` so it stays out of balances
 *            and stats until the user confirms it in Review.
 * [LOW]    — too little evidence; only offered during a first import the user reviews row by row,
 *            and only as a `suspicious` row.
 */
enum class SmsConfidence(val points: Int) {
    LOW(0), MEDIUM(1), HIGH(2);

    companion object {
        /** Score at or above this is HIGH. */
        const val HIGH_AT = 6

        /** Score at or above this is MEDIUM. */
        const val MEDIUM_AT = 4

        fun fromPoints(points: Int): SmsConfidence = when {
            points >= HIGH_AT -> HIGH
            points >= MEDIUM_AT -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Grades a parsed transaction by how much of the original message was understood.
 * Pure function of the parse result plus the message body, so it is unit-testable and stays
 * consistent between the first import (review-screen ordering) and background scans.
 */
object SmsConfidenceScorer {

    fun score(txn: ParsedTransaction, body: String): SmsConfidence {
        var points = 0
        // The parser never emits a missing amount, but the point keeps the arithmetic honest.
        if (txn.amount > 0) points += 2
        // A merchant/counterparty or an explicit reference number anchors the reading.
        if (!txn.merchant.isNullOrBlank()) points += 1
        if (!txn.referenceNo.isNullOrBlank()) points += 1
        // A named channel (UPI, card, ATM, IMPS…) means a format-specific rule matched, not just
        // "an amount appeared near a debit word".
        if (txn.channel != TxnChannel.OTHER) points += 1
        // A date inside the message means the reading did not have to guess from delivery time.
        if (Dates.first(body) != null) points += 1
        // A balance the bank itself reported makes a misread amount much less likely.
        if (txn.availableBalance != null || txn.availableCreditLimit != null) points += 1
        // Counter-evidence: large round numbers without a merchant or reference are often
        // reference digits misread as rupees.
        if (txn.amount >= 1_000_000 && txn.merchant.isNullOrBlank() && txn.referenceNo.isNullOrBlank()) points -= 1

        return SmsConfidence.fromPoints(points)
    }
}
