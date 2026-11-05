package dev.kosh.financetracker.ui.format

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Indian digit grouping: last 3 digits together, then groups of 2
 * (12,34,567.89 not 1,234,567.89). `hidden` masks the digits for the
 * privacy/eye toggle while keeping the ₹ sign and layout stable.
 */
fun formatRupees(amount: BigDecimal, hidden: Boolean = false): String {
    if (hidden) return "₹*****"

    val rounded = amount.setScale(2, RoundingMode.HALF_UP)
    val negative = rounded.signum() < 0
    val plain = rounded.abs().toPlainString()
    val (intPart, fracPart) = plain.split(".").let { it[0] to it.getOrElse(1) { "00" } }

    return buildString {
        if (negative) append('-')
        append('₹')
        append(groupIndianDigits(intPart))
        append('.')
        append(fracPart.padEnd(2, '0').take(2))
    }
}

private fun groupIndianDigits(intDigits: String): String {
    if (intDigits.length <= 3) return intDigits

    val lastThree = intDigits.takeLast(3)
    var remaining = intDigits.dropLast(3)
    val groups = ArrayDeque<String>()
    while (remaining.length > 2) {
        groups.addFirst(remaining.takeLast(2))
        remaining = remaining.dropLast(2)
    }
    if (remaining.isNotEmpty()) groups.addFirst(remaining)
    groups.addLast(lastThree)
    return groups.joinToString(",")
}
