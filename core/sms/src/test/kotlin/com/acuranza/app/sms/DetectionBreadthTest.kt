package com.acuranza.app.sms

import com.acuranza.app.model.Bank
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Detection-breadth cases that the pre-v1.0.1 gate skipped: balance-only reports, the newer verb
 * set (charged/swiped/recharge/auto-pay), and the standing exclusion contracts (OTP, promos,
 * personal chat).
 */
class DetectionBreadthTest {

    private val at = LocalDateTime.of(2026, 10, 12, 18, 30).atZone(ZONE).toInstant().toEpochMilli()
    private fun t(sender: String, body: String) = txn(sender, body, at)

    // ---- balance-only alerts pass the gate and parse without inventing a transaction ----

    @Test fun balanceReportFromKnownSenderParsesAsBalance() {
        // Balance SMS parse as a balance report, not an invented transaction.
        val r = SmsParser.parse(sms("VM-HDFCBK", "Avl Bal Rs.18,240.55 on A/c XX4521 as on 12-10-26", at))
        org.junit.Assert.assertTrue(r is SmsParseResult.BalanceUpdate)
        r as SmsParseResult.BalanceUpdate
        assertEquals(Bank.HDFC, r.update.bank)
    }

    @Test fun balanceReportFromUnknownSenderPassesTheGate() {
        // No transaction verb — the balance pattern is the evidence. The parser files a balance,
        // not an invented transaction.
        assertTrue(isFinancialMessage("VM-XYZBNK", "Avl Bal Rs.5,100.00 on A/c XX9876 as on 12-10-26"))
    }

    @Test fun availableBalanceWordingPassesTheGate() {
        assertTrue(isFinancialMessage("AD-IOBCHN", "Available balance Rs.7,200.50 in A/c XX3311 on 12-10-26"))
    }

    // ---- the widened verb set ----

    @Test fun chargedVerbPasses() {
        assertTrue(looksTransactional("Rs.99 charged to card XX1234 on 12-10-26"))
    }

    @Test fun swipedVerbPasses() {
        assertTrue(looksTransactional("Rs.450 swiped on card XX1234 at 12-10-26"))
    }

    @Test fun rechargeVerbPasses() {
        assertTrue(looksTransactional("Rs.299 recharged to your mobile linked XX1234 on 12-10-26"))
    }

    @Test fun autoPayVerbPasses() {
        assertTrue(looksTransactional("Rs.199 auto-pay debited from A/c XX1234 on 12-10-26"))
    }

    // ---- exclusions hold ----

    @Test fun otpIsNeverFinancial() {
        // A bank header passes the sender gate; the parser must still refuse the OTP.
        val r = SmsParser.parse(sms("VM-HDFCBK", "123456 is your OTP for login. Do not share it with anyone.", at))
        org.junit.Assert.assertFalse(r is SmsParseResult.Transaction)
    }

    @Test fun promoIsNotFinancial() {
        val r = SmsParser.parse(sms("VM-HDFCBK", "Get 10% off on all offers today! Hurry, Rs.500 minimum.", at))
        org.junit.Assert.assertFalse(r is SmsParseResult.Transaction)
    }

    @Test fun personalChatIsNotFinancial() {
        assertFalse(isFinancialMessage("+919876543210", "Hey, sent you Rs.500 for the movie last night"))
    }

    // ---- rejected remain rejected (failed/declined transactions are not recorded) ----

    @Test fun declinedTransactionIsRejected() {
        val r = SmsParser.parse(sms("VM-HDFCBK", "Rs.5,000.00 declined on card XX4521 on 12-10-26 due to insufficient balance", at))
        org.junit.Assert.assertFalse(r is SmsParseResult.Transaction)
    }
}
