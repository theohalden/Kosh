package dev.kosh.financetracker.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import dev.kosh.financetracker.data.database.FinanceDatabase
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.database.TransactionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class StatementImportIntegrityTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), FinanceDatabase::class.java,
        ).build()
        repository = RoomTransactionRepository(database.transactionDao(), database.merchantCategoryMappingDao(), database)
    }

    @After fun tearDown() { database.close() }

    private fun row(bank: String = "HDFC", description: String = "NEW", reference: String? = null) =
        StatementTransaction(
            timestamp = Instant.parse("2026-10-01T06:00:00Z"), amount = BigDecimal("100.00"),
            direction = TransactionDirection.DEBIT, description = description, referenceNo = reference,
            balanceAfter = null, bank = bank, accountSuffix = "1234",
        )

    @Test fun `additive import preserves existing records and other banks with the same suffix`() = runBlocking {
        val old = row(description = "OLD").toTransaction()
        val other = row(bank = "Kotak", description = "OTHER").toTransaction()
        val unknown = row(description = "UNKNOWN").toTransaction().copy(bank = null, source = TransactionSource.SMS)
        repository.save(old)
        repository.save(other)
        repository.save(unknown)

        val result = repository.importStatement(listOf(row()))

        val stored = database.transactionDao().getAll()
        assertEquals(0, result.single().replacedCount)
        assertEquals(setOf("OLD", "NEW", "OTHER", "UNKNOWN"), stored.map { it.rawMerchant }.toSet())
    }

    @Test fun `failed insert rolls back deletion and earlier inserted rows`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import BEFORE INSERT ON transactions " +
                "WHEN NEW.rawMerchant = 'FAIL' BEGIN SELECT RAISE(ABORT, 'forced import failure'); END",
        )
        val result = runCatching { repository.importStatement(listOf(row(), row(description = "FAIL"))) }

        assertTrue(result.isFailure)
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `failure in later account rolls back every imported account`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import BEFORE INSERT ON transactions " +
                "WHEN NEW.rawMerchant = 'FAIL' BEGIN SELECT RAISE(ABORT, 'forced import failure'); END",
        )
        val result = runCatching {
            repository.importStatement(listOf(row(), row(bank = "Kotak", description = "FAIL")))
        }

        assertTrue(result.isFailure)
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `rows without references and exact duplicate rows survive repeat import`() = runBlocking {
        val rows = listOf(row(description = "ONE"), row(description = "TWO"), row(description = "TWO"))
        repository.importStatement(rows)
        val first = database.transactionDao().getAll()
        assertEquals(3, first.size)
        assertEquals(3, first.map { it.sourceMessageId }.distinct().size)

        repository.importStatement(rows.reversed())
        val second = database.transactionDao().getAll()
        assertEquals(3, second.size)
        assertEquals(first.map { it.sourceMessageId }.toSet(), second.map { it.sourceMessageId }.toSet())
    }

    @Test fun `same suffix at two banks is imported as two separate accounts`() = runBlocking {
        val results = repository.importStatement(listOf(row(), row(bank = "Kotak")))
        assertEquals(setOf("HDFC", "Kotak"), results.map { it.bank }.toSet())
        assertEquals(2, database.transactionDao().getAll().size)
    }

    @Test fun `replacement preserves rows outside the imported dates`() = runBlocking {
        val outside = row(description = "OUTSIDE").copy(timestamp = Instant.parse("2026-09-01T06:00:00Z"))
        repository.save(outside.toTransaction())
        repository.importStatement(listOf(row()))
        assertEquals(setOf("NEW", "OUTSIDE"), database.transactionDao().getAll().map { it.rawMerchant }.toSet())
    }

    @Test fun `cancellation after insert restores the original account data`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        val dao = database.transactionDao()
        val cancellingDao = object : TransactionDao by dao {
            override suspend fun insertStatementRows(transactions: List<TransactionEntity>) {
                dao.insertStatementRows(transactions)
                throw CancellationException("cancelled during import")
            }
        }
        val cancellingRepository = RoomTransactionRepository(cancellingDao, database.merchantCategoryMappingDao(), database)

        val result = runCatching { cancellingRepository.importStatement(listOf(row())) }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertEquals(listOf("ORIGINAL"), dao.getAll().map { it.rawMerchant })
    }

    @Test fun `bank case differences preserve existing account records`() = runBlocking {
        repository.save(row(bank = "hdfc", description = "ORIGINAL").toTransaction())
        val result = repository.importStatement(listOf(row(bank = " HDFC ")))
        assertEquals(0, result.single().replacedCount)
        assertEquals(setOf("ORIGINAL", "NEW"), database.transactionDao().getAll().map { it.rawMerchant }.toSet())
    }

    @Test fun `invalid bank identity is rejected before any deletion`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        val result = runCatching { repository.importStatement(listOf(row(), row(bank = " "))) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `repeated reference numbers do not overwrite distinct rows`() = runBlocking {
        repository.importStatement(listOf(row(reference = "REF"), row(description = "OTHER", reference = "REF")))
        assertEquals(2, database.transactionDao().getAll().size)
    }

    @Test fun `empty import leaves existing data alone`() = runBlocking {
        repository.save(row(description = "ORIGINAL").toTransaction())
        assertTrue(repository.importStatement(emptyList()).isEmpty())
        assertEquals(listOf("ORIGINAL"), database.transactionDao().getAll().map { it.rawMerchant })
    }

    @Test fun `historical scheduled alerts are excluded without deleting or editing their records`() = runBlocking {
        val alert = row().toTransaction().copy(
            source = TransactionSource.SMS, sourceMessageId = "sms:scheduled",
            rawSourceText = "Rs.1593.00 will be debited from HDFC Bank Card 1234",
        )
        repository.save(alert)
        assertTrue(repository.observeTransactions().first().isEmpty())
        assertEquals(1, database.transactionDao().getAllObservations().size)
        assertEquals(alert.rawSourceText, database.transactionDao().getAllObservations().single().rawSourceText)
    }

    @Test fun `statement and repeated SMS scans count once and retain both source rows`() = runBlocking {
        val statement = row(description = "UPI-EXAMPLE-123456789012")
        val sms = statement.toTransaction().copy(source = TransactionSource.SMS, sourceMessageId = "sms:example")
        repository.saveIfNew(sms)
        repository.importStatement(listOf(statement))
        assertEquals(1, database.transactionDao().getAll().size)
        assertEquals(2, database.transactionDao().getAllObservations().size)
        assertFalse(repository.saveIfNew(sms))
        assertFalse(repository.saveIfNew(sms.copy(sourceMessageId = "sms:second-alert")))
        assertEquals(1, database.transactionDao().getAll().size)
        assertEquals(3, database.transactionDao().getAllObservations().size)
        repository.importStatement(listOf(statement))
        assertEquals(3, database.transactionDao().getAllObservations().size)
    }

    @Test fun `same amounts without references stay separate and manual entries survive`() = runBlocking {
        val manual = row().toTransaction().copy(source = TransactionSource.MANUAL, sourceMessageId = null)
        repository.save(manual)
        repository.saveIfNew(row().toTransaction().copy(source = TransactionSource.SMS, sourceMessageId = "sms:no-ref"))
        repository.importStatement(listOf(row()))
        assertEquals(3, database.transactionDao().getAll().size)
        assertTrue(database.sourceLinkDao().getAll().isEmpty())
    }

    @Test fun `ambiguous reference matches stay visible`() = runBlocking {
        val first = row(description = "UPI-SHOP-123456789012")
        repository.importStatement(listOf(first, first.copy(description = "UPI-OTHER-123456789012")))
        repository.saveIfNew(first.toTransaction().copy(source = TransactionSource.SMS, sourceMessageId = "sms:ambiguous"))
        assertEquals(3, database.transactionDao().getAll().size)
        assertTrue(database.sourceLinkDao().getAll().isEmpty())
    }

    @Test fun `legacy statement identities match repeated balance cycles by reference`() = runBlocking {
        val one = row(description = "UPI-SHOP-123456789012").copy(balanceAfter = BigDecimal("900"))
        val two = one.copy(description = "UPI-SHOP-123456789013")
        repository.save(one.toTransaction().copy(sourceMessageId = "legacy:one"))
        repository.save(two.toTransaction().copy(sourceMessageId = "legacy:two"))
        // No running balance is provided by this synthetic import so the two
        // transactions can model separate same-day cycles without missing intervening rows.
        repository.importStatement(listOf(one.copy(balanceAfter = null), two.copy(balanceAfter = null)))
        assertEquals(2, database.transactionDao().getAll().size)
        assertEquals(4, database.transactionDao().getAllObservations().size)
    }

    @Test fun `failed evidence link rolls back the entire import`() = runBlocking {
        val original = row(description = "UPI-EXAMPLE-123456789012").toTransaction()
            .copy(source = TransactionSource.SMS, sourceMessageId = "sms:original")
        repository.save(original)
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_link BEFORE INSERT ON transaction_source_links " +
                "BEGIN SELECT RAISE(ABORT, 'forced link failure'); END",
        )
        val result = runCatching { repository.importStatement(listOf(row(description = "UPI-EXAMPLE-123456789012"))) }
        assertTrue(result.isFailure)
        assertEquals(1, database.transactionDao().getAllObservations().size)
        assertEquals(original.rawSourceText, database.transactionDao().getAllObservations().single().rawSourceText)
        assertTrue(database.sourceLinkDao().getAll().isEmpty())
    }
}
