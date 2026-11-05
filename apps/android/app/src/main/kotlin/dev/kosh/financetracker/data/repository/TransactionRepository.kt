package dev.kosh.financetracker.data.repository

import androidx.room.withTransaction
import dev.kosh.financetracker.core.categorization.CategoryEngine
import dev.kosh.financetracker.core.classifier.IncomeClassifier
import dev.kosh.financetracker.core.classifier.CardTransactionClassifier
import dev.kosh.financetracker.core.finance.TransferDetector
import dev.kosh.financetracker.core.finance.SourceReconciliation
import dev.kosh.financetracker.core.finance.LedgerProjection
import dev.kosh.financetracker.core.parser.TransactionMessageStatus
import dev.kosh.financetracker.core.model.TransactionSource
import dev.kosh.financetracker.data.database.TransactionSourceLink
import dev.kosh.financetracker.core.statement.StatementValidation
import dev.kosh.financetracker.core.model.Transaction
import dev.kosh.financetracker.core.model.TransactionCategory
import dev.kosh.financetracker.core.model.TransactionDirection
import dev.kosh.financetracker.core.model.TransactionType
import dev.kosh.financetracker.core.statement.StatementImportResult
import dev.kosh.financetracker.core.statement.StatementTransaction
import dev.kosh.financetracker.core.statement.toTransaction
import dev.kosh.financetracker.data.database.MerchantCategoryMappingDao
import dev.kosh.financetracker.data.database.FinanceDatabase
import dev.kosh.financetracker.data.database.MerchantCategoryMappingEntity
import dev.kosh.financetracker.data.database.TransactionDao
import dev.kosh.financetracker.data.database.toDomain
import dev.kosh.financetracker.data.database.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>
    suspend fun save(transaction: Transaction): Long

    /** Returns true if actually inserted, false if a transaction from this source already existed. */
    suspend fun saveIfNew(transaction: Transaction): Boolean

    /** Re-runs transfer detection across all stored transactions and reclassifies matches. */
    suspend fun reconcileTransfers()

    /** Re-runs the salary/income heuristic across all stored CREDIT transactions —
     * fixes rows already imported under an older, looser classification rule. */
    suspend fun reconcileIncomeClassification()

    /** Re-runs CategoryEngine against every still-UNCATEGORIZED row — picks up new
     * keyword-rule coverage retroactively without touching rows a user already
     * confirmed via Review/Detail (those are no longer UNCATEGORIZED). */
    suspend fun reconcileCategories()

    /** Sets [id]'s category and, when it has a merchant, remembers the choice so
     * every other (and future) transaction from that same merchant is categorized
     * the same way without needing review again. */
    suspend fun setCategory(id: Long, category: TransactionCategory)

    /** Adds statement observations without deleting existing records. Reference-backed
     * SMS matches are retained as linked evidence and counted once. */
    suspend fun importStatement(transactions: List<StatementTransaction>): List<StatementImportResult.Imported>
}

