package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

class HdfcStatementXlsParserTest {

    private val parser = HdfcStatementXlsParser()

    /** Builds a workbook mirroring the real HDFC .xls shape (anonymized values):
     * metadata rows, an asterisk divider, the header row, another divider, then
     * data rows, ending with a blank row before a footer section. */
    private fun buildWorkbook(): ByteArray {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Statement")

        sheet.createRow(0).createCell(0).setCellValue("HDFC BANK Ltd. Statement of accounts")
        repeat(13) { sheet.createRow(it + 1) }
        sheet.createRow(14).createCell(4).setCellValue("Account No :50100390001235590   EXAMPLE")
        sheet.createRow(19).also { row ->
            row.createCell(0).setCellValue("Date")
            row.createCell(1).setCellValue("Narration")
            row.createCell(2).setCellValue("Chq./Ref.No.")
            row.createCell(3).setCellValue("Value Dt")
            row.createCell(4).setCellValue("Withdrawal Amt.")
            row.createCell(5).setCellValue("Deposit Amt.")
            row.createCell(6).setCellValue("Closing Balance")
        }
        sheet.createRow(20).createCell(0).setCellValue("********")

        sheet.createRow(21).also { row ->
            row.createCell(0).setCellValue("01/01/26")
            row.createCell(1).setCellValue("UPI-EXAMPLE MERCHANT-example@okaxis-636726925223-UPI")
            row.createCell(2).setCellValue("0000636726925223")
            row.createCell(3).setCellValue("01/01/26")
            row.createCell(4).setCellValue(40.0)
            row.createCell(6).setCellValue(70.7)
        }
        sheet.createRow(22).also { row ->
            row.createCell(0).setCellValue("04/01/26")
            row.createCell(1).setCellValue("UPI-EXAMPLE PAYER-payer@okicici-440951037588-UPI")
            row.createCell(2).setCellValue("0000440951037588")
            row.createCell(3).setCellValue("04/01/26")
            row.createCell(5).setCellValue(750.0)
            row.createCell(6).setCellValue(820.7)
        }

        sheet.createRow(23)
        sheet.createRow(24).createCell(0).setCellValue("********")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }

    @Test
    fun `parses debit and credit rows with correct amount, balance and account suffix`() {
        val bytes = buildWorkbook()
        val result = parser.parse(ByteArrayInputStream(bytes))

        assertEquals(2, result.size)

        val first = result[0]
        // BigDecimal.valueOf(double) is scale-sensitive like equals() — compareTo
        // is the correct numeric comparison here (40.0 == 40, 70.7 == 70.7).
        assertEquals(0, BigDecimal("40").compareTo(first.amount))
        assertEquals(TransactionDirection.DEBIT, first.direction)
        assertEquals(0, BigDecimal("70.7").compareTo(first.balanceAfter))
        assertEquals("5590", first.accountSuffix)
        assertEquals("HDFC", first.bank)

        val second = result[1]
        assertEquals(0, BigDecimal("750").compareTo(second.amount))
        assertEquals(TransactionDirection.CREDIT, second.direction)
    }

    @Test
    fun `claims any xls file by extension since content can't be cheaply sniffed`() {
        assertTrue(parser.canParse("statement.xls", ""))
    }
}
