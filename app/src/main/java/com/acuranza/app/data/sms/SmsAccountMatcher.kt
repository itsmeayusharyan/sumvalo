package com.acuranza.app.data.sms

import com.acuranza.app.model.AccountType
import com.acuranza.app.model.TxnChannel

/**
 * Pure account matching for SMS-derived transactions: which existing account a parsed bank
 * message belongs to, and how sure we are about it. JVM-testable without Room.
 *
 * [Exact]  — the message's last four digits are digits the account already knows (its own last4
 *            or merged otherDigits): a confident match, safe to import without a second look.
 * [ByBank] — digits didn't match, but exactly one account of the right type (card channel →
 *            credit card, else bank) belongs to the message's bank: an educated guess that the
 *            review screen shows so the user can override it.
 * [None]   — nothing matches; confirming would create a new, clearly-labelled account for that
 *            bank. The review screen flags this so nothing lands somewhere silently.
 */
object SmsAccountMatcher {

    /** The account facts the matcher needs; [CandidateAccount.allDigits] carries last4 + otherDigits. */
    data class CandidateAccount(
        val id: Long,
        val name: String,
        val bank: String,
        val type: AccountType,
        val allDigits: List<String>,
    )

    sealed interface Match {
        data class Exact(val account: CandidateAccount) : Match
        data class ByBank(val account: CandidateAccount) : Match
        data object None : Match
    }

    fun match(accounts: List<CandidateAccount>, bank: String, last4: String?, channel: TxnChannel): Match {
        val live = accounts.filterNot { it.type == AccountType.CASH || it.type == AccountType.WALLET || it.type == AccountType.BANK && it.bank.isBlank() }
        val digits = listOfNotNull(last4?.takeIf { it.isNotBlank() })
        if (digits.isNotEmpty()) {
            live.firstOrNull { a -> digits.any { it in a.allDigits } }?.let { return Match.Exact(it) }
        }
        val wantedType = if (channel == TxnChannel.CARD) AccountType.CREDIT_CARD else AccountType.BANK
        val byBank = live.filter { it.bank == bank && it.type == wantedType }
        return when (byBank.size) {
            0 -> Match.None
            1 -> Match.ByBank(byBank.single())
            else -> {
                // Several accounts of this bank and type: the digits would have told them apart.
                // Not confident enough to guess.
                Match.None
            }
        }
    }
}
