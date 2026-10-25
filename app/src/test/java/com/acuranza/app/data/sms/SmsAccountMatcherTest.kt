package com.acuranza.app.data.sms

import com.acuranza.app.model.AccountType
import com.acuranza.app.model.TxnChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Account matching for SMS-derived transactions — the logic that decides where a parsed bank
 * message lands and how confidently, before the user confirms anything.
 */
class SmsAccountMatcherTest {

    private fun account(id: Long, name: String, bank: String, type: AccountType, vararg digits: String) =
        SmsAccountMatcher.CandidateAccount(id, name, bank, type, digits.toList())

    @Test
    fun digitsMatchWinsConfidently() {
        val hdfc = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK, "1234")
        val result = SmsAccountMatcher.match(listOf(hdfc), "HDFC Bank", "1234", TxnChannel.UPI)
        assertEquals(1L, (result as SmsAccountMatcher.Match.Exact).account.id)
    }

    @Test
    fun digitsMatchAlsoRecognisesMergedDigits() {
        // "otherDigits": a debit card's last four merged into the bank account
        val hdfc = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK, "1234", "5678")
        val result = SmsAccountMatcher.match(listOf(hdfc), "HDFC Bank", "5678", TxnChannel.CARD)
        assertTrue(result is SmsAccountMatcher.Match.Exact)
    }

    @Test
    fun bankFallbackIsALowerConfidenceMatch() {
        val hdfc = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK)
        val result = SmsAccountMatcher.match(listOf(hdfc), "HDFC Bank", null, TxnChannel.UPI)
        assertEquals(1L, (result as SmsAccountMatcher.Match.ByBank).account.id)
    }

    @Test
    fun cardChannelPrefersCreditCardAccounts() {
        // A card spend on a bank whose credit card is set up: the guess must be the card, not the bank a/c.
        val bank = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK)
        val card = account(2, "HDFC Card", "HDFC Bank", AccountType.CREDIT_CARD)
        val result = SmsAccountMatcher.match(listOf(bank, card), "HDFC Bank", null, TxnChannel.CARD)
        assertEquals(2L, (result as SmsAccountMatcher.Match.ByBank).account.id)
    }

    @Test
    fun twoAccountsOfTheSameBankIsNotConfidentEnough() {
        val a = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK)
        val b = account(2, "Savings", "HDFC Bank", AccountType.BANK)
        val result = SmsAccountMatcher.match(listOf(a, b), "HDFC Bank", null, TxnChannel.UPI)
        assertTrue(result is SmsAccountMatcher.Match.None)
    }

    @Test
    fun unknownBankCreatesNoMatchAndTheUiFlagsANewAccount() {
        val hdfc = account(1, "Salary a/c", "HDFC Bank", AccountType.BANK)
        val result = SmsAccountMatcher.match(listOf(hdfc), "Kotak Mahindra Bank", "9999", TxnChannel.UPI)
        assertTrue(result is SmsAccountMatcher.Match.None)
    }

    @Test
    fun cashAndWalletAccountsNeverTakeBankMessages() {
        val cash = account(1, "Wallet", "Paytm", AccountType.WALLET, "1234")
        val result = SmsAccountMatcher.match(listOf(cash), "Paytm", "1234", TxnChannel.UPI)
        assertTrue(result is SmsAccountMatcher.Match.None)
    }
}
