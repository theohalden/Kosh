package dev.kosh.financetracker.core.statement

/**
 * Minimal quoted-CSV line splitter — handles the `"field","field"` shape bank
 * statement exports use, including commas that appear inside quoted amounts
 * (e.g. `"5,000.00"`), which a naive `split(",")` would wrongly break apart.
 */
internal fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields += current.toString()
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields += current.toString()
    return fields
}
