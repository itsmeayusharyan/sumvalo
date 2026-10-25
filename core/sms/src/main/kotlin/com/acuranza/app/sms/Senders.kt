package com.acuranza.app.sms

import com.acuranza.app.model.Bank

/**
 * Indian commercial SMS arrive from DLT headers such as "AD-HDFCBK", "VM-ICICIB", "JD-KOTAKB-S".
 * The 6-character core identifies the sender; the 2-letter prefix is the operator/circle.
 */
private val BANK_HEADERS: Map<String, Bank> = mapOf(
    "HDFCBK" to Bank.HDFC, "HDFCBN" to Bank.HDFC, "HDFCCC" to Bank.HDFC, "HDFCBA" to Bank.HDFC,
    "ICICIB" to Bank.ICICI, "ICICIT" to Bank.ICICI, "ICICIO" to Bank.ICICI, "ICICIC" to Bank.ICICI,
    "KOTAKB" to Bank.KOTAK, "KOTAK" to Bank.KOTAK, "KMBANK" to Bank.KOTAK, "KOTAKC" to Bank.KOTAK,
    "KMBLCC" to Bank.KOTAK,
    "DBSBNK" to Bank.DBS, "DBSBANK" to Bank.DBS, "DIGIBK" to Bank.DBS, "DBSIND" to Bank.DBS,
    "SBIINB" to Bank.SBI, "SBIUPI" to Bank.SBI, "SBICRD" to Bank.SBI, "ATMSBI" to Bank.SBI,
    "SBIPSG" to Bank.SBI, "CBSSBI" to Bank.SBI, "SBMSMS" to Bank.SBI,
    "AXISBK" to Bank.AXIS, "AXISBN" to Bank.AXIS, "AXISCC" to Bank.AXIS, "AXISMR" to Bank.AXIS,
    "PNBSMS" to Bank.PNB, "PUNBNK" to Bank.PNB,
    "BOBTXN" to Bank.BOB, "BOBSMS" to Bank.BOB, "BOBCRD" to Bank.BOB,
    "CANBNK" to Bank.CANARA, "CANBKS" to Bank.CANARA,
    "UNIONB" to Bank.UNION, "UBOIND" to Bank.UNION,
    "IDFCFB" to Bank.IDFC, "IDFCBK" to Bank.IDFC,
    "INDUSB" to Bank.INDUSIND, "INDUSN" to Bank.INDUSIND,
    "YESBNK" to Bank.YES, "YESBK" to Bank.YES,
    "AUBANK" to Bank.AU, "AUBNKS" to Bank.AU,
    "FEDBNK" to Bank.FEDERAL, "FEDFIB" to Bank.FEDERAL,
    "IDBIBK" to Bank.IDBI,
    "BDBLBK" to Bank.BANDHAN,
    "RBLBNK" to Bank.RBL,
    "KTKBNK" to Bank.KARNATAKA,
    "JKBANK" to Bank.J_AND_K,
    "KVBKB" to Bank.KARUR_VYSYA,
    "HSBCIN" to Bank.HSBC,
    "CITIBK" to Bank.CITIBANK,
    "SCBANK" to Bank.STANDARD_CHARTERED,
    "AMEXIN" to Bank.AMERICAN_EXPRESS,
    "BOIIND" to Bank.BANK_OF_INDIA,
    "CENTBK" to Bank.CENTRAL_BANK,
    "UCOBK" to Bank.UCO,
    "MAHBNK" to Bank.BANK_OF_MAHARASHTRA,
    "PAYTMB" to Bank.PAYTM_BANK,
    "AIRBNK" to Bank.AIRTEL_BANK,
    "JIOPBK" to Bank.JIO_BANK,
    "IPPBL" to Bank.INDIA_POST_BANK,
    "SBIBNK" to Bank.SBI,
    "SLCEIT" to Bank.SLICE,
    "SLICEN" to Bank.SLICE,
)

/** Headers of banks/cards/payment banks not modelled individually but clearly financial. */
private val OTHER_FINANCIAL_HEADERS = setOf(
    "IOBCHN", "ONECRD",
)

/**
 * Reduces a sender to its identifying core: "VK-HDFCBK-S" -> "HDFCBK", "hdfcbk" -> "HDFCBK",
 * RCS business names such as "Kotak Mahindra Bank" -> "KOTAKMAHINDRABANK".
 */
