package dev.kosh.financetracker.core.statement

import org.junit.Assert.assertEquals
import org.junit.Test

class StatementTransactionMapperTest {

    @Test
    fun `extracts merchant from Kotak's slash-delimited narration`() {
        assertEquals("EXAMPLE MERCHANT", guessMerchant("UPI/EXAMPLE MERCHANT/636724319358/UPI"))
    }

    @Test
    fun `extracts merchant from HDFC's dash-delimited narration`() {
        assertEquals(
            "EXAMPLE PAYEE",
            guessMerchant("UPI-EXAMPLE PAYEE-example@okicici-HDFC0000814-636726925223-UPI"),
        )
    }

    @Test
    fun `extracts merchant from Slice's dash-delimited narration with a type prefix`() {
        assertEquals(
            "EXAMPLE STORE",
            guessMerchant("UPI Debit-EXAMPLE STORE-example@hdfcbank-102444034574-PAYMENT"),
        )
    }

    @Test
    fun `extracts merchant from Slice's other shape where Debit and a ref number precede the name`() {
        // Regression: caught on a real device test — "UPI-Debit-627889421762-Rapido
        // QR Pay-..." was returning the literal word "Debit" as the merchant,
        // because this shape puts the direction marker and reference number
        // *before* the name, unlike the other Slice narration shape.
        assertEquals(
            "Rapido QR Pay",
            guessMerchant("UPI-Debit-627889421762-Rapido QR Pay-AIRP0000011-Payment To Rapido QR"),
        )
        assertEquals(
            "Example Payer",
            guessMerchant("UPI-Credit-601408913032-Example Payer-self transfer"),
        )
    }

    @Test
    fun `falls back to the full description when no known pattern matches`() {
        assertEquals("Interest Cr. for 11-Jan-2026", guessMerchant("Interest Cr. for 11-Jan-2026"))
        assertEquals("CASHBACK EARNED", guessMerchant("CASHBACK EARNED"))
    }
}
