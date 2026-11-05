package dev.kosh.financetracker.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, MerchantCategoryMappingEntity::class, TransactionSourceLink::class],
    version = 8,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
    abstract fun sourceLinkDao(): TransactionSourceLinkDao

    companion object {
        const val DB_NAME = "finance-tracker.db"
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionRef TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS transaction_source_links (observationId INTEGER NOT NULL, canonicalId INTEGER NOT NULL, PRIMARY KEY(observationId))")
            }
        }
    }
}