internal fun headerCore(sender: String): String {
    val up = sender.trim().uppercase()
    if (up.contains(' ')) return up.filter { it.isLetterOrDigit() }
    val parts = up.split('-', '_', '.').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0]
        // "AD-HDFCBK", "VK-HDFCBK-S": the longest token is the brand (prefix = operator, suffix = S/T/P/G)
        else -> parts.maxBy { it.length }
    }.filter { it.isLetterOrDigit() || it == '+' }
}

/**
 * Bank names that may appear inside an RCS sender name or a longer header. Names of the newer banks are
 * spelled out in full ("UNIONBANK", not "UNION"; "CANARABANK", not "CANARA", which is also Canara HSBC Life
 * and Canara Robeco MF) so that group companies and unrelated brands don't match.
 */
private val BANK_NAME_HINTS: List<Pair<String, Bank>> = listOf(
    "HDFC" to Bank.HDFC, "ICICI" to Bank.ICICI, "KOTAK" to Bank.KOTAK, "DBS" to Bank.DBS,
    "DIGIBANK" to Bank.DBS, "SBI" to Bank.SBI, "STATEBANK" to Bank.SBI, "AXIS" to Bank.AXIS,
    "PUNJABNATIONAL" to Bank.PNB, "BANKOFBARODA" to Bank.BOB, "BOBCARD" to Bank.BOB,
    "CANARABANK" to Bank.CANARA, "UNIONBANK" to Bank.UNION, "IDFCFIRST" to Bank.IDFC, "IDFCBANK" to Bank.IDFC,
    "INDUSIND" to Bank.INDUSIND, "YESBANK" to Bank.YES, "AUBANK" to Bank.AU, "AUSMALLFINANCE" to Bank.AU,
    "FEDERALBANK" to Bank.FEDERAL,
    "IDBIBANK" to Bank.IDBI,
    "BANDHANBANK" to Bank.BANDHAN,
    "RBLBANK" to Bank.RBL,
    "KARNATAKABANK" to Bank.KARNATAKA,
    "JK BANK" to Bank.J_AND_K, "JKBANK" to Bank.J_AND_K,
    "KARURVYSYA" to Bank.KARUR_VYSYA,
    "CITIBANK" to Bank.CITIBANK,
    "STANDARDCHARTERED" to Bank.STANDARD_CHARTERED,
    "AMERICANEXPRESS" to Bank.AMERICAN_EXPRESS,
    "CENTRALBANKOFINDIA" to Bank.CENTRAL_BANK,
    "BANKOFINDIA" to Bank.BANK_OF_INDIA,
    "UCOBANK" to Bank.UCO,
    "BANKOFMAHARASHTRA" to Bank.BANK_OF_MAHARASHTRA,
    "PAYTMPAYMENTSBANK" to Bank.PAYTM_BANK,
    "AIRTELPAYMENTSBANK" to Bank.AIRTEL_BANK,
    "JIOPAYMENTSBANK" to Bank.JIO_BANK,
    "INDIAPOSTPAYMENTSBANK" to Bank.INDIA_POST_BANK,
    "SLICE" to Bank.SLICE,
)

/** Header prefixes too generic to name a bank on their own ("UNIONMF", "INDUSTOWER"); only exact headers count. */
private val AMBIGUOUS_PREFIXES = setOf("UNION", "INDUS")

internal fun bankFromSenderName(sender: String): Bank? {
    val core = headerCore(sender)
    BANK_HEADERS[core]?.let { return it }
    BANK_HEADERS.entries.firstOrNull { val p = it.key.take(5); p !in AMBIGUOUS_PREFIXES && core.startsWith(p) }?.let { return it.value }
    return BANK_NAME_HINTS.firstOrNull { core.contains(it.first) }?.second
}

/** True when [sender] looks like a bank / card issuer DLT header (or RCS name) rather than a person or a brand. */
fun isLikelyFinancialSender(sender: String): Boolean {
    val core = headerCore(sender)
    if (core.isEmpty() || core.all { it.isDigit() || it == '+' }) return false
    if (core in BANK_HEADERS || core in OTHER_FINANCIAL_HEADERS) return true
    if (bankFromSenderName(sender) != null) return true
    return bankishHeader.containsMatchIn(core) && !promoHeader.containsMatchIn(core)
}

