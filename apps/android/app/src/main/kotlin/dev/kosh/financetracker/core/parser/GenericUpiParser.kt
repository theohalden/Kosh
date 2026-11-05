package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection

/**
 * Fallback for any bank/UPI message that doesn't match a bank-specific parser.
 * Lower confidence by design — bank-specific parsers should win when they apply.
 */
class GenericUpiParser : TransactionParser {

    private val debitedRegex = Regex("""\b(debited|spent|sent|paid)\b""", RegexOption.IGNORE_CASE)
    private val creditedRegex = Regex("""\b(credited|received)\b""", RegexOption.IGNORE_CASE)

    override fun canParse(event: RawFinancialEvent): Boolean {
        val body = event.rawText
        val hasUpi = body.contains("UPI", ignoreCase = true) || ParserFields.vpa(body) != null
        val hasDirectionWord = debitedRegex.containsMatchIn(body) || creditedRegex.containsMatchIn(body)
        return hasUpi && hasDirectionWord
    }

    override fun parse(event: RawFinancialEvent): ParsedTransaction? {
        val body = event.rawText
        if (TransactionMessageStatus.isNonCompleted(body)) return null
        val amount = ParserFields.amount(body) ?: return null

        val direction = when {
            debitedRegex.containsMatchIn(body) -> TransactionDirection.DEBIT
            creditedRegex.containsMatchIn(body) -> TransactionDirection.CREDIT
            else -> TransactionDirection.UNKNOWN
        }

        val merchant = ParserFields.merchantFromVpa(body)
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
            bank = null,
            cardSuffix = ParserFields.cardSuffix(body),
            confidence = ParserFields.confidence(
                hasAmount = true,
                hasDirection = direction != TransactionDirection.UNKNOWN,
                hasAccount = accountSuffix != null,
                hasMerchant = merchant != null,
                bankRecognized = false,
            ),
        )
    }
}
