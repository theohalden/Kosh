package dev.kosh.financetracker.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.kosh.financetracker.core.model.TransactionCategory

/**
 * "Historical mapping" tier from the categorization decision order (user rule >
 * merchant rule > historical mapping > keyword rule > generic rule > UNKNOWN):
 * once a person picks a category for a merchant via Review or Detail, remember it
 * so the same merchant never needs re-reviewing again. [merchant] is stored
 * normalized (trimmed, lowercased) so matching is exact but case/whitespace
 * insensitive.
 */
@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMappingEntity(
    @PrimaryKey val merchant: String,
    val category: TransactionCategory,
    val updatedAtEpochMillis: Long,
)
