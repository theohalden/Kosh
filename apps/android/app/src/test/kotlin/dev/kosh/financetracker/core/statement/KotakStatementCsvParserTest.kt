package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal
import java.io.ByteArrayInputStream

class KotakStatementCsvParserTest {

    private val parser = KotakStatementCsvParser()

    // Mirrors the real Kotak CSV export shape (anonymized values).
    private val sampleCsv = """
        "","","Account Statement"
        "Example Name"
        "S-O: EXAMPLE","","","","Cust. Reln. No.","920000000"
        "EXAMPLE ADDRESS","","","","Account No.","2049590001"
        "EXAMPLE CITY","","","","Period","From 01/01/2026 To 04/10/2026"
        "EXAMPLE","","","","Currency","INR"
        "EXAMPLE","","","","Branch","EXAMPLE BRANCH"
        "EXAMPLE","","","","Nomination Regd","Y"
        "000000","","","","Nominee Name",""
        "","","","","Joint Holder(s)",""
        "","","","","IFSC","KKBK0000681"
        "","","","","MICR","400000000"

        "Sl. No.","Transaction Date","Value Date","Description","Chq /Ref No.","Amount","Dr / Cr","Balance","Dr / Cr"
        "1","01-01-2026 10:19:27","01-01-2026","UPI/EXAMPLE MERCHANT/636724319358/UPI","UPI-600185993756","31.00","DR","387.67","CR"
        "2","12-01-2026 10:26:22","12-01-2026","UPI/EXAMPLE PAYER/117059786329/UPI","UPI-601217265584","5,000.00","CR","5,001.17","CR"
        "Closing Balance","as on 04/10/2026 INR 102.79"

        "Important Note:"
        "Disclaimer text follows."
    """.trimIndent()

    @Test
    fun `claims a real-shaped Kotak CSV`() {
        assertTrue(parser.canParse("statement.csv", sampleCsv.take(500)))
    }

    @Test
    fun `parses transactions with correct direction, amount, balance and account suffix`() {
        val result = parser.parse(ByteArrayInputStream(sampleCsv.toByteArray()))

        assertEquals(2, result.size)

        val first = result[0]
        assertEquals(BigDecimal("31.00"), first.amount)
        assertEquals(TransactionDirection.DEBIT, first.direction)
        assertEquals(BigDecimal("387.67"), first.balanceAfter)
        assertEquals("0001", first.accountSuffix)
        assertEquals("Kotak", first.bank)

        val second = result[1]
        // Regression: amount has an embedded comma ("5,000.00") inside quotes —
        // a naive comma-split would have broken this into the wrong fields.
        assertEquals(BigDecimal("5000.00"), second.amount)
        assertEquals(TransactionDirection.CREDIT, second.direction)
        assertEquals(BigDecimal("5001.17"), second.balanceAfter)
    }

    @Test
    fun `does not claim an unrelated file`() {
        assertTrue(!parser.canParse("other.csv", "Date,Narration,Amount\n01/01/26,Example,100"))
    }

    @Test fun `a malformed transaction is reported rather than silently skipped`() {
        assertThrows(IllegalStateException::class.java) {
            parser.parse(ByteArrayInputStream(sampleCsv.replace("31.00", "invalid").toByteArray()))
        }
    }

    @Test fun `debit balance is represented as negative`() {
        val input = sampleCsv.replace("\"387.67\",\"CR\"", "\"387.67\",\"DR\"")
        assertEquals(BigDecimal("-387.67"), parser.parse(ByteArrayInputStream(input.toByteArray())).first().balanceAfter)
    }
}
