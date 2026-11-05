package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection

/**
 * Covers standard Kotak SMS shapes:
 *  - "Rs.500.00 debited from your A/c XX1234 on 28-Sep-26 to VPA merchant@ok via UPI
 *     Ref 123456789012. Avl Bal Rs 5000.00 -Kotak Bank"
 *  - "You have received Rs.500.00 in your A/c XX1234 on 28-Sep-26 from VPA sender@ok
 *     via UPI Ref 123456789012.-Kotak Bank"
 *  - "Received Rs.100.00 in your Kotak Bank AC 9996 from AMOL KALEL on 28-09-26.UPI"
 *     (name instead of a VPA handle — common for person-to-person UPI transfers)
 */
class KotakParser : TransactionParser {

    private val debitedRegex = Regex("""\bdebited\b""", RegexOption.IGNORE_CASE)
    private val creditedOrReceivedRegex = Regex("""\b(credited|received)\b""", RegexOption.IGNORE_CASE)

    // Fallback when there's no "VPA x@y" handle, just a plain name: "from/to NAME on <date>".
    private val fromToOnRegex = Regex("""(?:from|to)\s+(.+?)\s+on\s+\d""", RegexOption.IGNORE_CASE)

    override fun canParse(event: RawFinancialEvent): Boolean {
        val sender = event.sender.orEmpty().uppercase()
        return sender.contains("KOTAK") ||
            event.rawText.contains("Kotak Bank", ignoreCase = true) ||
            event.rawText.contains("Kotak Mahindra", ignoreCase = true)
    }

    override fun parse(event: RawFinancialEvent): ParsedTransaction? {
        val body = event.rawText
        if (TransactionMessageStatus.isNonCompleted(body)) return null
        val amount = ParserFields.amount(body) ?: return null

        val direction = when {
            debitedRegex.containsMatchIn(body) -> TransactionDirection.DEBIT
            creditedOrReceivedRegex.containsMatchIn(body) -> TransactionDirection.CREDIT
            else -> TransactionDirection.UNKNOWN
        }

        val merchant = ParserFields.merchantFromVpa(body)
            ?: fromToOnRegex.find(body)?.groupValues?.get(1)?.trim()
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
            bank = "Kotak",
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
