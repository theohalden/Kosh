package dev.kosh.financetracker.core.classifier

/**
 * "Credit does not automatically mean income" (finance integrity rule). A random
 * UPI credit from a person is real money received, but it isn't Income in the
 * headline sense — only recognizable salary/payroll credits are. Anything else
 * stays TransactionType.UNKNOWN until a real classifier (Phase 8) or a user
 * correction resolves it, rather than silently inflating the Income figure.
 */
object IncomeClassifier {

    private val SALARY_KEYWORDS = listOf(
        "salary", "sal credit", "sal.credit", "payroll", "wages", "stipend", "sal cr",
    )

    fun isLikelySalary(merchant: String?, rawSourceText: String?): Boolean {
        val haystack = "${merchant.orEmpty()} ${rawSourceText.orEmpty()}".lowercase()
        return SALARY_KEYWORDS.any { haystack.contains(it) }
    }
}