private val bankishHeader = Regex("BNK|BANK|BK$|CARD|CRD")
private val promoHeader = Regex("OFFER|PROMO|SALE|LOAN")

private val amountPattern = Regex("(?i)(?:rs[.:]?|inr|₹)\\s*[0-9][0-9,]*(?:\\.\\d{1,2})?")
private val txnVerbs = Regex(
    "(?i)\\b(debited|credited|spent|withdrawn|sent|received|paid|transferred|txn|transaction|purchase|purchased|" +
        "used for|used at|deducted|deposited|refund(?:ed)?|charged|swiped|withdraw|recharge[d]?|auto-?pay|" +
        "added to|loaded)\\b",
)
private val balanceReport = Regex("(?i)\\b(avl bal|available bal|bal rs|balance (?:is |of )?rs)")
private val accountMention = Regex(
    "(?i)\\b(a/?c|ac|acct|account|card)\\b|\\bx{2,}\\s?\\d{3,4}\\b|\\*{2,}\\d{3,4}\\b|ending\\s*(?:with\\s*)?\\d{4}|\\bupi\\b",
)

/**
 * True when the body reads like a bank alert: an amount, an account/card/UPI mention, and either a
 * transaction verb or a balance report (balance-only SMS are alerts too — the parser files them
 * without inventing a transaction).
 */
fun looksTransactional(body: String): Boolean =
    amountPattern.containsMatchIn(body) && accountMention.containsMatchIn(body) &&
        (txnVerbs.containsMatchIn(body) || balanceReport.containsMatchIn(body))

/**
 * Permissive gate used by the importer: known bank senders always pass; any other sender (DLT header,
 * RCS name, short code or number) passes when the body itself looks like a transaction alert.
 * The parser then decides what the message is.
 */
fun isFinancialMessage(sender: String, body: String): Boolean {
    if (isLikelyFinancialSender(sender)) return true
    if (!looksTransactional(body)) return false
    // Personal mobile numbers need stronger evidence (a masked account/card number or a reference),
    // so chats like "sent you Rs.500, debited from my a/c" are not imported.
    return !isPersonalNumber(sender) || maskedOrReference.containsMatchIn(body)
}

private val maskedOrReference = Regex(
    "(?i)\\bx{1,}\\s?\\d{3,4}\\b|\\*{2,}\\d{3,4}\\b|ending\\s*(?:with\\s*)?\\d{4}|\\bref(?:\\s*no)?\\.?\\s*:?\\s*\\d{6,}",
)

private fun isPersonalNumber(sender: String): Boolean {
    val digits = sender.filter { it.isDigit() }
    return sender.trim().all { it.isDigit() || it == '+' || it == ' ' || it == '-' } && digits.length >= 10
}

/**
 * Banks that Sumvalo filed under [Bank.OTHER] before it recognised them. Their transactions keep the
 * fingerprint they had then (see [BankDetector.fingerprintBank]) so that re-scanning the inbox doesn't
 * import them a second time, and the importer may keep using an existing "Other" account with the same digits.
 */
val BANKS_FORMERLY_OTHER: Set<Bank> = setOf(
    Bank.PNB, Bank.BOB, Bank.CANARA, Bank.UNION, Bank.IDFC, Bank.INDUSIND, Bank.YES, Bank.AU, Bank.FEDERAL,
    Bank.IDBI, Bank.BANDHAN, Bank.RBL, Bank.KARNATAKA, Bank.J_AND_K, Bank.KARUR_VYSYA,
    Bank.HSBC, Bank.CITIBANK, Bank.STANDARD_CHARTERED, Bank.AMERICAN_EXPRESS,
    Bank.BANK_OF_INDIA, Bank.CENTRAL_BANK, Bank.UCO, Bank.BANK_OF_MAHARASHTRA,
    Bank.PAYTM_BANK, Bank.AIRTEL_BANK, Bank.JIO_BANK, Bank.INDIA_POST_BANK,
    Bank.SLICE,
)

