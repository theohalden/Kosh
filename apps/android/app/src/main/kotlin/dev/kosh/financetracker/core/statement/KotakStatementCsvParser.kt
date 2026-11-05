package dev.kosh.financetracker.core.statement

import dev.kosh.financetracker.core.model.TransactionDirection
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Kotak Bank's CSV statement export. Real shape (metadata header rows, then):
 *   "Sl. No.","Transaction Date","Value Date","Description","Chq /Ref No.","Amount","Dr / Cr","Balance","Dr / Cr"
 *   "1","01-01-2026 10:19:27","01-01-2026","UPI/SAH MANOHAR/636724319358/UPI","UPI-600185993756","31.00","DR","387.67","CR"
 * ...followed by a "Closing Balance" line and disclaimer text, which are skipped.
 */
class KotakStatementCsvParser : StatementParser {

    private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
    private val accountNoRegex = Regex("""(\d{6,})""")

    override fun canParse(fileName: String, peek: String): Boolean =
        peek.contains("Kotak Mahindra", ignoreCase = true) ||
            peek.contains("KKBK", ignoreCase = true) ||
            (peek.contains("Sl. No.") && peek.contains("Dr / Cr"))

    override fun parse(input: InputStream): List<StatementTransaction> {
        val lines = input.bufferedReader(Charsets.UTF_8).readLines()
        val accountSuffix = findAccountSuffix(lines) ?: return emptyList()
        val headerIndex = lines.indexOfFirst { it.startsWith("\"Sl. No.\"") }
        if (headerIndex == -1) return emptyList()

        val transactions = mutableListOf<StatementTransaction>()
        for (line in lines.drop(headerIndex + 1)) {
            val fields = parseCsvLine(line)
            val slNo = fields.firstOrNull()?.trim().orEmpty()
            if (slNo.toIntOrNull() == null) continue // "Closing Balance" / footer rows
            require(fields.size >= 9) { "Incomplete statement transaction $slNo" }

            val timestamp = runCatching {
                LocalDateTime.parse(fields[1].trim(), dateFormat).atZone(ZoneId.systemDefault()).toInstant()
            }.getOrNull() ?: error("Invalid statement date at transaction $slNo")
            val amount = fields[5].trim().replace(",", "").toBigDecimalOrNull()
                ?: error("Invalid statement amount at transaction $slNo")
            val direction = when (fields[6].trim().uppercase()) {
                "DR" -> TransactionDirection.DEBIT
                "CR" -> TransactionDirection.CREDIT
                else -> error("Invalid debit/credit direction at transaction $slNo")
            }
            val balance = fields[7].trim().replace(",", "").toBigDecimalOrNull()
                ?: error("Invalid balance at transaction $slNo")
            val balanceAfter = when (fields[8].trim().uppercase()) {
                "DR" -> balance.negate()
                "CR" -> balance
                else -> error("Invalid balance direction at transaction $slNo")
            }

            transactions += StatementTransaction(
                timestamp = timestamp,
                amount = amount,
                direction = direction,
                description = fields[3].trim(),
                referenceNo = fields[4].trim().ifBlank { null },
                balanceAfter = balanceAfter,
                bank = "Kotak",
                accountSuffix = accountSuffix,
            )
        }
        return transactions
    }

    private fun findAccountSuffix(lines: List<String>): String? {
        val accountLine = lines.firstOrNull { it.contains("\"Account No.\"") } ?: return null
        val fields = parseCsvLine(accountLine)
        val accountNo = fields.lastOrNull { accountNoRegex.matches(it.trim()) } ?: return null
        return accountNo.trim().takeLast(4)
    }
}
