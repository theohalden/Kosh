package dev.kosh.financetracker.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ParserFieldsTest {
    @Test fun `reads rupee INR and Rs amounts`() {
        for (prefix in listOf("Rs.", "INR ", "₹")) {
            assertEquals(BigDecimal("1234.50"), ParserFields.amount("${prefix}1,234.50 debited"))
        }
    }
    @Test fun `uses last digits of a full account number rather than first six`() {
        assertEquals("1234", ParserFields.accountSuffix("credited to Account No. 987654321234"))
        assertEquals("1234", ParserFields.accountSuffix("A/c XX1234"))
    }
}
