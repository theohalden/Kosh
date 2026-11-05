package dev.kosh.financetracker.core.classifier

import dev.kosh.financetracker.core.categorization.CategoryEngine
import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionType
import java.time.Instant

/**
 * Deliberately naive DEBIT->EXPENSE mapping — this is NOT the real type classifier
 * from the plan (transfers, EMI, credit-card payments, investments are all still
 * misclassified as plain EXPENSE here). CREDIT is narrower: "credit does not
 * automatically mean income" (finance integrity rule) — only a recognizable
 * salary/payroll credit becomes INCOME; everything else stays UNKNOWN rather than
 * inflating the Income figure with refunds, P2P transfers-in, etc. Category comes
 * from the keyword-rule CategoryEngine tier.
 */
fun ParsedTransaction.toTransaction(sourceMessageId: String, rawSourceText: String): Transaction {
    val type = when (direction) {
        TransactionDirection.DEBIT -> TransactionType.EXPENSE
        TransactionDirection.CREDIT ->
            if (IncomeClassifier.isLikelySalary(merchantRaw, rawSourceText)) {
                TransactionType.INCOME
            } else {
                TransactionType.UNKNOWN
            }
        TransactionDirection.UNKNOWN -> TransactionType.UNKNOWN
    }

    val now = Instant.now()
    return CardTransactionClassifier.project(Transaction(
        timestamp = timestamp,
        amount = amount,
        direction = direction,
        type = type,
        category = CategoryEngine.categorize(merchantRaw),
        merchant = merchantRaw,
        rawMerchant = merchantRaw,
        accountId = null,
        accountSuffix = accountSuffix,
        bank = bank,
        paymentMethod = paymentMethod,
        source = TransactionSource.SMS,
        sourceMessageId = sourceMessageId,
        rawSourceText = rawSourceText,
        confidence = confidence,
        notes = null,
        createdAt = now,
        balanceAfter = balanceAfter,
        transactionRef = transactionRef,
    ))
}
