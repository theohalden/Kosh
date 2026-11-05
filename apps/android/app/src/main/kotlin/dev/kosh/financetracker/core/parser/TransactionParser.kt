package dev.kosh.financetracker.core.parser

import dev.kosh.financetracker.core.model.ParsedTransaction
import dev.kosh.financetracker.core.model.RawFinancialEvent

/**
 * One adapter per bank/app format. Deliberately not one giant regex across every
 * bank — each parser only needs to understand its own sender and message shapes.
 */
interface TransactionParser {
    fun canParse(event: RawFinancialEvent): Boolean
    fun parse(event: RawFinancialEvent): ParsedTransaction?
}
