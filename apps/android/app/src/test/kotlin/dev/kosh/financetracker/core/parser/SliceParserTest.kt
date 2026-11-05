package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.EventSource
import dev.kosh.financetracker.core.model.RawFinancialEvent
import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class SliceParserTest {

    private val parser = SliceParser()

    private fun event(body: String, sender: String? = "AX-SLICEB-S") = RawFinancialEvent(
        id = "1",
        source = EventSource.SMS,
        sender = sender,
        rawText = body,
        timestamp = Instant.now(),
        sourceIdentifier = null,
    )

    @Test
    fun `claims and parses the AutoPay template`() {
        val body = "Successfully paid Rs.59 from slice a/c XX1234 to Example Merchant " +
            "on 22-Sep-26 via UPI AutoPay. UMN - abc123@slc - slice"
        val e = event(body)

        assertTrue(parser.canParse(e))
        val parsed = parser.parse(e)!!
        assertEquals(BigDecimal("59"), parsed.amount)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("1234", parsed.accountSuffix)
        assertEquals("Example Merchant", parsed.merchantRaw)
        assertEquals("Slice", parsed.bank)
    }

    @Test
    fun `claims and parses the sent-to template`() {
        val body = "Rs. 1,800 sent from a/c XX1234 on 20-Sep-26 to Example Payee " +
            "(UPI Ref: 626309763233). Not you? Call 08048329999 - slice"
        val e = event(body)

        assertTrue(parser.canParse(e))
        val parsed = parser.parse(e)!!
        assertEquals(BigDecimal("1800"), parsed.amount)
        assertEquals(TransactionDirection.DEBIT, parsed.direction)
        assertEquals("Example Payee", parsed.merchantRaw)
        assertEquals("Slice", parsed.bank)
    }

    @Test
    fun `does not claim an unrelated HDFC message`() {
        val body = "Sent Rs.119.18 From HDFC Bank A/C *5590 To Example On 28/09/26 Ref No 123"
        assertTrue(!parser.canParse(event(body, sender = "AD-HDFCBK-T")))
    }

    @Test
    fun `returns null without a parseable amount`() {
        val body = "Your slice account statement is ready - slice"
        val parsed = parser.parse(event(body))
        assertNull(parsed)
    }
}
