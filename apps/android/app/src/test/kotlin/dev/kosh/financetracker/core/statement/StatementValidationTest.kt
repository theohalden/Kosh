package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class StatementValidationTest {
    private fun row(amount: String, balance: String) = StatementTransaction(
        Instant.parse("2026-01-01T00:00:00Z"), BigDecimal(amount), TransactionDirection.DEBIT,
        "Example", null, BigDecimal(balance), "HDFC", "1234",
    )
    @Test fun `finds missing rows through balance discrepancies`() {
        assertTrue(StatementValidation.check(listOf(row("10", "90"), row("20", "70"))).isEmpty())
        val issues = StatementValidation.check(listOf(row("10", "90"), row("20", "60")))
        assertEquals(2, issues.single().row)
    }
    @Test fun `does not compare balances across accounts`() {
        assertTrue(StatementValidation.check(listOf(row("10", "90"), row("20", "500").copy(bank = "Kotak"))).isEmpty())
    }
}
