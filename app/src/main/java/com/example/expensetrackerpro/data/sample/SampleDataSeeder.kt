package com.example.expensetrackerpro.data.sample

import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.data.mapper.toEntity
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import javax.inject.Inject

/** Writes generated transactions into the database. */
class SampleDataSeeder @Inject constructor(
    private val sampleDataDao: SampleDataDao,
    private val generateTransactions: SampleTransactionGenerator
) : SampleDataRepository {

    override suspend fun loadSampleData(): Outcome<Int> =
        storageOutcome {
            // The samples name default categories, any of which may have been renamed or deleted
            val categories = sampleDataDao.addMissingCategories(DefaultCategories.all.map { it.toEntity() })
            val categoryIds = categories.associate { (it.name to it.type) to it.id }

            val transactions = generateTransactions().mapNotNull { sample ->
                val categoryId = categoryIds[sample.categoryName to sample.type] ?: return@mapNotNull null
                TransactionEntity(
                    title = sample.title,
                    amountPaise = sample.amount.paise,
                    note = sample.note,
                    dateMillis = sample.dateMillis,
                    categoryId = categoryId,
                    transactionType = sample.type
                )
            }
            sampleDataDao.replaceAllTransactions(transactions)

            transactions.size
        }
}