private fun normalizeMerchant(merchant: String?): String? =
    merchant?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val database: FinanceDatabase,
) : TransactionRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { entities ->
            LedgerProjection.project(entities.map { it.toDomain() })
        }

    override fun observeTransaction(id: Long): Flow<Transaction?> =
        combine(dao.observeById(id), dao.observeAll()) { entity, all ->
            entity?.let { selected -> LedgerProjection.project(all.map { row -> row.toDomain() }).firstOrNull { it.id == selected.id } }
        }

    override suspend fun save(transaction: Transaction): Long =
        dao.insert(transaction.toEntity())

    override suspend fun saveIfNew(transaction: Transaction): Boolean = database.withTransaction {
        val id = dao.insertIfNew(transaction.toEntity())
        if (id == -1L) return@withTransaction false
        reconcileSourceObservations()
        database.sourceLinkDao().getAll().none { it.observationId == id }
    }

    private suspend fun reconcileSourceObservations(): Int {
        val all = dao.getAll().map { it.toDomain() }
        val statements = all.filter { it.source == TransactionSource.STATEMENT }
        var linked = 0
        for (sms in all.filter { it.source == TransactionSource.SMS }) {
            val candidates = statements.filter { SourceReconciliation.matches(sms, it) }
            // Ambiguous repeated payments stay visible rather than losing a payment.
            val canonical = candidates.singleOrNull() ?: continue
            database.sourceLinkDao().insert(TransactionSourceLink(sms.id, canonical.id))
            linked++
        }
        return linked
    }

    override suspend fun reconcileTransfers() {
        val all = CardTransactionClassifier.projectAll(dao.getAll().map { it.toDomain() })
        val transferIds = TransferDetector.findTransferPairs(all)
        if (transferIds.isNotEmpty()) {
            dao.updateType(transferIds.toList(), TransactionType.TRANSFER)
        }
    }

    override suspend fun reconcileIncomeClassification() {
        val all = dao.getAll().map { it.toDomain() }
        val toIncome = mutableListOf<Long>()
        val toUnknown = mutableListOf<Long>()

        for (transaction in all) {
            if (transaction.direction != TransactionDirection.CREDIT) continue
            if (transaction.type != TransactionType.INCOME && transaction.type != TransactionType.UNKNOWN) continue

            val shouldBeIncome = IncomeClassifier.isLikelySalary(transaction.merchant, transaction.rawSourceText)
            when {
                shouldBeIncome && transaction.type != TransactionType.INCOME -> toIncome += transaction.id
                !shouldBeIncome && transaction.type == TransactionType.INCOME -> toUnknown += transaction.id
            }
        }

        if (toIncome.isNotEmpty()) dao.updateType(toIncome, TransactionType.INCOME)
        if (toUnknown.isNotEmpty()) dao.updateType(toUnknown, TransactionType.UNKNOWN)
    }

    override suspend fun reconcileCategories() {
        val all = dao.getAll().map { it.toDomain() }
        val learnedMappings = merchantCategoryMappingDao.getAll()
            .associate { it.merchant to it.category }
        val idsByNewCategory = mutableMapOf<TransactionCategory, MutableList<Long>>()

        for (transaction in all) {
            if (transaction.category != TransactionCategory.UNCATEGORIZED && transaction.category != null) continue
            // Historical mapping (a category the user already picked for this exact
            // merchant) takes priority over the generic keyword-rule tier.
            val recategorized = normalizeMerchant(transaction.merchant)?.let { learnedMappings[it] }
                ?: CategoryEngine.categorize(transaction.merchant)
            if (recategorized != TransactionCategory.UNCATEGORIZED) {
                idsByNewCategory.getOrPut(recategorized) { mutableListOf() } += transaction.id
            }
        }

        idsByNewCategory.forEach { (category, ids) -> dao.updateCategories(ids, category) }
    }

    override suspend fun setCategory(id: Long, category: TransactionCategory) {
        dao.updateCategory(id, category)

        val merchant = normalizeMerchant(dao.getByIdOnce(id)?.merchant) ?: return
        merchantCategoryMappingDao.upsert(
            MerchantCategoryMappingEntity(
                merchant = merchant,
                category = category,
                updatedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )

        // Immediately propagate to every other still-unresolved transaction from the
        // same merchant, not just future imports — the whole point is "never ask
        // about this merchant again."
        val sameMerchantIds = dao.getAll()
            .filter {
                it.id != id &&
                    normalizeMerchant(it.merchant) == merchant &&
                    (it.category == TransactionCategory.UNCATEGORIZED || it.category == null)
            }
            .map { it.id }
        if (sameMerchantIds.isNotEmpty()) {
            dao.updateCategories(sameMerchantIds, category)
        }
    }

    override suspend fun importStatement(transactions: List<StatementTransaction>): List<StatementImportResult.Imported> {
        if (transactions.isEmpty()) return emptyList()
        require(transactions.all { it.bank.isNotBlank() && it.accountSuffix.isNotBlank() }) {
            "Statement rows must identify a bank and account"
        }
        val zone = ZoneId.systemDefault()
        require(StatementValidation.check(transactions).isEmpty()) {
            "Statement balances do not reconcile; no records were imported"
        }
        return database.withTransaction {
            val results = mutableListOf<StatementImportResult.Imported>()

            val accounts = transactions.groupBy { it.bank.trim().lowercase(Locale.ROOT) to it.accountSuffix.trim() }
            for ((account, rows) in accounts) {
                val (_, accountSuffix) = account
                val periodStart = rows.minOf { it.timestamp }.atZone(zone).toLocalDate()
                val periodEnd = rows.maxOf { it.timestamp }.atZone(zone).toLocalDate()

                var addedCount = 0
                var existingCount = 0
                var linkedCount = 0
                val priorStatements = dao.getAll().map { it.toDomain() }
                    .filter { it.source == TransactionSource.STATEMENT }.toMutableList()
                val priorLinks = database.sourceLinkDao().getAll().associate { it.observationId to it.canonicalId }
                val occurrences = mutableMapOf<String, Int>()
                for (row in rows) {
                    val transaction = row.toTransaction()
                    val key = requireNotNull(transaction.sourceMessageId)
                    val occurrence = occurrences.getOrDefault(key, 0)
                    occurrences[key] = occurrence + 1
                    val sourceId = "$key:$occurrence"
                    val existing = dao.getBySourceId(sourceId)
                    if (existing != null) {
                        existingCount++
                        val canonicalId = priorLinks[existing.id] ?: existing.id
                        priorStatements.removeAll { it.id == canonicalId }
                        continue
                    }
                    val ranked = priorStatements.mapNotNull { stored ->
                        SourceReconciliation.statementMatchRank(transaction, stored, zone)?.let { it to stored }
                    }
                    val bestRank = ranked.minOfOrNull { it.first }
                    val candidates = ranked.filter { it.first == bestRank }.map { it.second }
                    // Identical source rows are allocated one-to-one, never collapsed
                    // within the incoming statement. Different tied narrations require review.
                    val canonical = candidates.singleOrNull() ?: candidates.takeIf { matches ->
                        matches.isNotEmpty() && matches.map { it.rawSourceText }.distinct().size == 1 &&
                            matches.first().rawSourceText == transaction.rawSourceText
                    }?.minByOrNull { it.id }
                    dao.insertStatementRows(listOf(transaction.copy(sourceMessageId = sourceId).toEntity()))
                    if (canonical != null) {
                        val observation = requireNotNull(dao.getBySourceId(sourceId))
                        database.sourceLinkDao().insert(TransactionSourceLink(observation.id, canonical.id))
                        priorStatements.removeAll { it.id == canonical.id }
                        existingCount++
                        linkedCount++
                    } else addedCount++
                }
                linkedCount += reconcileSourceObservations()
                val unresolvedStatementCount = priorStatements.count { stored ->
                    val day = stored.timestamp.atZone(zone).toLocalDate()
                    stored.bank?.trim().equals(rows.first().bank.trim(), true) &&
                        stored.accountSuffix == accountSuffix && day >= periodStart && day <= periodEnd
                }
                val unresolvedSmsCount = dao.getAll().count { stored ->
                    val day = Instant.ofEpochMilli(stored.timestampEpochMillis).atZone(zone).toLocalDate()
                    stored.source == TransactionSource.SMS && day >= periodStart && day <= periodEnd &&
                        (stored.bank == null || stored.bank.trim().equals(rows.first().bank.trim(), true)) &&
                        (stored.accountSuffix == null || stored.accountSuffix == accountSuffix)
                }

                results += StatementImportResult.Imported(
                    bank = rows.first().bank.trim(),
                    accountSuffix = accountSuffix,
                    importedCount = rows.size,
                    replacedCount = 0,
                    addedCount = addedCount,
                    existingCount = existingCount,
                    linkedCount = linkedCount,
                    unresolvedSmsCount = unresolvedSmsCount,
                    unresolvedStatementCount = unresolvedStatementCount,
                    periodStart = periodStart,
                    periodEnd = periodEnd,
                )
            }

            // Reconciliation links source observations; it must not rewrite user
            // categories or legacy transaction types as a side effect of importing.
            results
        }
    }
}
