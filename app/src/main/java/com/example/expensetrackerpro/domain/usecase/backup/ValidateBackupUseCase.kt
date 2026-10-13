package com.example.expensetrackerpro.domain.usecase.backup

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Backup
import com.example.expensetrackerpro.domain.models.BackupCategoryRecord
import com.example.expensetrackerpro.domain.models.BackupDocument
import com.example.expensetrackerpro.domain.models.BackupTransactionRecord
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import javax.inject.Inject

/**
 * Checks a backup file from top to bottom, so that restoring it can't leave the app with data
 * that doesn't add up. Nothing is corrected: one wrong value turns the whole file down.
 */
class ValidateBackupUseCase @Inject constructor(
    private val validateCategoryName: ValidateCategoryNameUseCase
) {

    /** Fails with BackupTooNew for a newer file version, and with BackupInvalid for anything else. */
    operator fun invoke(document: BackupDocument): Outcome<Backup> {
        if (document.format != Backup.FORMAT) return invalid
        if (document.version > Backup.VERSION) return Outcome.Failure(DomainError.BackupTooNew)
        if (document.version < FIRST_VERSION) return invalid

        val categories = mutableListOf<Category>()
        document.categories.forEach { record ->
            val category = record.toCategory() ?: return invalid
            // The same rules as for a name typed in the app, checked against the categories before it
            val name = validateCategoryName(category.name, category.type, editingId = null, existing = categories)
            if (name != CategoryNameValidation.VALID) return invalid
            categories += category
        }
        val categoriesById = categories.associateBy { it.id }
        if (categoriesById.size != categories.size) return invalid
        // Two categories can't both be the same built-in
        val builtInKeys = categories.mapNotNull { it.builtInKey }
        if (builtInKeys.distinct().size != builtInKeys.size) return invalid
        // With no category of a type, no transaction of that type could be added after the restore
        if (TransactionType.entries.any { type -> categories.none { it.type == type } }) return invalid

        val transactions = document.transactions.map { record ->
            val category = categoriesById[CategoryId(record.categoryId)] ?: return invalid
            record.toTransaction(category) ?: return invalid
        }

        return Outcome.Success(
            Backup(
                createdAtMillis = document.createdAtMillis,
                appVersion = document.appVersion,
                categories = categories,
                transactions = transactions
            )
        )
    }

    private fun BackupCategoryRecord.toCategory(): Category? {
        // The database gives out ids from 1; it would replace any other with one of its own
        if (id <= 0) return null

        val categoryType = type.toTransactionType() ?: return null
        // A key this version does not know is dropped: the category keeps the name in the file
        val builtIn = builtInKey?.let(BuiltInCategory::findByKey)
        if (builtIn != null && (isCustom || builtIn.type != categoryType)) return null

        return Category(
            id = CategoryId(id),
            name = name,
            type = categoryType,
            icon = CategoryIcon.findByKey(icon) ?: return null,
            isCustom = isCustom,
            builtInKey = builtIn
        )
    }

    /** Null unless the amount is positive and the type is known and the same as [category]'s. */
    private fun BackupTransactionRecord.toTransaction(category: Category): Transaction? {
        if (amountPaise <= 0 || type.toTransactionType() != category.type) return null

        return Transaction(
            title = title,
            amount = Money(amountPaise),
            note = note,
            dateMillis = dateMillis,
            categoryId = category.id,
            categoryName = category.name,
            transactionType = category.type,
            categoryIcon = category.icon
        )
    }

    private fun String.toTransactionType(): TransactionType? =
        TransactionType.entries.find { it.name == this }

    private companion object {
        const val FIRST_VERSION = 1
        val invalid = Outcome.Failure(DomainError.BackupInvalid)
    }
}
