package dev.kosh.financetracker.data.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Original source rows stay intact; only the canonical record contributes to totals. */
@Entity(tableName = "transaction_source_links")
data class TransactionSourceLink(
    @PrimaryKey val observationId: Long,
    val canonicalId: Long,
)

@Dao
interface TransactionSourceLinkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(link: TransactionSourceLink)

    @Query("SELECT * FROM transaction_source_links")
    suspend fun getAll(): List<TransactionSourceLink>
}
