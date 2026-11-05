package dev.kosh.financetracker.core.statement

import java.io.ByteArrayInputStream
import javax.inject.Inject

/**
 * Tries each bank's statement parser in turn, picking the one whose [StatementParser.canParse]
 * recognizes the real file content — same pattern as [dev.kosh.financetracker.core.parser.TransactionParserRegistry]
 * for SMS.
 */
class StatementParserRegistry @Inject constructor() {

    private val parsers: List<StatementParser> = listOf(
        KotakStatementCsvParser(),
        HdfcStatementXlsParser(),
        SliceStatementXlsxParser(),
    )

    /** [bytes] is the full file content — statement files are small enough (a few
     * hundred KB at most) to hold in memory, and parsers need to both peek and
     * then fully read, which a one-shot InputStream can't do twice. */
    fun parse(fileName: String, bytes: ByteArray): List<StatementTransaction> {
        val peek = runCatching { bytes.decodeToString(0, minOf(bytes.size, 4000)) }.getOrDefault("")
        val parser = parsers.firstOrNull { it.canParse(fileName, peek) } ?: return emptyList()
        return parser.parse(ByteArrayInputStream(bytes))
    }
}
