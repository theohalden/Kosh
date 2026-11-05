package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.parser.TransactionMessageStatus
import java.time.Duration

/** Matches unique debit/credit pairs across identified accounts using amount,
 * shared payment reference and compatible dates. Equal amounts alone are insufficient.
 * Incomplete evidence remains unresolved rather than being guessed into a pair. */
object TransferDetector {

    private val SMS_TIME_WINDOW: Duration = Duration.ofMinutes(15)

    /** Returns the ids of transactions that form a transfer pair. */
    fun findTransferPairs(transactions: List<Transaction>): Set<Long> {
        // UNKNOWN is included deliberately: since IncomeClassifier narrowed CREDIT->INCOME
        // to salary-only, a self-transfer's incoming leg is UNKNOWN (not INCOME) until this
        // detector re-labels it TRANSFER. Excluding UNKNOWN here silently broke matching for
        // every non-salary credit — the exact case this detector exists to catch.
        val candidates = transactions.filter {
            !TransactionMessageStatus.isNonCompleted(it.rawSourceText.orEmpty()) &&
            it.direction != TransactionDirection.UNKNOWN && (
            it.type == TransactionType.EXPENSE ||
                it.type == TransactionType.INCOME ||
                it.type == TransactionType.UNKNOWN || it.type == TransactionType.TRANSFER)
        }
        val matched = mutableSetOf<Long>()
        val references = candidates.associate { it.id to SourceReconciliation.references(it) }
        val possible = mutableMapOf<Long, MutableSet<Long>>()
        for (i in candidates.indices) {
            val a = candidates[i]

            for (j in i + 1 until candidates.size) {
                val b = candidates[j]
                // BigDecimal.equals() treats "5210" and "5210.00" as different (scale
                // differs); compareTo() is the numeric comparison we actually want here.
                if (a.amount.compareTo(b.amount) != 0) continue
                if (a.direction == b.direction) continue
                // Card receipts and unattributed alerts are not proof of a
                // transfer between two identified bank accounts.
                if (a.bank.isNullOrBlank() || b.bank.isNullOrBlank() || a.accountSuffix.isNullOrBlank() || b.accountSuffix.isNullOrBlank()) continue
                if (a.bank.equals(b.bank, true) && a.accountSuffix == b.accountSuffix) continue
                val dateOnly = a.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT ||
                    b.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT
                val window = if (dateOnly) Duration.ofDays(3) else SMS_TIME_WINDOW
                if (Duration.between(a.timestamp, b.timestamp).abs() > window) continue
                if (references.getValue(a.id).intersect(references.getValue(b.id)).isEmpty()) continue
                possible.getOrPut(a.id) { mutableSetOf() } += b.id
                possible.getOrPut(b.id) { mutableSetOf() } += a.id
            }
        }
        for ((id, peers) in possible) {
            val peer = peers.singleOrNull() ?: continue
            if (possible[peer]?.singleOrNull() == id) {
                matched += id
                matched += peer
            }
        }

        return matched
    }
}
