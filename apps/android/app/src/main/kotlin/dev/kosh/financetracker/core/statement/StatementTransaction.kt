package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import java.math.BigDecimal
import java.time.Instant

/**
 * A single row parsed out of a bank statement file — the statement-import analog
 * of [dev.kosh.financetracker.core.model.ParsedTransaction] for SMS. Always carries
 * a real bank name, account suffix, and running balance, since every statement row
 * has them (unlike SMS, where they're sometimes missing).
 */
data class StatementTransaction(
    val timestamp: Instant,
    val amount: BigDecimal,
    val direction: TransactionDirection,
    val description: String,
    val referenceNo: String?,
    val balanceAfter: BigDecimal?,
    val bank: String,
    val accountSuffix: String,
)
