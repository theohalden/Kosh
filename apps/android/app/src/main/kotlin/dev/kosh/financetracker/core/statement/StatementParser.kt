package dev.kosh.financetracker.core.statement

import java.io.InputStream

/**
 * One parser per bank's statement export format. Like [dev.kosh.financetracker.core.parser.TransactionParser]
 * for SMS, [canParse] sniffs real file content (header rows, bank-identifying
 * text) rather than trusting the filename — a user can save the download as
 * anything.
 */
interface StatementParser {
    /** [peek] is the first chunk of the file, decoded as text where possible, used
     * only to decide ownership — cheap, doesn't need to fully parse. */
    fun canParse(fileName: String, peek: String): Boolean

    /** Parses the full file. [input] must still be at the start of the stream. */
    fun parse(input: InputStream): List<StatementTransaction>
}
