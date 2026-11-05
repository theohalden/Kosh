package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent
import javax.inject.Inject

/**
 * Tries bank-specific parsers first (in order), falls back to the generic UPI
 * parser last. First parser that both claims and successfully parses wins.
 */
class TransactionParserRegistry @Inject constructor() {

    private val parsers: List<TransactionParser> = listOf(
        HdfcParser(),
        KotakParser(),
        SliceParser(),
        GenericUpiParser(),
    )

    fun parse(event: RawFinancialEvent): ParsedTransaction? {
        if (TransactionMessageStatus.isNonCompleted(event.rawText)) return null
        var incomplete: ParsedTransaction? = null
        for (parser in parsers.filter { it.canParse(event) }) {
            val parsed = parser.parse(event) ?: continue
            if (parsed.direction != dev.kosh.financetracker.core.model.TransactionDirection.UNKNOWN) {
                return parsed.copy(bank = parsed.bank ?: incomplete?.bank)
            }
            if (incomplete == null) incomplete = parsed
        }
        return incomplete
    }
}
