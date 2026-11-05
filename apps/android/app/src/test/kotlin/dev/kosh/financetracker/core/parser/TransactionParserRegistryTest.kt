package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TransactionParserRegistryTest {
    @Test fun `falls back for a claimed bank message without losing bank attribution`() {
        val result = TransactionParserRegistry().parse(RawFinancialEvent(
            id = "example", source = EventSource.SMS, sender = "HDFCBK",
            rawText = "INR 75 spent from A/c XX1234 via UPI Ref No 123456789012",
            timestamp = Instant.parse("2026-01-01T00:00:00Z"), sourceIdentifier = null,
        ))
        assertEquals(TransactionDirection.DEBIT, result?.direction)
        assertEquals("HDFC", result?.bank)
    }
}
