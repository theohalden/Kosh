package dev.kosh.financetracker.core.categorization

import dev.kosh.financetracker.core.model.TransactionCategory

/**
 * Keyword-rule tier only, per the plan's decision order (user rule > merchant rule >
 * historical mapping > keyword rule > generic rule > UNKNOWN). The earlier tiers —
 * per-user overrides and learned merchant mappings — need a corrections UI and a
 * merchant-mapping store, neither of which exist yet; this is the fallback until then.
 */
object CategoryEngine {

    /** Short brand names (2-3 letters) that would false-positive-match as a substring
     * of unrelated words (e.g. "vi" inside "Vijay", "Service", "Investment") — matched
     * by exact merchant string instead of `contains`. */
    private val EXACT_MATCH_RULES: Map<String, TransactionCategory> = mapOf(
        "vi" to TransactionCategory.BILLS,
    )

    private val RULES: List<Pair<TransactionCategory, List<String>>> = listOf(
        TransactionCategory.GROCERIES to listOf(
            "blinkit", "zepto", "bigbasket", "grofers", "dmart", "grocery", "instamart", "supermarket", "dairy",
            "reliance fresh", "more supermarket", "more megastore", "spencer", "nature's basket", "star bazaar",
            "jiomart", "milk", "kirana",
        ),
        TransactionCategory.FOOD to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "kitchen", "biryani", "pizza", "domino", "mcdonald",
            "kfc", "burger", "dhaba", "idli", "dosa", "food", "eatery", "bakery", "chicken", "misal",
            "sweets", "mithai", "juice", "hotel", "tiffin", "meals", "aaswad",
        ),
        TransactionCategory.TRAVEL to listOf(
            "uber", "ola", "rapido", "irctc", "redbus", "indigo", "spicejet", "railway", "metro", "cab",
            "airport", "toll", "fastag", "parking", "auto",
        ),
        TransactionCategory.FUEL to listOf(
            "petrol", "diesel", "fuel", "indian oil", "bharat petroleum", "hp petrol", "hpcl", "iocl", "petroleum",
        ),
        TransactionCategory.SUBSCRIPTIONS to listOf(
            "google workspace", "googleworksp",
            "netflix", "spotify", "prime video", "hotstar", "subscription", "youtube premium", "apple music",
            "gym membership", "sonyliv", "zee5",
        ),
        TransactionCategory.BILLS to listOf(
            "electricity", "water bill", "recharge", "postpaid", "prepaid", "airtel", "jio", "vodafone",
            "vi recharge", "vi payment", "bsnl", "broadband", "gas bill", "dth", "wifi", "internet bill",
        ),
        TransactionCategory.RENT to listOf("rent"),
        TransactionCategory.HEALTH to listOf(
            "pharmacy", "hospital", "clinic", "medical", "apollo", "medplus", "diagnostic", "netmeds",
            "1mg", "pharmeasy", "doctor", "dental", "lab test", "chemist",
        ),
        TransactionCategory.ENTERTAINMENT to listOf(
            "bookmyshow", "pvr", "inox", "cinema", "movie", "gaming", "playstation", "steam",
        ),
        TransactionCategory.EDUCATION to listOf(
            "school", "college", "university", "udemy", "byju", "coaching", "tuition", "coursera", "unacademy",
        ),
        TransactionCategory.PERSONAL_CARE to listOf(
            "salon", "spa", "hair dresser", "beauty", "barber", "nykaa",
        ),
        TransactionCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "retail", "mall", "store", "meesho", "snapdeal",
            "reliance digital", "reliance trends", "lifestyle", "shoppers stop", "pantaloons", "westside",
        ),
        TransactionCategory.TECHNOLOGY to listOf(
            "apple", "samsung", "croma", "electronics", "mobile store", "laptop", "vijay sales", "oneplus",
            "xiaomi", "mi store",
        ),
        TransactionCategory.HOME to listOf(
            "furniture", "ikea", "home center", "home centre", "decor", "urban ladder", "pepperfry", "hardware",
        ),
        TransactionCategory.BUSINESS to listOf(
            "invoice", "gst payment", "vendor payment", "office supplies",
        ),
        TransactionCategory.FEES to listOf(
            "late fee", "penalty", "processing fee", "service charge", "annual fee", "convenience fee",
        ),
        TransactionCategory.OTHER to listOf(
            "groww", "zerodha", "upstox", "kuvera", "paytm money", "angel one", "mutual fund", "sip payment",
            "insurance premium", "lic",
        ),
    )

    fun categorize(merchant: String?): TransactionCategory {
        if (merchant.isNullOrBlank()) return TransactionCategory.UNCATEGORIZED
        val lower = merchant.lowercase().trim()
        EXACT_MATCH_RULES[lower]?.let { return it }
        for ((category, keywords) in RULES) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return TransactionCategory.UNCATEGORIZED
    }
}
