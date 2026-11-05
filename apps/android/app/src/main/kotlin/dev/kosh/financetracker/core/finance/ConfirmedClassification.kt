package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionType

/** Explicit user decisions take precedence over automatic classification. */
object ConfirmedClassification {
    private const val PREFIX = "Kosh user-confirmed type: "

    fun type(row: Transaction): TransactionType? {
        val firstLine = row.notes?.lineSequence()?.firstOrNull() ?: return null
        if (!firstLine.startsWith(PREFIX)) return null
        return TransactionType.entries.firstOrNull { it.name == firstLine.removePrefix(PREFIX) }
    }
}
