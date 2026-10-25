package com.acuranza.app.model

/**
 * Banks and card issuers the app recognises. Parsers are tuned for ICICI, HDFC, Kotak, DBS, SBI and Axis;
 * PNB, Bank of Baroda, Canara, Union Bank, IDFC FIRST, IndusInd, Yes Bank, AU Small Finance Bank and
 * Federal Bank are identified by sender and parsed with the generic rules. Everything else is [OTHER].
 */
enum class Bank(val displayName: String, val shortName: String, val brandColor: Long) {
    ICICI("ICICI Bank", "ICICI", 0xFFB02A30),
    HDFC("HDFC Bank", "HDFC", 0xFF004C8F),
    KOTAK("Kotak Mahindra Bank", "Kotak", 0xFFED1C24),
    DBS("DBS Bank", "DBS", 0xFFE20613),
    SBI("State Bank of India", "SBI", 0xFF22409A),
    AXIS("Axis Bank", "Axis", 0xFF97144D),
    PNB("Punjab National Bank", "PNB", 0xFFA20E37),
    BOB("Bank of Baroda", "BoB", 0xFFF26522),
    CANARA("Canara Bank", "Canara", 0xFF0089CF),
    UNION("Union Bank of India", "Union", 0xFFDA251D),
    IDFC("IDFC FIRST Bank", "IDFC First", 0xFF9C1D26),
    INDUSIND("IndusInd Bank", "IndusInd", 0xFF8B2332),
    YES("Yes Bank", "Yes", 0xFF0E5CAD),
    AU("AU Small Finance Bank", "AU", 0xFF652D90),
    FEDERAL("Federal Bank", "Federal", 0xFF173F8A),
    IDBI("IDBI Bank", "IDBI", 0xFF6E2B8B),
    BANDHAN("Bandhan Bank", "Bandhan", 0xFFF47B20),
    RBL("RBL Bank", "RBL", 0xFF8B1D41),
    KARNATAKA("Karnataka Bank", "Karnataka", 0xFFB02A30),
    J_AND_K("J&K Bank", "J&K", 0xFFC8102E),
    KARUR_VYSYA("Karur Vysya Bank", "KVB", 0xFF7B2D26),
    HSBC("HSBC India", "HSBC", 0xFFDB0011),
    CITIBANK("Citibank India", "Citi", 0xFF003B70),
    STANDARD_CHARTERED("Standard Chartered", "StanChart", 0xFF00625E),
    AMERICAN_EXPRESS("American Express", "Amex", 0xFF006FCF),
    BANK_OF_INDIA("Bank of India", "BoI", 0xFFFF6D00),
    CENTRAL_BANK("Central Bank of India", "Central", 0xFF0072BC),
    UCO("UCO Bank", "UCO", 0xFFF7A800),
    BANK_OF_MAHARASHTRA("Bank of Maharashtra", "Mahabank", 0xFFE87722),
    PAYTM_BANK("Paytm Payments Bank", "Paytm", 0xFF00BAF2),
    AIRTEL_BANK("Airtel Payments Bank", "Airtel", 0xFFE40000),
    JIO_BANK("Jio Payments Bank", "Jio", 0xFF0A2885),
    INDIA_POST_BANK("India Post Payments Bank", "IPPB", 0xFFB41118),
    SLICE("Slice", "Slice", 0xFF201E50),
    OTHER("Other", "Other", 0xFF5F6B7A);

    companion object {
        fun fromName(name: String?): Bank = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

enum class TxnType { EXPENSE, INCOME, TRANSFER }

enum class TxnChannel { UPI, CARD, NEFT, IMPS, RTGS, ATM, NETBANKING, AUTO_DEBIT, CHEQUE, OTHER }

enum class AccountType { BANK, CREDIT_CARD, CASH, WALLET }

enum class TxnSource { MANUAL, SMS, EMAIL, AUTO }
