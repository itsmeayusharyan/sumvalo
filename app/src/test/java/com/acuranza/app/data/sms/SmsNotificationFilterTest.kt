package com.acuranza.app.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gate that decides which notifications automatic capture may even look at. Only the SMS
 * app's notifications pass; everything else in the world is ignored before any text is read.
 */
class SmsNotificationFilterTest {

    @Test
    fun smsAppPackagesPassTheGate() {
        assertTrue(SmsNotificationFilter.isSmsPackage("com.google.android.apps.messaging"))
        assertTrue(SmsNotificationFilter.isSmsPackage("com.android.mms"))
        assertTrue(SmsNotificationFilter.isSmsPackage("com.samsung.android.messaging"))
        assertTrue(SmsNotificationFilter.isSmsPackage("com.xiaomi.mms"))
    }

    @Test
    fun chatBankingAndSystemNotificationsAreIgnoredEntirely() {
        // Personal conversations and everything else must never reach the pipeline.
        assertFalse(SmsNotificationFilter.isSmsPackage("com.whatsapp"))
        assertFalse(SmsNotificationFilter.isSmsPackage("org.telegram.messenger"))
        assertFalse(SmsNotificationFilter.isSmsPackage("com.android.systemui"))
        assertFalse(SmsNotificationFilter.isSmsPackage("com.hdfcbank.bank"))
        assertFalse(SmsNotificationFilter.isSmsPackage(""))
    }

    @Test
    fun expandedBodyWinsOverCollapsedLine() {
        assertEquals("Full debit text", SmsNotificationFilter.bodyOf("Full debit text", "HDFC Bank: …"))
    }

    @Test
    fun collapsedLineIsUsedWhenThereIsNoBigText() {
        assertEquals("HDFC: Rs.250 debited", SmsNotificationFilter.bodyOf(null, "HDFC: Rs.250 debited"))
    }

    @Test
    fun blankTextIsRejected() {
        assertNull(SmsNotificationFilter.bodyOf("   ", null))
        assertNull(SmsNotificationFilter.bodyOf(null, null))
        assertNull(SmsNotificationFilter.bodyOf(null, "  "))
    }
}