internal object BankDetector {
    /** Body hints of older versions, in their order; [fingerprintBank] depends on them, so don't change these. */
    private val legacyHints = listOf(
        Regex("(?i)\\bhdfc\\b") to Bank.HDFC,
        Regex("(?i)\\bicici\\b") to Bank.ICICI,
        Regex("(?i)\\bkotak\\b") to Bank.KOTAK,
        Regex("(?i)\\bdbs\\b|digibank") to Bank.DBS,
        Regex("(?i)\\bsbi\\b|state bank") to Bank.SBI,
        Regex("(?i)\\baxis\\b") to Bank.AXIS,
    )

    /** Names of the banks modelled later, matched outside UPI handles ("rahul@pnb" names the payee's bank). */
    private val newerHints = listOf(
        Regex("(?i)punjab national bank|\\bpnb\\b(?!\\s*(?:housing|metlife))") to Bank.PNB,
        Regex("(?i)bank of baroda|\\bbob ?card\\b") to Bank.BOB,
        Regex("(?i)canara bank") to Bank.CANARA,
        Regex("(?i)union bank") to Bank.UNION,
        Regex("(?i)idfc first|idfc bank") to Bank.IDFC,
        Regex("(?i)\\bindusind\\b") to Bank.INDUSIND,
        Regex("(?i)\\byes bank\\b") to Bank.YES,
        Regex("(?i)\\bau small finance|\\bau bank\\b") to Bank.AU,
        Regex("(?i)federal bank") to Bank.FEDERAL,
        Regex("(?i)idbi bank") to Bank.IDBI,
        Regex("(?i)bandhan bank") to Bank.BANDHAN,
        Regex("(?i)\\brbl bank\\b") to Bank.RBL,
        Regex("(?i)karnataka bank") to Bank.KARNATAKA,
        Regex("(?i)j&k bank|jammu & kashmir bank") to Bank.J_AND_K,
        Regex("(?i)karur vysya") to Bank.KARUR_VYSYA,
        Regex("(?i)\\bhsbc\\b") to Bank.HSBC,
        Regex("(?i)citibank") to Bank.CITIBANK,
        Regex("(?i)standard chartered") to Bank.STANDARD_CHARTERED,
        Regex("(?i)american express|\\bamex\\b") to Bank.AMERICAN_EXPRESS,
        Regex("(?i)central bank of india") to Bank.CENTRAL_BANK,
        Regex("(?i)(?<!state |central )bank of india") to Bank.BANK_OF_INDIA,
        Regex("(?i)\\buco bank\\b") to Bank.UCO,
        Regex("(?i)bank of maharashtra") to Bank.BANK_OF_MAHARASHTRA,
        Regex("(?i)paytm payments bank|paytm bank") to Bank.PAYTM_BANK,
        Regex("(?i)airtel payments bank") to Bank.AIRTEL_BANK,
        Regex("(?i)jio payments bank") to Bank.JIO_BANK,
        Regex("(?i)india post payments bank|\\bippbl\\b") to Bank.INDIA_POST_BANK,
    )

    private val vpa = Regex("\\S+@\\S+")

    /** Returns the bank, [Bank.OTHER] for unmodelled financial senders, or null when not financial. */
    fun detect(sender: String, body: String): Bank? {
        bankFromSenderName(sender)?.let { return it }
        if (!isFinancialMessage(sender, body)) return null
        val legacy = legacyHints.firstOrNull { it.first.containsMatchIn(body) }?.second
        val text = body.replace(vpa, " ")
        // A legacy hint found only inside a UPI handle ("to VPA swiggy@icici ... -Punjab National Bank")
        // gives way to a newer bank named in the text; otherwise the result is what it always was.
        if (legacy != null && legacyHints.any { it.first.containsMatchIn(text) }) return legacy
        newerHints.firstOrNull { it.first.containsMatchIn(text) }?.let { return it.second }
        return legacy ?: Bank.OTHER
    }

    /**
     * The bank written into fingerprints. For [BANKS_FORMERLY_OTHER] it is the bank older versions detected
     * for the same message (a legacy body hint, else OTHER), so messages imported back then still de-duplicate.
     */
    fun fingerprintBank(bank: Bank, body: String): Bank {
        if (bank !in BANKS_FORMERLY_OTHER) return bank
        return legacyHints.firstOrNull { it.first.containsMatchIn(body) }?.second ?: Bank.OTHER
    }
}
