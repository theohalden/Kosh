package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SliceStatementXlsxParserTest {

    private val parser = SliceStatementXlsxParser()

    private val sharedStrings = listOf(
        "A/C Number", "033325220001234", "DATE", "DETAILS", "REF NO.", "TRANSACTION TYPE",
        "DEBIT", "CREDIT", "BALANCE", "11 Jan '26", "Example credit details", "ref1",
        "Example debit details", "ref2",
    )

    /** Hand-builds a minimal real-shaped .xlsx (zip of sharedStrings.xml + sheet1.xml)
     * mirroring the real Slice export: an A/C Number row, a header row, then data rows. */
    private fun buildXlsx(sheetName: String = "sheet1.xml"): ByteArray {
        val sharedStringsXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sharedStrings.forEach { append("<si><t>$it</t></si>") }
            append("</sst>")
        }

        val sheetXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            append("""<row r="1"><c r="D1" t="s"><v>0</v></c><c r="E1" t="s"><v>1</v></c></row>""")
            append(
                """<row r="8"><c r="A8" t="s"><v>2</v></c><c r="B8" t="s"><v>3</v></c>""" +
                    """<c r="C8" t="s"><v>4</v></c><c r="D8" t="s"><v>5</v></c>""" +
                    """<c r="E8" t="s"><v>6</v></c><c r="F8" t="s"><v>7</v></c>""" +
                    """<c r="G8" t="s"><v>8</v></c></row>""",
            )
            append(
                """<row r="9"><c r="A9" t="s"><v>9</v></c><c r="B9" t="s"><v>10</v></c>""" +
                    """<c r="C9" t="s"><v>11</v></c><c r="D9" t="s"><v>7</v></c>""" +
                    """<c r="F9"><v>200</v></c><c r="G9"><v>212.51</v></c></row>""",
            )
            append(
                """<row r="10"><c r="A10" t="s"><v>9</v></c><c r="B10" t="s"><v>12</v></c>""" +
                    """<c r="C10" t="s"><v>13</v></c><c r="D10" t="s"><v>6</v></c>""" +
                    """<c r="E10"><v>172</v></c><c r="G10"><v>40.51</v></c></row>""",
            )
            append("</sheetData></worksheet>")
        }

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(("<Relationships><Relationship Id=\"rId4\" " +
                "Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" " +
                "Target=\"worksheets/$sheetName\"/></Relationships>").toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStringsXml.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("xl/worksheets/$sheetName"))
            zip.write(sheetXml.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    @Test
    fun `parses debit and credit rows with correct amount, balance and account suffix`() {
        val result = parser.parse(ByteArrayInputStream(buildXlsx()))

        assertEquals(2, result.size)

        val credit = result[0]
        assertEquals(BigDecimal("200"), credit.amount)
        assertEquals(TransactionDirection.CREDIT, credit.direction)
        assertEquals(BigDecimal("212.51"), credit.balanceAfter)
        assertEquals("1234", credit.accountSuffix)
        assertEquals("Slice", credit.bank)

        val debit = result[1]
        assertEquals(BigDecimal("172"), debit.amount)
        assertEquals(TransactionDirection.DEBIT, debit.direction)
    }

    @Test
    fun `reads the worksheet relationship when sheet1 does not exist`() {
        assertEquals(2, parser.parse(ByteArrayInputStream(buildXlsx("sheet2.xml"))).size)
    }

    @Test
    fun `claims any xlsx file by extension since zip content can't be cheaply sniffed`() {
        assertTrue(parser.canParse("statement.xlsx", ""))
    }
}
