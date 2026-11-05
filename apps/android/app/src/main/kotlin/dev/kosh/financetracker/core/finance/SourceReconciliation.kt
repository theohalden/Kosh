package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import java.time.Duration
import java.time.ZoneId

/** Conservative source matching. A similar amount or merchant is never proof. */
object SourceReconciliation {
    private val reference = Regex("""(?<!\d)\d{12}(?!\d)""")
    private val accountNumber = Regex("""(?i)\b(?:a/c|account|ac)\.?\s*(?:no\.?)?\s*[x*]*\d+""")
    private val neftReference = Regex("""\b[A-Z]{4}\d{4}[A-Z]\d{7}\b""", RegexOption.IGNORE_CASE)

    fun references(transaction: Transaction): Set<String> {
        val explicit = transaction.transactionRef?.trim()?.takeIf { it.isNotEmpty() }
        val fromText = reference.findAll(accountNumber.replace(transaction.rawSourceText.orEmpty(), "")).map { it.value }.toSet()
        return (fromText + neftReference.findAll(transaction.rawSourceText.orEmpty()).map { it.value }.toSet() + listOfNotNull(explicit))
            .map { it.uppercase(java.util.Locale.ROOT) }.toSet()
    }

    fun sameStatement(a: Transaction, b: Transaction, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        return statementMatchRank(a, b, zone) != null
    }

    /** Lower is stronger. A balance coincidence must not obscure a unique reference. */
    fun statementMatchRank(a: Transaction, b: Transaction, zone: ZoneId = ZoneId.systemDefault()): Int? {
        if (a.source != TransactionSource.STATEMENT || b.source != TransactionSource.STATEMENT) return null
        if (!a.bank?.trim().equals(b.bank?.trim(), true) || a.accountSuffix != b.accountSuffix) return null
        if (a.direction != b.direction || a.amount.compareTo(b.amount) != 0) return null
        if (a.timestamp.atZone(zone).toLocalDate() != b.timestamp.atZone(zone).toLocalDate()) return null
        if (a.balanceAfter != null && b.balanceAfter != null && a.balanceAfter.compareTo(b.balanceAfter) != 0) return null
        if (references(a).intersect(references(b)).isNotEmpty()) return 0
        if (a.balanceAfter != null && b.balanceAfter != null && a.balanceAfter.compareTo(b.balanceAfter) == 0) {
            return if (a.rawSourceText == b.rawSourceText) 1 else 2
        }
        return null
    }

    fun matches(observation: Transaction, statement: Transaction): Boolean {
        if (observation.source != TransactionSource.SMS || statement.source != TransactionSource.STATEMENT) return false
        if (observation.direction == TransactionDirection.UNKNOWN || observation.direction != statement.direction) return false
        if (observation.amount.compareTo(statement.amount) != 0) return false
        // Never infer a bank from a payment-provider alert. An absent account can
        // only be resolved later if there is exactly one reference-backed candidate.
        if (observation.bank.isNullOrBlank() || !observation.bank.trim().equals(statement.bank?.trim(), true)) return false
        if (observation.accountSuffix != null && observation.accountSuffix != statement.accountSuffix) return false
        if (Duration.between(observation.timestamp, statement.timestamp).abs() > Duration.ofDays(3)) return false
        return references(observation).intersect(references(statement)).isNotEmpty()
    }
}
