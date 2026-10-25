package com.acuranza.app.sms

/**
 * Permissionless SMS capture: the user shares a bank message from their Messages app to Sumvalo
 * (or pastes it). This pipeline turns the shared text into a candidate import — or a rejection —
 * and is pure so every rule is unit-testable on the JVM.
 *
 * Privacy rules, enforced here and in storage:
 *  - Authentication messages (OTP/PIN/password/CVV deliveries) are NEVER processed, even when
 *    they mention money. A transaction message that merely carries "never share your OTP"
 *    boilerplate is not an authentication message and still parses.
 *  - Promotions, balance-only alerts, bill notices and non-bank text are rejected.
 *  - Ambiguous readings (below MEDIUM confidence) are excluded rather than guessed.
 *  - Only the extracted transaction fields survive the pipeline; the raw text is discarded.
 */
object SharedSmsPipeline {

    private val authPurpose = Regex(
        "(?i)\\b(?:otp|one[- ]time password|verification code|passcode|password|pin|cvv|cvc|card verification)\\b",
    )

    /** The outcome of one shared message. */
    sealed interface Result {
        /** A transaction candidate the user should confirm (HIGH or MEDIUM confidence). */
        data class Candidate(val txn: ParsedTransaction, val confidence: SmsConfidence) : Result

        /** Nothing imported; the reason is shown to the user. */
        data class Rejected(val reason: String) : Result
    }

    fun process(body: String, nowMillis: Long): Result {
        val text = body.trim()
        if (text.isEmpty()) return Result.Rejected("empty message")

        // An authentication message never becomes a transaction. A real alert that merely carries
        // "never share your OTP/PIN" boilerplate still parses: it says a transaction happened
        // (amount + debit/credit verb + account), which the OTP delivery never does.
        if (authPurpose.containsMatchIn(text) && !looksTransactional(text)) {
            return Result.Rejected("authentication message — not processed")
        }

        // The sender is unknown for shared text; the parser's body checks decide whether this is
        // financial at all (amount + money-movement verb + an account/card mention).
        return when (val parsed = SmsParser.parse(SmsMessage(sender = "", body = text, timestampMillis = nowMillis))) {
            is SmsParseResult.Transaction -> {
                val confidence = SmsConfidenceScorer.score(parsed.txn, text)
                // Ambiguous readings are excluded outright: better no transaction than a wrong one.
                if (confidence == SmsConfidence.LOW) return Result.Rejected("ambiguous message — not processed")
                Result.Candidate(parsed.txn, confidence)
            }
            is SmsParseResult.BalanceUpdate -> Result.Rejected("balance alert without a transaction")
            is SmsParseResult.CardBill -> Result.Rejected("card bill notice — add it in Card bills")
            is SmsParseResult.Ignored -> Result.Rejected(parsed.reason)
        }
    }
}
