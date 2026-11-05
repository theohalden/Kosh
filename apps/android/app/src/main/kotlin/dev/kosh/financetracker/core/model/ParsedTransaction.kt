package dev.kosh.financetracker.core.model

import java.math.BigDecimal
import java.time.Instant

/**
 * What a [dev.kosh.financetracker.core.parser.TransactionParser] extracts from a
 * raw message. Deliberately narrower than [Transaction]: it has no [TransactionType]
 * or [TransactionCategory] yet, because those are decided later by the classifier
 * and category engine, working across the whole transaction, not just this SMS.
 */
data class ParsedTransaction(
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val accountSuffix: String?,
    val merchantRaw: String?,
    val transactionRef: String?,
    val paymentMethod: PaymentMethod?,
    val balanceAfter: BigDecimal?,
    val timestamp: Instant,
    val bank: String?,
    val cardSuffix: String?,
    val confidence: Double,
)
