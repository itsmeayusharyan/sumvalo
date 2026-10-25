package com.acuranza.app.notify

import com.acuranza.app.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test

/** The detected-transaction alert wording: amount, direction, then merchant · bank. */
class DetectedTransactionAlertTest {

    @Test fun incomeWording() {
        assertEquals("Money received: \u20b9700.00", SmsNotificationListenerService.alertTitle(TxnType.INCOME, 70000L))
    }

    @Test fun expenseAndTransferWording() {
        assertEquals("Spent: \u20b9249.00", SmsNotificationListenerService.alertTitle(TxnType.EXPENSE, 24900L))
        assertEquals("Transfer: \u20b95,000.00", SmsNotificationListenerService.alertTitle(TxnType.TRANSFER, 5_000_00L))
    }

    @Test fun textJoinsMerchantAndBank() {
        assertEquals(
            "AYUSH ARYAN \u00b7 Slice",
            SmsNotificationListenerService.alertText(70000L, TxnType.INCOME, "AYUSH ARYAN", "Slice"),
        )
    }

    @Test fun textWithoutMerchantFallsBackToBank() {
        assertEquals(
            "Slice",
            SmsNotificationListenerService.alertText(70000L, TxnType.INCOME, null, "Slice"),
        )
    }

    @Test fun textWithNoDetailsFallsBackToReviewPrompt() {
        // Production always passes a non-null displayName; an empty one still guards the fallback.
        assertEquals(
            "Tap to review this transaction",
            SmsNotificationListenerService.alertText(70000L, TxnType.INCOME, null, ""),
        )
    }
}
