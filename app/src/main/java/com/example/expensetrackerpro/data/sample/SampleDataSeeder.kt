package com.example.expensetrackerpro.data.sample

import com.example.expensetrackerpro.data.local.dao.SampleDataDao
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.entity.TransactionEntity
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.DefaultCategories
import com.example.expensetrackerpro.domain.repository.SampleDataRepository
import javax.inject.Inject

/** Writes generated transactions into the database. */
class SampleDataSeeder @Inject constructor(
    private val sampleDataDao: SampleDataDao,
    private val generateTransactions: SampleTransactionGenerator,
    private val names: DefaultCategoryNames,
    private val localeProvider: AppLocaleProvider
) : SampleDataRepository {

    override suspend fun loadSampleData(): Outcome<Int> =
        storageOutcome {
            val locale = localeProvider.current()
            val defaults = DefaultCategories.all.map { builtIn ->
                CategoryEntity(
                    name = names.nameOf(builtIn, locale),
                    type = builtIn.type,
                    icon = builtIn.icon,
                    isCustom = false,
                    builtInKey = builtIn
                )
            }
            // The samples name built-in categories, any of which may have been renamed or deleted
            val categories = sampleDataDao.addMissingCategories(defaults)
            val idsByKey = categories.mapNotNull { category -> category.builtInKey?.let { it to category.id } }.toMap()
            val idsByName = categories.associate { (it.name to it.type) to it.id }

            val transactions = generateTransactions().mapNotNull { sample ->
                val categoryId = idsByKey[sample.category]
                    // A category under the built-in's name stands in for one that lost its key
                    ?: idsByName[names.nameOf(sample.category, locale) to sample.type]
                    ?: return@mapNotNull null
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
