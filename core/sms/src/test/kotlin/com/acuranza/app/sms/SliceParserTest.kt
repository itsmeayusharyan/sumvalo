package com.acuranza.app.sms

import com.acuranza.app.model.Bank
import com.acuranza.app.model.TxnChannel
import com.acuranza.app.model.TxnType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two verified Slice messages the user provided, plus representative variants. Expected
 * outcomes are the user's explicit spec: the repayment is a card payment on the Slice card (never
 * salary/ordinary income), the UPI credit carries the sender, masked account, reference and
 * balance metadata, and nothing double-counts the available balance as a transaction.
 */
class SliceParserTest {

    private val at = LocalDateTime.of(2026, 10, 12, 18, 30).atZone(ZONE).toInstant().toEpochMilli()
    private fun t(sender: String, body: String) = txn(sender, body, at)

    // ---- Message A: credit-card repayment ----

    @Test fun repaymentIsACardPaymentOnTheSliceCard() {
        val t = t(
            "VM-SLCEIT",
            "Repayment of Rs.1,762.8 received for the slice credit card. The amount has been credited " +
                "to your card account - slice",
        )
        assertEquals(Bank.SLICE, t.bank)
        assertEquals(176280L, t.amount)                      // ₹1,762.80
        assertTrue(t.isCreditCard)
        // A repayment is money INTO the card — never salary or ordinary income spending.
        assertTrue(t.type == TxnType.INCOME || t.type == TxnType.TRANSFER)
        // The card-payment classification keeps it out of the salary/expense flows.
        assertTrue(t.isCardPayment)
    }

    // ---- Message B: UPI money received ----

    @Test fun upiReceivedParsesFully() {
        val t = t(
            "VM-SLCEIT",
            "Rs. 700 received in slice A/c xx0370 on 11-Oct-26 from AYUSH ARYAN via UPI " +
                "(Ref ID: 461299092846). Avl. Bal. Rs. 701.96 - slice",
        )
        assertEquals(Bank.SLICE, t.bank)
        assertEquals(70000L, t.amount)                       // ₹700.00
        assertEquals(TxnType.INCOME, t.type)                 // money received
        assertEquals("0370", t.accountLast4)                 // slice account ending 0370
        // the engine extracts merchants from the lowercased body (all banks do this)
        assertEquals("ayush aryan", t.merchant?.lowercase())              // sender
        assertEquals("461299092846", t.referenceNo)          // reference ID
        assertEquals(TxnChannel.UPI, t.channel)
    }

    // ---- balance metadata is never a transaction ----

    @Test fun availableBalanceAloneIsABalanceUpdate() {
        val r = SmsParser.parse(sms("VM-SLCEIT", "Avl. Bal. Rs. 701.96 - slice", at))
        assertTrue(r is SmsParseResult.BalanceUpdate)
    }

    // ---- exclusions hold for Slice senders ----

    @Test fun sliceOtpIsIgnored() {
        val r = SmsParser.parse(sms("VM-SLCEIT", "784512 is your OTP for slice login. Never share it.", at))
        assertTrue(r !is SmsParseResult.Transaction)
    }

    @Test fun slicePromoIsIgnored() {
        val r = SmsParser.parse(sms("VM-SLCEIT", "Get flat Rs.250 cashback on your next slice spend! T&C apply.", at))
        assertTrue(r !is SmsParseResult.Transaction)
    }

    // ---- more representative Slice formats ----

    @Test fun upiDebitParses() {
        val t = t(
            "VM-SLCEIT",
            "Rs. 249 debited from slice A/c xx0370 on 11-Oct-26 to SWIGGY via UPI (Ref ID: 461299099111). " +
                "Avl. Bal. Rs. 452.96 - slice",
        )
        assertEquals(Bank.SLICE, t.bank)
        assertEquals(24900L, t.amount)
        assertEquals(TxnType.EXPENSE, t.type)
        assertEquals("0370", t.accountLast4)
        assertEquals("swiggy", t.merchant?.lowercase())
        assertEquals("461299099111", t.referenceNo)
    }

    @Test fun duplicateMessageFingerprintsIdentically() {
        val body = "Rs. 700 received in slice A/c xx0370 on 11-Oct-26 from AYUSH ARYAN via UPI " +
            "(Ref ID: 461299092846). Avl. Bal. Rs. 701.96 - slice"
        val a = t("VM-SLCEIT", body)
        val b = t("JD-SLCEIT", body)   // different operator prefix, same bank core
        assertEquals(a.fingerprint, b.fingerprint)
    }
}
