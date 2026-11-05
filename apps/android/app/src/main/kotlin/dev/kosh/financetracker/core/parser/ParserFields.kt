package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.PaymentMethod
import java.math.BigDecimal

/**
 * Small, independent field extractors shared by every bank parser. Each one only
 * knows how to pull a single field out of free text, so bank parsers compose them
 * instead of each hand-rolling one giant regex.
 */
internal object ParserFields {

    private val AMOUNT_REGEX = Regex("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val ACCOUNT_REGEX = Regex("""\b(?:A/c|account|Ac)\.?\s*(?:no\.?)?\s*[Xx*]*(\d{3,18})(?!\d)""", RegexOption.IGNORE_CASE)
    private val REF_REGEX = Regex("""(?:Ref\.?\s*No\.?|RRN|UPI\s*Ref\.?\s*No\.?)[:\s]*([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
    private val BALANCE_REGEX = Regex("""(?:Avl\s*Bal|Available\s*Balance|Bal)\.?\s*:?\s*Rs\.?\s*([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
    private val VPA_REGEX = Regex("""(?:to|from)\s+VPA\s+([\w.\-]+@[\w.\-]+)""", RegexOption.IGNORE_CASE)
    private val CARD_SUFFIX_REGEX = Regex("""card\s*(?:ending|no\.?)?\s*(?:in|with)?\s*[Xx*]*(\d{4})(?!\d)""", RegexOption.IGNORE_CASE)

    fun amount(body: String): BigDecimal? =
        AMOUNT_REGEX.find(body)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()

    fun accountSuffix(body: String): String? =
        ACCOUNT_REGEX.find(body)?.groupValues?.get(1)?.takeLast(4)

    fun transactionRef(body: String): String? =
        REF_REGEX.find(body)?.groupValues?.get(1)

    fun balanceAfter(body: String): BigDecimal? =
        BALANCE_REGEX.find(body)?.groupValues?.get(1)?.replace(",", "")?.toBigDecimalOrNull()

    fun vpa(body: String): String? =
        VPA_REGEX.find(body)?.groupValues?.get(1)

    fun cardSuffix(body: String): String? =
        CARD_SUFFIX_REGEX.find(body)?.groupValues?.get(1)

    /** Merchant guess: VPA handle before '@' if present, else null (bank parsers may override). */
    fun merchantFromVpa(body: String): String? =
        vpa(body)?.substringBefore("@")

    fun confidence(
        hasAmount: Boolean,
        hasDirection: Boolean,
        hasAccount: Boolean,
        hasMerchant: Boolean,
        bankRecognized: Boolean,
    ): Double {
        var score = 0.0
        if (hasAmount) score += 0.4
        if (hasDirection) score += 0.2
        if (hasAccount) score += 0.15
        if (hasMerchant) score += 0.15
        if (bankRecognized) score += 0.1
        return score.coerceIn(0.0, 1.0)
    }

    fun paymentMethod(body: String): PaymentMethod {
        val lower = body.lowercase()
        return when {
            lower.contains("upi") || vpa(body) != null -> PaymentMethod.UPI
            lower.contains("neft") -> PaymentMethod.NEFT
            lower.contains("imps") -> PaymentMethod.IMPS
            lower.contains("rtgs") -> PaymentMethod.RTGS
            lower.contains("atm") -> PaymentMethod.CASH
            lower.contains("card") -> PaymentMethod.CARD
            lower.contains("netbanking") -> PaymentMethod.NETBANKING
            else -> PaymentMethod.UNKNOWN
        }
    }
}
