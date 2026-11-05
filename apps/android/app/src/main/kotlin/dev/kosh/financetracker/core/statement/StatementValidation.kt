package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import java.math.BigDecimal
import java.util.Locale

/** Checks bank ledger arithmetic, never spending classifications. Row numbers are 1-based parsed positions. */
object StatementValidation {
    data class Issue(val row: Int, val reason: String)

    fun check(rows: List<StatementTransaction>): List<Issue> {
        val issues = mutableListOf<Issue>()
        val previous = mutableMapOf<Pair<String, String>, StatementTransaction>()
        rows.forEachIndexed { index, row ->
            val key = row.bank.trim().lowercase(Locale.ROOT) to row.accountSuffix.trim()
            if (row.amount <= BigDecimal.ZERO || row.direction == TransactionDirection.UNKNOWN) {
                issues += Issue(index + 1, "Invalid amount or direction")
            }
            val last = previous[key]
            if (last != null && row.timestamp < last.timestamp) {
                issues += Issue(index + 1, "Transactions are not in chronological order")
            }
            if (last?.balanceAfter != null && row.balanceAfter != null) {
                val change = if (row.direction == TransactionDirection.CREDIT) row.amount else row.amount.negate()
                if ((last.balanceAfter + change).compareTo(row.balanceAfter) != 0) {
                    issues += Issue(index + 1, "Running balance does not reconcile")
                }
            }
            previous[key] = row
        }
        return issues
    }
}
