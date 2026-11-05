package dev.kosh.financetracker.data.repository

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.kosh.financetracker.core.statement.StatementParserRegistry
import dev.kosh.financetracker.core.statement.StatementValidation
import dev.kosh.financetracker.core.statement.toTransaction
import dev.kosh.financetracker.data.database.FinanceDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import java.math.BigDecimal

/** Opt-in local audit. Private inputs are never fixtures, logs, or repository assets. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalStatementAuditTest {
    @Test fun `private confirmed corrections produce expected totals without rewriting evidence`() = runBlocking {
        val snapshot = System.getenv("KOSH_CONFIRMED_DATABASE")
        assumeTrue("Private confirmed audit is opt-in", snapshot != null)
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.getDatabasePath("local-confirmed-audit.db")
        file.parentFile!!.mkdirs()
        File(requireNotNull(snapshot)).copyTo(file, overwrite = true)
        val db = Room.databaseBuilder(context, FinanceDatabase::class.java, "local-confirmed-audit.db")
            .addMigrations(FinanceDatabase.MIGRATION_7_8).build()
        try {
            val before = db.transactionDao().getAllObservations()
            val repo = RoomTransactionRepository(db.transactionDao(), db.merchantCategoryMappingDao(), db)
            val all = repo.observeTransactions().first()
            val confirmed = all.filter { dev.kosh.financetracker.core.finance.ConfirmedClassification.type(it) != null }
            assertEquals(4, confirmed.size)
            assertEquals(3, confirmed.count { it.type == dev.kosh.financetracker.core.model.TransactionType.TRANSFER })
            assertEquals(1, confirmed.count { it.type == dev.kosh.financetracker.core.model.TransactionType.LOAN_EMI })
            val summary = dev.kosh.financetracker.core.finance.FinanceCalculator.summaryForMonth(all,
                java.time.YearMonth.of(2026, 11), java.time.ZoneId.of("Asia/Kolkata"))
            assertEquals(0, BigDecimal(requireNotNull(System.getenv("KOSH_CONFIRMED_SPENDING"))).compareTo(summary.expense))
            assertEquals(0, BigDecimal(requireNotNull(System.getenv("KOSH_CONFIRMED_INCOME"))).compareTo(summary.income))
            for (row in confirmed) assertEquals(row, repo.observeTransaction(row.id).first())
            assertEquals(before, db.transactionDao().getAllObservations())
            println("LOCAL_CONFIRMED_AUDIT confirmed=${confirmed.size} spending=${summary.expense} income=${summary.income} evidenceUnchanged=true")
        } finally { db.close(); context.deleteDatabase("local-confirmed-audit.db") }
    }

    @Test fun `private card examples are corrected consistently without rewriting records`() = runBlocking {
        val snapshot = System.getenv("KOSH_CARD_AUDIT_DATABASE")
        assumeTrue("Private card audit is opt-in", snapshot != null)
        val context = ApplicationProvider.getApplicationContext<Application>()
        val file = context.getDatabasePath("local-card-audit.db")
        file.parentFile!!.mkdirs()
        File(requireNotNull(snapshot)).copyTo(file, overwrite = true)
        val db = Room.databaseBuilder(context, FinanceDatabase::class.java, "local-card-audit.db")
            .addMigrations(FinanceDatabase.MIGRATION_7_8).build()
        try {
            val before = db.transactionDao().getAllObservations()
            val repo = RoomTransactionRepository(db.transactionDao(), db.merchantCategoryMappingDao(), db)
            val all = repo.observeTransactions().first()
            val purchaseId = requireNotNull(System.getenv("KOSH_CARD_PURCHASE_ID")).toLong()
            val reminderId = requireNotNull(System.getenv("KOSH_CARD_REMINDER_ID")).toLong()
            val receiptIds = requireNotNull(System.getenv("KOSH_CARD_RECEIPT_IDS")).split(',').map { it.toLong() }
            assertTrue(all.none { it.id == reminderId })
            assertEquals(dev.kosh.financetracker.core.model.TransactionType.CREDIT_CARD_PURCHASE, all.single { it.id == purchaseId }.type)
            for (id in receiptIds) {
                assertEquals(dev.kosh.financetracker.core.model.TransactionType.CREDIT_CARD_PAYMENT, all.single { it.id == id }.type)
                assertEquals(all.single { it.id == id }, repo.observeTransaction(id).first())
            }
            val summary = dev.kosh.financetracker.core.finance.FinanceCalculator.summaryForMonth(
                all, java.time.YearMonth.of(2026, 11), java.time.ZoneId.of("Asia/Kolkata"),
            )
            assertEquals(0, BigDecimal(requireNotNull(System.getenv("KOSH_CARD_EXPECTED_SPENDING"))).compareTo(summary.expense))
            assertEquals(before, db.transactionDao().getAllObservations())
            println("LOCAL_CARD_AUDIT storedRecords=${before.size} NovemberSpending=${summary.expense} originalRowsUnchanged=true")
        } finally {
            db.close()
            context.deleteDatabase("local-card-audit.db")
        }
    }

    @Test fun `private statements reconcile and import twice without adding transactions`() = runBlocking {
        val paths = listOf("KOSH_AUDIT_HDFC", "KOSH_AUDIT_KOTAK", "KOSH_AUDIT_SLICE").map { System.getenv(it) }
        assumeTrue("Private statement audit is opt-in", paths.all { it != null })
        val counts = requireNotNull(System.getenv("KOSH_AUDIT_COUNTS")).split(',').map { it.toInt() }
        val context = ApplicationProvider.getApplicationContext<Application>()
        val databaseFile = context.getDatabasePath("local-audit.db")
        databaseFile.parentFile!!.mkdirs()
        val snapshot = System.getenv("KOSH_AUDIT_DATABASE")
        if (snapshot != null) File(snapshot).copyTo(databaseFile, overwrite = true)
        val db = Room.databaseBuilder(context, FinanceDatabase::class.java, "local-audit.db")
            .addMigrations(FinanceDatabase.MIGRATION_7_8).build()
        var audited = false
        try {
            val before = db.transactionDao().getAllObservations().associateBy { it.id }
            val repository = RoomTransactionRepository(db.transactionDao(), db.merchantCategoryMappingDao(), db)
            val allRows = paths.mapIndexed { index, path ->
                val file = File(requireNotNull(path))
                val original = file.readBytes()
                val hash = MessageDigest.getInstance("SHA-256").digest(original)
                val rows = StatementParserRegistry().parse(file.name, original)
                assertEquals("Parsed count for source ${index + 1}", counts[index], rows.size)
                assertTrue("Balance validation for source ${index + 1}", StatementValidation.check(rows).isEmpty())
                repository.importStatement(rows)
                assertArrayEquals(hash, MessageDigest.getInstance("SHA-256").digest(file.readBytes()))
                rows
            }
            val first = db.transactionDao().getAll()
            for (rows in allRows) {
                val bank = rows.first().bank
                val canonical = first.filter { it.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT && it.bank.equals(bank, true) }
                assertEquals("Bank row count: $bank", rows.size, canonical.size)
                for (direction in listOf(dev.kosh.financetracker.core.model.TransactionDirection.DEBIT, dev.kosh.financetracker.core.model.TransactionDirection.CREDIT)) {
                    val expected = rows.filter { it.direction == direction }.fold(BigDecimal.ZERO) { total, row -> total + row.amount }
                    val actual = canonical.filter { it.direction == direction }.fold(BigDecimal.ZERO) { total, row -> total + row.amount }
                    assertEquals("Bank total: $bank $direction", 0, expected.compareTo(actual))
                    println("LOCAL_BANK_CONTROL bank=$bank direction=$direction amount=$actual rows=${canonical.count { it.direction == direction }}")
                }
            }
            val projected = repository.observeTransactions().first()
            for (rows in allRows) {
                val bank = rows.first().bank
                val bankRows = projected.filter { it.bank.equals(bank, true) && it.accountSuffix == rows.first().accountSuffix && it.reconciliationNote == null }
                val zone = java.time.ZoneId.systemDefault()
                val dates = projected.filter { it.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT && it.bank.equals(bank, true) }.map { it.timestamp.atZone(zone).toLocalDate() }
                for (direction in listOf(dev.kosh.financetracker.core.model.TransactionDirection.DEBIT, dev.kosh.financetracker.core.model.TransactionDirection.CREDIT)) {
                    val actual = bankRows.filter { it.direction == direction && it.timestamp.atZone(zone).toLocalDate() in dates.min()..dates.max() }.sumOf { it.amount }
                    val expected = rows.filter { it.direction == direction }.sumOf { it.amount }
                    assertEquals("Projected bank control: $bank $direction", 0, expected.compareTo(actual))
                }
            }
            println("LOCAL_UNMATCHED excluded=${projected.count { it.reconciliationNote != null }}")
            System.getenv("KOSH_AUDIT_PROJECTED_CSV")?.let { path ->
                val header = "id,bank,accountSuffix,timestamp,amount,direction,type,source,excluded"
                val lines = projected.map { row ->
                    listOf(row.id, row.bank.orEmpty(), row.accountSuffix.orEmpty(), row.timestamp, row.amount, row.direction, row.type, row.source, row.reconciliationNote != null).joinToString(",")
                }
                File(path).parentFile!!.mkdirs()
                File(path).writeText((listOf(header) + lines).joinToString("\n"))
            }
            println("LOCAL_LEDGER_PROJECTION visible=${projected.size} transferLegs=${projected.count { it.type == dev.kosh.financetracker.core.model.TransactionType.TRANSFER }} unresolvedPurposes=${projected.count { it.type == dev.kosh.financetracker.core.model.TransactionType.UNKNOWN }}")
            val statementCount = first.count { it.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT }
            val links = db.sourceLinkDao().getAll().associate { it.observationId to it.canonicalId }
            val importedCanonicalIds = mutableSetOf<Long>()
            for (rows in allRows) {
                val occurrences = mutableMapOf<String, Int>()
                for (row in rows) {
                    val key = requireNotNull(row.toTransaction().sourceMessageId)
                    val occurrence = occurrences.getOrDefault(key, 0)
                    occurrences[key] = occurrence + 1
                    val record = requireNotNull(db.transactionDao().getBySourceId("$key:$occurrence"))
                    importedCanonicalIds += links[record.id] ?: record.id
                }
            }
            assertEquals("Each supplied statement row has a distinct canonical transaction", counts.sum(), importedCanonicalIds.size)
            val unmatchedOldStatements = first.filter {
                it.source == dev.kosh.financetracker.core.model.TransactionSource.STATEMENT && it.id !in importedCanonicalIds
            }
            for (row in unmatchedOldStatements) {
                println("LOCAL_AUDIT_UNRESOLVED id=${row.id} bank=${row.bank} timestamp=${row.timestampEpochMillis} amount=${row.amount} direction=${row.direction} balance=${row.balanceAfter}")
            }
            val observations = db.transactionDao().getAllObservations()
            for (rows in allRows) repository.importStatement(rows)
            assertEquals(first, db.transactionDao().getAll())
            assertEquals(observations, db.transactionDao().getAllObservations())
            // Source observations, including every prior classification and
            // user category, are immutable during statement reconciliation.
            val after = db.transactionDao().getAllObservations().associateBy { it.id }
            for ((id, old) in before) {
                val current = requireNotNull(after[id])
                assertEquals(old, current)
            }
            println("LOCAL_AUDIT parsed=${allRows.sumOf { it.size }} original=${before.size} active=${first.size} statementTransactions=$statementCount retainedObservations=${observations.size} links=${db.sourceLinkDao().getAll().size}")
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
            audited = true
        } finally {
            db.close()
            val output = System.getenv("KOSH_AUDIT_OUTPUT_DATABASE")
            if (audited && output != null) {
                File(output).parentFile!!.mkdirs()
                databaseFile.copyTo(File(output), overwrite = true)
            }
            context.deleteDatabase("local-audit.db")
        }
    }
}
