package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection

/**
 * Covers the two Slice SMS shapes seen in practice — every message Slice sends
 * ends with the fixed template suffix "- slice", which is what every Slice
 * customer's SMS carries (not specific to any one account):
 *  - "Successfully paid Rs.59 from slice a/c XX8384 to Google on 22-Sep-26 via UPI
 *     AutoPay. UMN - <id>@slc - slice"
 *  - "Rs. 1,800 sent from a/c XX8384 on 20-Sep-26 to RAVI K JAIN (UPI Ref:
 *     626309763233). Not you? Call 08048329999 - slice"
 */
class SliceParser : TransactionParser {

    private val sliceSuffixRegex = Regex("""-\s*slice\s*$""", RegexOption.IGNORE_CASE)
    private val paidOrSentRegex = Regex("""\b(paid|sent|spent|debited)\b""", RegexOption.IGNORE_CASE)
    private val creditedOrReceivedRegex = Regex("""\b(credited|received)\b""", RegexOption.IGNORE_CASE)

    // "to <merchant> on <date>" (AutoPay template) or "to <merchant> (UPI Ref: ...)".
    private val toOnRegex = Regex("""\bto\s+(.+?)\s+on\s+\d""", RegexOption.IGNORE_CASE)
    private val toParenRegex = Regex("""\bto\s+(.+?)\s*\(""", RegexOption.IGNORE_CASE)

    override fun canParse(event: RawFinancialEvent): Boolean {
        val sender = event.sender.orEmpty().uppercase()
        return sender.contains("SLICE") ||
            sliceSuffixRegex.containsMatchIn(event.rawText) ||
            event.rawText.contains("slice a/c", ignoreCase = true)
    }

    override fun parse(event: RawFinancialEvent): ParsedTransaction? {
        val body = event.rawText
        if (TransactionMessageStatus.isNonCompleted(body)) return null
        val amount = ParserFields.amount(body) ?: return null

        val direction = when {
            paidOrSentRegex.containsMatchIn(body) -> TransactionDirection.DEBIT
            creditedOrReceivedRegex.containsMatchIn(body) -> TransactionDirection.CREDIT
            else -> TransactionDirection.UNKNOWN
        }

        val merchant = ParserFields.merchantFromVpa(body)
            ?: toOnRegex.find(body)?.groupValues?.get(1)?.trim()
            ?: toParenRegex.find(body)?.groupValues?.get(1)?.trim()

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
            bank = "Slice",
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
