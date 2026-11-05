package dev.kosh.financetracker.core.statement

import java.time.LocalDate

sealed interface StatementImportResult {
    /** The file didn't match any known bank statement format. */
    data object Unrecognized : StatementImportResult

    data class Imported(
        val bank: String,
        val accountSuffix: String,
        val importedCount: Int,
        val replacedCount: Int,
        val periodStart: LocalDate,
        val periodEnd: LocalDate,
        val addedCount: Int = importedCount,
        val existingCount: Int = 0,
        val linkedCount: Int = 0,
        val unresolvedSmsCount: Int = 0,
        val unresolvedStatementCount: Int = 0,
    ) : StatementImportResult
}
