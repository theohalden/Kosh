package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class HdfcParserTest {

    private val parser = HdfcParser()

    private fun event(sender: String, body: String) = RawFinancialEvent(
        id = "test-1",
        source = EventSource.SMS,
        sender = sender,
        rawText = body,
        timestamp = Instant.now(),
        sourceIdentifier = null,
    )

    @Test
    fun `recognizes HDFC sender codes`() {
        assertTrue(parser.canParse(event("CP-HDFCBN-S", "anything")))
        assertTrue(parser.canParse(event("HDFCBK", "anything")))
        assertFalse(parser.canParse(event("KOTAKB", "anything with HDFC nowhere")))
    }

    @Test
    fun `parses Sent Rs From HDFC Bank pattern as DEBIT with merchant`() {
        val body = "Sent Rs.119.18 From HDFC Bank A/C *5590 To swiggy@ybl On 28/09/26 Ref No 234567890123"
        val result = parser.parse(event("CP-HDFCBN-S", body))

        assertNotNull(result)
        assertEquals(BigDecimal("119.18"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals("swiggy@ybl", result.merchantRaw)
        assertEquals("5590", result.accountSuffix)
        assertEquals("234567890123", result.transactionRef)
        assertEquals("HDFC", result.bank)
    }

    @Test
    fun `parses standard debited VPA pattern`() {
        val body = "Rs.450.00 debited from A/c XX1234 on 28-Sep-26 to VPA swiggy@ybl SWIGGY. Ref No 234567890124"
        val result = parser.parse(event("HDFCBK", body))

        assertNotNull(result)
        assertEquals(BigDecimal("450.00"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals("swiggy", result.merchantRaw)
        assertEquals("1234", result.accountSuffix)
    }

    @Test
    fun `parses NEFT credited pattern with Info merchant`() {
        val body = "Rs.49,800.00 credited to A/c XX1234 on 28-Sep-26 by NEFT. Info: SALARY. Avl Bal Rs.55,230.00"
        val result = parser.parse(event("HDFCBK", body))

        assertNotNull(result)
        assertEquals(BigDecimal("49800.00"), result!!.amount)
        assertEquals(TransactionDirection.CREDIT, result.direction)
        assertEquals("SALARY", result.merchantRaw)
        assertEquals(BigDecimal("55230.00"), result.balanceAfter)
    }

    @Test
    fun `returns null when no amount is present`() {
        val result = parser.parse(event("HDFCBK", "Your HDFC Bank statement is ready for download"))
        assertEquals(null, result)
    }
}
