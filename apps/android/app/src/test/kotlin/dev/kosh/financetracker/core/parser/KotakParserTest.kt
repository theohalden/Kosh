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

class KotakParserTest {

    private val parser = KotakParser()

    private fun event(sender: String, body: String) = RawFinancialEvent(
        id = "test-1",
        source = EventSource.SMS,
        sender = sender,
        rawText = body,
        timestamp = Instant.now(),
        sourceIdentifier = null,
    )

    @Test
    fun `recognizes Kotak sender and signature`() {
        assertTrue(parser.canParse(event("KOTAKB", "anything")))
        assertTrue(parser.canParse(event("VM-KOTAKB", "signed off -Kotak Bank")))
        assertFalse(parser.canParse(event("HDFCBK", "no kotak mention here")))
    }

    @Test
    fun `parses debited UPI pattern`() {
        val body = "Rs.500.00 debited from your A/c XX8892 on 28-Sep-26 to VPA merchant@okhdfcbank via UPI Ref 234567890125. Avl Bal Rs 5000.00 -Kotak Bank"
        val result = parser.parse(event("KOTAKB", body))

        assertNotNull(result)
        assertEquals(BigDecimal("500.00"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals("merchant", result.merchantRaw)
        assertEquals("8892", result.accountSuffix)
        assertEquals("Kotak", result.bank)
    }

    @Test
    fun `parses received UPI pattern as CREDIT`() {
        val body = "You have received Rs.2000.00 in your A/c XX8892 on 28-Sep-26 from VPA sender@okaxis via UPI Ref 234567890126.-Kotak Bank"
        val result = parser.parse(event("KOTAKB", body))

        assertNotNull(result)
        assertEquals(BigDecimal("2000.00"), result!!.amount)
        assertEquals(TransactionDirection.CREDIT, result.direction)
        assertEquals("sender", result.merchantRaw)
    }

    @Test
    fun `parses received pattern with a plain name instead of a VPA handle`() {
        val body = "Received Rs.100.00 in your Kotak Bank AC 9996 from AMOL KALEL on 28-09-26.UPI"
        val result = parser.parse(event("JX-KOTAKB-S", body))

        assertNotNull(result)
        assertEquals(BigDecimal("100.00"), result!!.amount)
        assertEquals(TransactionDirection.CREDIT, result.direction)
        assertEquals("AMOL KALEL", result.merchantRaw)
        assertEquals("9996", result.accountSuffix)
    }
}
