package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * HDFC's statement export is legacy binary .xls (no CSV option on net banking) —
 * real shape: metadata rows, an asterisk divider, a header row (Date, Narration,
 * Chq./Ref.No., Value Dt, Withdrawal Amt., Deposit Amt., Closing Balance), another
 * asterisk divider, then one row per transaction, ending with a blank row before
 * a "STATEMENT SUMMARY" footer section.
 */
class HdfcStatementXlsParser : StatementParser {

    // Content sniffing isn't practical here: .xls is a binary OLE2/BIFF8 format
    // storing strings as UTF-16LE, so a naive byte-range decode doesn't reliably
    // surface readable substrings like "HDFC" — they come out interleaved with
    // null bytes. Extension is the honest signal available cheaply; parse() itself
    // still bails safely (returns no rows) if the header row it expects isn't found.
    override fun canParse(fileName: String, peek: String): Boolean =
        fileName.endsWith(".xls", ignoreCase = true)

    override fun parse(input: InputStream): List<StatementTransaction> {
        val workbook = HSSFWorkbook(input)
        val sheet = workbook.getSheetAt(0)
        val accountSuffix = findAccountSuffix(sheet) ?: return emptyList()

        val headerRowIndex = (0..sheet.lastRowNum).firstOrNull { idx ->
            val row = sheet.getRow(idx) ?: return@firstOrNull false
            cellText(row.getCell(0)).equals("Date", ignoreCase = true) &&
                cellText(row.getCell(1)).equals("Narration", ignoreCase = true)
        } ?: return emptyList()

        val transactions = mutableListOf<StatementTransaction>()
        for (idx in (headerRowIndex + 2)..sheet.lastRowNum) {
            val row = sheet.getRow(idx) ?: continue
            val dateText = cellText(row.getCell(0))
            if (dateText.startsWith("STATEMENT SUMMARY", ignoreCase = true)) break
            if (dateText.isBlank() || dateText.all { it == '*' }) continue
            val date = parseDdMmYy(dateText) ?: error("Invalid statement date at row ${idx + 1}")

            val withdrawal = cellNumber(row.getCell(4))
            val deposit = cellNumber(row.getCell(5))
            val (amount, direction) = when {
                withdrawal != null && withdrawal > BigDecimal.ZERO -> withdrawal to TransactionDirection.DEBIT
                deposit != null && deposit > BigDecimal.ZERO -> deposit to TransactionDirection.CREDIT
                else -> error("Invalid statement amount at row ${idx + 1}")
            }
            require(!(withdrawal != null && withdrawal > BigDecimal.ZERO && deposit != null && deposit > BigDecimal.ZERO)) {
                "Both debit and credit are populated at row ${idx + 1}"
            }

            transactions += StatementTransaction(
                timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                amount = amount,
                direction = direction,
                description = cellText(row.getCell(1)),
                referenceNo = cellText(row.getCell(2)).ifBlank { null },
                balanceAfter = cellNumber(row.getCell(6)),
                bank = "HDFC",
                accountSuffix = accountSuffix,
            )
        }
        workbook.close()
        return transactions
    }

    private fun findAccountSuffix(sheet: org.apache.poi.ss.usermodel.Sheet): String? {
        for (idx in 0..minOf(30, sheet.lastRowNum)) {
            val row = sheet.getRow(idx) ?: continue
            for (c in 0 until row.lastCellNum) {
                val text = cellText(row.getCell(c))
                val digits = Regex("""Account No\s*:\s*(\d+)""").find(text)?.groupValues?.get(1)
                if (digits != null && digits.length >= 4) return digits.takeLast(4)
            }
        }
        return null
    }

    private fun parseDdMmYy(text: String): LocalDate? {
        val parts = text.trim().split("/")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull()?.let { 2000 + it } ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }

    private fun cellText(cell: Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> cell.numericCellValue.toString()
            else -> ""
        }
    }

    private fun cellNumber(cell: Cell?): BigDecimal? {
        if (cell == null) return null
        return when (cell.cellType) {
            CellType.NUMERIC -> BigDecimal.valueOf(cell.numericCellValue)
            CellType.STRING -> cell.stringCellValue.trim().replace(",", "").toBigDecimalOrNull()
            else -> null
        }
    }
}
