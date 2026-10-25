package com.acuranza.app.sms

import com.acuranza.app.model.Bank
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The banks promoted out of "Other" in v1.13.24: sender headers, body hints and the dedup
 * guarantee that fingerprints written when these were still [Bank.OTHER] keep working
 * ([BankDetector.fingerprintBank] must return OTHER for the same message's old fingerprint).
 */
class NewlyModelledBanksTest {

    private val at = LocalDateTime.of(2026, 10, 12, 18, 30).atZone(ZONE).toInstant().toEpochMilli()
    private fun t(sender: String, body: String) = txn(sender, body, at)

    // ---- sender-header identification ----

    @Test fun idbiByHeader() {
        assertEquals(Bank.IDBI, BankDetector.detect("VM-IDBIBK", "Rs.500 debited from A/c XX7788 on 12-10-26. Ref 123456"))
    }

    @Test fun bandhanByHeader() {
        assertEquals(Bank.BANDHAN, BankDetector.detect("AD-BDBLBK", "Rs.1,200 debited from A/c XX4411 on 12-10-26"))
    }

    @Test fun rblByHeader() {
        assertEquals(Bank.RBL, BankDetector.detect("VM-RBLBNK", "Rs.2,000 credited to A/c XX9090 on 12-10-26"))
    }

    @Test fun hsbcCitibankScAmexByHeader() {
        assertEquals(Bank.HSBC, BankDetector.detect("AM-HSBCIN", "Rs.560.00 spent on card XX3311 on 12-10-26"))
        assertEquals(Bank.CITIBANK, BankDetector.detect("VM-CITIBK", "Rs.999 spent on card XX5522 on 12-10-26"))
        assertEquals(Bank.STANDARD_CHARTERED, BankDetector.detect("VM-SCBANK", "Rs.430 spent on card XX6633 on 12-10-26"))
        assertEquals(Bank.AMERICAN_EXPRESS, BankDetector.detect("AD-AMEXIN", "Rs.1,500 spent on card XX7788 on 12-10-26"))
    }

    @Test fun publicSectorBanksByHeader() {
        assertEquals(Bank.BANK_OF_INDIA, BankDetector.detect("VM-BOIIND", "Rs.300 debited from A/c XX1122 on 12-10-26"))
        assertEquals(Bank.CENTRAL_BANK, BankDetector.detect("VM-CENTBK", "Rs.450 debited from A/c XX3344 on 12-10-26"))
        assertEquals(Bank.UCO, BankDetector.detect("VM-UCOBK", "Rs.600 debited from A/c XX5566 on 12-10-26"))
        assertEquals(Bank.BANK_OF_MAHARASHTRA, BankDetector.detect("VM-MAHBNK", "Rs.700 debited from A/c XX7788 on 12-10-26"))
    }

    @Test fun paymentsBanksByHeader() {
        assertEquals(Bank.PAYTM_BANK, BankDetector.detect("VM-PAYTMB", "Rs.150 debited from A/c XX9911 on 12-10-26"))
        assertEquals(Bank.AIRTEL_BANK, BankDetector.detect("BV-AIRBNK", "Rs.250 debited from A/c XX2233 on 12-10-26"))
        assertEquals(Bank.JIO_BANK, BankDetector.detect("JD-JIOPBK", "Rs.350 debited from A/c XX4455 on 12-10-26"))
        assertEquals(Bank.INDIA_POST_BANK, BankDetector.detect("VM-IPPBL", "Rs.50 debited from A/c XX6677 on 12-10-26"))
    }

    @Test fun regionalBanksByHeader() {
        assertEquals(Bank.KARNATAKA, BankDetector.detect("VM-KTKBNK", "Rs.800 debited from A/c XX8899 on 12-10-26"))
        assertEquals(Bank.J_AND_K, BankDetector.detect("VM-JKBANK", "Rs.900 debited from A/c XX1010 on 12-10-26"))
        assertEquals(Bank.KARUR_VYSYA, BankDetector.detect("VM-KVBKB", "Rs.1,000 debited from A/c XX2020 on 12-10-26"))
    }

    // ---- body-hint identification (unknown sender, bank named in the text) ----

    @Test fun bandhanByBody() {
        assertEquals(Bank.BANDHAN, BankDetector.detect("VM-XYZBK", "Bandhan Bank: Rs.700 debited from A/c XX1234 on 12-10-26"))
    }

    @Test fun bankOfIndiaNotConfusedWithStateBank() {
        // "State Bank of India" must keep resolving to SBI even though it contains "bank of India".
        assertEquals(Bank.SBI, BankDetector.detect("VM-SBIBNK", "State Bank of India: Rs.100 debited from A/c XX4567 on 12-10-26"))
        assertEquals(Bank.BANK_OF_INDIA, BankDetector.detect("VM-XYZBK", "Bank of India: Rs.100 debited from A/c XX4567 on 12-10-26"))
    }

    @Test fun centralBankOfIndiaNotFiledAsBankOfIndia() {
        assertEquals(Bank.CENTRAL_BANK, BankDetector.detect("VM-XYZBK", "Central Bank of India: Rs.120 debited from A/c XX2345 on 12-10-26"))
    }

    // ---- dedup fingerprints survive the re-filing ----

    @Test fun fingerprintsFromTheOtherEraStillMatch() {
        // A message from IDBI was imported when IDBI was unmodelled: its fingerprint bank was OTHER.
        // The same message re-scanned today must fingerprint identically (no double import).
        val body = "Rs.500 debited from A/c XX7788 on 12-10-26"
        val now = BankDetector.detect("VM-IDBIBK", body)!!
        assertEquals(Bank.OTHER, BankDetector.fingerprintBank(now, body))
    }

    @Test fun establishedBanksKeepTheirOwnFingerprint() {
        val body = "Rs.250 debited from A/c XX1234 on 12-10-26"
        val hdfc = BankDetector.detect("VM-HDFCBK", body)!!
        assertEquals(Bank.HDFC, BankDetector.fingerprintBank(hdfc, body))
    }
}
