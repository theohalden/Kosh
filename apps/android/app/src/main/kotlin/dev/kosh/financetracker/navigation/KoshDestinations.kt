package dev.kosh.financetracker.navigation

sealed class KoshDestination(val route: String) {
    object Overview : KoshDestination("overview")
    object Trail : KoshDestination("trail")
    object Insights : KoshDestination("insights")
    object Review : KoshDestination("review")
    object Detail : KoshDestination("detail/{transactionId}") {
        const val ARG_TRANSACTION_ID = "transactionId"
        fun createRoute(transactionId: Long) = "detail/$transactionId"
    }
}

val BOTTOM_NAV_DESTINATIONS = listOf(
    KoshDestination.Overview,
    KoshDestination.Trail,
    KoshDestination.Insights,
    KoshDestination.Review,
)
