package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.categorization.CategoryEngine
import dev.kosh.financetracker.core.classifier.IncomeClassifier
import dev.kosh.financetracker.core.model.PaymentMethod
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import java.time.Instant
import java.security.MessageDigest
import java.util.Locale

/** Include the complete row identity: date-only timestamps and reference numbers
 * alone are not unique. Length prefixes avoid ambiguous field concatenation. */
private fun StatementTransaction.sourceId(): String {
    val fields = listOf(
        bank.trim().lowercase(Locale.ROOT), accountSuffix.trim(), timestamp.toString(),
        amount.stripTrailingZeros().toPlainString(), direction.name, description,
        referenceNo.orEmpty(), balanceAfter?.stripTrailingZeros()?.toPlainString().orEmpty(),
    )
    val input = fields.joinToString("") { "${it.length}:$it" }
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return "statement:" + digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

// Bank narrations starting with "UPI" follow one of several shapes, confirmed
// against real exports:
//   "UPI/<name>/<ref>/UPI"                   (Kotak)
//   "UPI-<name>-<vpa>-<bankcode>-<ref>-UPI"   (HDFC)
//   "UPI Debit-<name>-<vpa>-<ref>[-...]"      (Slice, shape A)
//   "UPI-Debit-<ref>-<name>-..."              (Slice, shape B — note Debit/Credit
//                                               and a numeric ref both come BEFORE
//                                               the name here, unlike shape A)
// The merchant is the first token, after splitting on whichever separator the
// narration uses, that isn't a structural marker ("UPI"/"UPI Debit"/"Debit"/
// "Credit") or a bare reference number. Descriptions that don't start with "UPI"
// at all (interest credits, fees, cashback) are left as-is — still usable for
// display and CategoryEngine's keyword matching, just less tidy.
private val structuralTokens = setOf("upi", "upi debit", "upi credit", "debit", "credit")

internal fun guessMerchant(description: String): String {
    val trimmed = description.trim()
    if (!trimmed.startsWith("UPI", ignoreCase = true)) return trimmed

    val separator = if (trimmed.contains('/')) '/' else '-'
    val merchant = trimmed.split(separator)
        .map { it.trim() }
        .firstOrNull { token ->
            token.isNotBlank() &&
                token.lowercase() !in structuralTokens &&
                !token.all { it.isDigit() }
        }
    return merchant ?: trimmed
}

/** Statement rows always have a known bank/account and a real timestamp — the type
 * classification and category logic mirror [dev.kosh.financetracker.core.classifier.toTransaction]
 * for SMS so a transaction looks and behaves the same regardless of where it came from. */
fun StatementTransaction.toTransaction(): Transaction {
    val merchant = guessMerchant(description)
    val type = when (direction) {
        TransactionDirection.DEBIT -> TransactionType.EXPENSE
        TransactionDirection.CREDIT ->
            if (IncomeClassifier.isLikelySalary(merchant, description)) TransactionType.INCOME else TransactionType.UNKNOWN
        TransactionDirection.UNKNOWN -> TransactionType.UNKNOWN
    }

    return Transaction(
        timestamp = timestamp,
        amount = amount,
        direction = direction,
        type = type,
        category = CategoryEngine.categorize(merchant),
        merchant = merchant,
        rawMerchant = merchant,
        accountId = null,
        accountSuffix = accountSuffix.trim(),
        bank = bank.trim(),
        paymentMethod = if (description.contains("UPI", ignoreCase = true)) PaymentMethod.UPI else PaymentMethod.UNKNOWN,
        source = TransactionSource.STATEMENT,
        sourceMessageId = sourceId(),
        rawSourceText = description,
        confidence = 1.0,
        notes = null,
        createdAt = Instant.now(),
        balanceAfter = balanceAfter,
        transactionRef = referenceNo,
    )
}
