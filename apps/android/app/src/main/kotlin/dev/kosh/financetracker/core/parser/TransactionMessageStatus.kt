package dev.kosh.financetracker.core.parser

/** A debit keyword in a reminder is not evidence of a completed movement. */
object TransactionMessageStatus {
    private val scheduled = Regex(
        """\b(?:will|would|shall|may)\s+(?:be\s+)?(?:debited|deducted|charged|credited|paid|collected|received)\b|\b(?:pre[- ]?debit|upcoming\s+(?:payment|debit)|payment\s+(?:reminder|due)|scheduled\s+(?:payment|debit|deduction))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val incomplete = Regex(
        """\b(?:transaction|payment|transfer|upi(?:\s+transaction)?)\s+(?:(?:has|was|is)\s+)?(?:failed|declined|unsuccessful|pending|processing)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val requestOnly = Regex(
        """\bauto\s+debit\b[\s\S]*\bis\s+set\b|\bdebit\s+request\b[\s\S]*\bin\s+process\b|\breceived\s+your\s+request\b[\s\S]*\b(?:make\s+a\s+payment|please\s+pay)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val confirmed = Regex(
        """\b(?:spent|sent|paid|received)\s+(?:Rs\.?|INR\.?|₹)\s*[\d,]+|(?:Rs\.?|INR\.?|₹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:(?:has\s+been|was)\s+)?(?:debited|credited)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun isNonCompleted(body: String): Boolean =
        (scheduled.containsMatchIn(body) || incomplete.containsMatchIn(body) || requestOnly.containsMatchIn(body)) && !confirmed.containsMatchIn(body)
}
