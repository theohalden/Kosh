package dev.kosh.financetracker.core.finance

import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class SourceReconciliationTest {
    private val statement = StatementTransaction(
        Instant.parse("2026-10-02T00:00:00Z"), BigDecimal("100.00"), TransactionDirection.DEBIT,
        "UPI-EXAMPLE-123456789012", null, BigDecimal("900.00"), "HDFC", "1234",
    ).toTransaction()
    private val sms = statement.copy(source = TransactionSource.SMS, timestamp = statement.timestamp.plusSeconds(3600))

    @Test fun `links only reference backed compatible observations`() {
        assertTrue(SourceReconciliation.matches(sms, statement))
        assertTrue(SourceReconciliation.matches(sms.copy(accountSuffix = null), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(bank = null), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(accountSuffix = "5678"), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(bank = "Kotak"), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(direction = TransactionDirection.CREDIT), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(rawSourceText = "UPI payment without reference"), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(amount = BigDecimal("101")), statement))
        assertFalse(SourceReconciliation.matches(sms.copy(timestamp = statement.timestamp.plusSeconds(4 * 86400)), statement))
    }

    @Test fun `account number alone is not a payment reference`() {
        val body = "Rs.100 debited from account 123456789012"
        assertFalse(SourceReconciliation.matches(sms.copy(rawSourceText = body), statement.copy(rawSourceText = body)))
    }

    @Test fun `retains an explicit reference independently of narration`() {
        assertTrue(SourceReconciliation.matches(
            sms.copy(rawSourceText = "payment", transactionRef = "123456789012"), statement,
        ))
    }
}
