package com.acuranza.app.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The permissionless capture path: what a shared or pasted message may become, and — more
 * importantly — what it must never become. Anonymised formats, same as the parser suites.
 */
class SharedSmsPipelineTest {

    private val now = java.time.LocalDateTime.of(2025, 1, 6, 12, 0)
        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun process(body: String) = SharedSmsPipeline.process(body, now)

    @Test
    fun upiDebitBecomesACandidate() {
        val r = process(
            "Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/01/25\nRef 412345678901\n" +
                "Not You? Call 18002586161",
        )
        assertTrue(r is SharedSmsPipeline.Result.Candidate)
        val txn = (r as SharedSmsPipeline.Result.Candidate).txn
        assertEquals(25000L, txn.amount)
        assertEquals("Swiggy", txn.merchant)
    }

    @Test
    fun cardSpendWithLimitBecomesAHighConfidenceCandidate() {
        val r = process("Rs.499.00 spent on HDFC Bank Credit Card xx5678 at NETFLIX on 2025-01-04:09:00:00. Avl Lmt: Rs.85,000.00")
        assertTrue(r is SharedSmsPipeline.Result.Candidate)
        assertEquals(SmsConfidence.HIGH, (r as SharedSmsPipeline.Result.Candidate).confidence)
    }

    @Test
    fun otpDeliveryIsNeverProcessed() {
        val r = process("Your OTP is 458712. Valid for 10 minutes. Do not share it with anyone, including bank staff.")
        assertTrue(r is SharedSmsPipeline.Result.Rejected)
        assertTrue((r as SharedSmsPipeline.Result.Rejected).reason.contains("authentication"))
    }

    @Test
    fun pinAndPasswordMessagesAreNeverProcessed() {
        assertTrue(process("Your UPI PIN has been reset successfully. If this was not you, call 18002586161.") is SharedSmsPipeline.Result.Rejected)
        assertTrue(process("Temporary password: Xk92! Set a new one at your nearest branch.") is SharedSmsPipeline.Result.Rejected)
    }

    @Test
    fun otpBoilerplateDoesNotBlockARealDebit() {
        val r = process(
            "Rs.1,299.00 debited from a/c **1234 on 03-01-25 to VPA blinkit@hdfcbank(UPI Ref No 501234567890). " +
                "Never share your OTP. Not you? Call on 18002586161 to report",
        )
        assertTrue(r is SharedSmsPipeline.Result.Candidate)
    }

    @Test
    fun promotionsBalanceAlertsAndBillsAreRejected() {
        assertTrue(process("Get pre-approved for a personal loan of Rs.5,00,000! Apply now. T&C apply.") is SharedSmsPipeline.Result.Rejected)
        assertTrue(process("Avl Bal in A/c XX1234 is Rs.12,345.67 as of 05-01-25.") is SharedSmsPipeline.Result.Rejected)
    }

    @Test
    fun personalConversationIsRejected() {
        val r = process("Hey, I sent you Rs.500 for lunch yesterday, please check and return it soon!")
        assertTrue(r is SharedSmsPipeline.Result.Rejected)
    }

    @Test
    fun ambiguousReadingIsExcluded() {
        val r = process("Rs.250.00 debited from your account")
        assertTrue(r is SharedSmsPipeline.Result.Rejected)
        assertTrue((r as SharedSmsPipeline.Result.Rejected).reason.contains("ambiguous"))
    }

    @Test
    fun emptyAndNonFinancialTextAreRejected() {
        assertEquals("empty message", (process("   ") as SharedSmsPipeline.Result.Rejected).reason)
        assertTrue(process("Your package will arrive tomorrow between 2 and 4 PM.") is SharedSmsPipeline.Result.Rejected)
    }
}
