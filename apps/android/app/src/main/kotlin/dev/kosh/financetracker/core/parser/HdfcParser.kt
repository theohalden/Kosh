package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection

/**
 * Covers the two HDFC SMS shapes seen in practice:
 *  - "Sent Rs.119.18 From HDFC Bank A/C *5590 To swiggy@ybl On 28/09/26 Ref No 123..."
 *  - "Rs.450.00 debited from A/c XX1234 on 28-Sep-26 to VPA swiggy@ybl SWIGGY. Ref No 123..."
 *  - "Rs.49,800.00 credited to A/c XX1234 on 28-Sep-26 by NEFT. Info: SALARY. Avl Bal Rs.55,230.00"
 */
class HdfcParser : TransactionParser {

    // Case-sensitive on purpose: only matches the "Sent Rs.X To <merchant> On <date>"
    // template (capitalized "To"/"On"), not generic lowercase "credited to X on <date>" phrasing.
    private val toOnRegex = Regex("""\bTo\s+(.+?)\s+On\s+\d""")
    private val infoRegex = Regex("""Info:\s*(.+?)(?:\.|Avl|$)""", RegexOption.IGNORE_CASE)
    private val sentRegex = Regex("""\bSent\b""", RegexOption.IGNORE_CASE)
    private val debitedRegex = Regex("""\b(debited|spent|paid)\b""", RegexOption.IGNORE_CASE)
    private val creditedOrReceivedRegex = Regex("""\b(credited|received)\b""", RegexOption.IGNORE_CASE)

    override fun canParse(event: RawFinancialEvent): Boolean {
        val sender = event.sender.orEmpty().uppercase()
        return sender.contains("HDFC") || event.rawText.contains("HDFC Bank", ignoreCase = true)
    }

    override fun parse(event: RawFinancialEvent): ParsedTransaction? {
        val body = event.rawText
        if (TransactionMessageStatus.isNonCompleted(body)) return null
        val amount = ParserFields.amount(body) ?: return null

        val direction = when {
            debitedRegex.containsMatchIn(body) || sentRegex.containsMatchIn(body) -> TransactionDirection.DEBIT
            creditedOrReceivedRegex.containsMatchIn(body) -> TransactionDirection.CREDIT
            else -> TransactionDirection.UNKNOWN
        }

        val merchant = ParserFields.merchantFromVpa(body)
            ?: toOnRegex.find(body)?.groupValues?.get(1)?.trim()
            ?: infoRegex.find(body)?.groupValues?.get(1)?.trim()

        val accountSuffix = ParserFields.accountSuffix(body)

        return ParsedTransaction(
            amount = amount,
            direction = direction,
            accountSuffix = accountSuffix,
            merchantRaw = merchant,
            transactionRef = ParserFields.transactionRef(body),
            paymentMethod = ParserFields.paymentMethod(body),
            balanceAfter = ParserFields.balanceAfter(body),
            timestamp = event.timestamp,
            bank = "HDFC",
            cardSuffix = ParserFields.cardSuffix(body),
            confidence = ParserFields.confidence(
                hasAmount = true,
                hasDirection = direction != TransactionDirection.UNKNOWN,
                hasAccount = accountSuffix != null,
                hasMerchant = merchant != null,
                bankRecognized = true,
            ),
        )
    }
}
