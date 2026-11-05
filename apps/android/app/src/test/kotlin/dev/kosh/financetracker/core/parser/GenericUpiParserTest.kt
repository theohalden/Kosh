package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class GenericUpiParserTest {

    private val parser = GenericUpiParser()

    private fun event(sender: String, body: String) = RawFinancialEvent(
        id = "test-1",
        source = EventSource.SMS,
        sender = sender,
        rawText = body,
        timestamp = Instant.now(),
        sourceIdentifier = null,
    )

    @Test
    fun `parses an unrecognized bank's UPI debit as a fallback`() {
        val body = "Rs.220.00 debited via UPI on 28-Sep-26 to merchant@upi Ref 234567890127"
        val event = event("AD-SBIUPI-S", body)

        assertTrue(parser.canParse(event))
        val result = parser.parse(event)

        assertNotNull(result)
        assertEquals(BigDecimal("220.00"), result!!.amount)
        assertEquals(TransactionDirection.DEBIT, result.direction)
        assertEquals(null, result.bank)
    }

    @Test
    fun `does not claim non-UPI messages`() {
        val event = event("PROMO", "Flat 50% off on your next order!")
        assertEquals(false, parser.canParse(event))
    }
}
