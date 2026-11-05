package dev.kosh.financetracker.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class RupeeFormatTest {

    @Test
    fun `groups a lakh with Indian digit pattern`() {
        assertEquals("₹1,23,456.78", formatRupees(BigDecimal("123456.78")))
    }

    @Test
    fun `groups a crore correctly`() {
        assertEquals("₹1,23,45,678.90", formatRupees(BigDecimal("12345678.90")))
    }

    @Test
    fun `does not group amounts under 1000`() {
        assertEquals("₹450.00", formatRupees(BigDecimal("450")))
        assertEquals("₹38.00", formatRupees(BigDecimal("38")))
    }

    @Test
    fun `groups exactly four digits with one comma`() {
        assertEquals("₹44,635.21", formatRupees(BigDecimal("44635.21")))
    }

    @Test
    fun `handles negative amounts`() {
        assertEquals("-₹1,000.00", formatRupees(BigDecimal("-1000")))
    }

    @Test
    fun `masks the amount when hidden`() {
        assertEquals("₹*****", formatRupees(BigDecimal("44635.21"), hidden = true))
    }
}
