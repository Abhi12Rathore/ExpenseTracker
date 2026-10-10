package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import javax.inject.Inject

/**
 * A name must not be blank, run past [NAME_MAX_LENGTH], or repeat another category of the same
 * type. Names are compared without the space around them and ignoring case.
 */
class ValidateCategoryNameUseCase @Inject constructor() {

    operator fun invoke(
        name: String,
        type: TransactionType,
        // The category being renamed, which may keep its own name
        editingId: CategoryId?,
        existing: List<Category>
    ): CategoryNameValidation {
        val trimmed = name.trim()

        return when {
            trimmed.isEmpty() -> CategoryNameValidation.BLANK
            trimmed.length > NAME_MAX_LENGTH -> CategoryNameValidation.TOO_LONG
            existing.any {
                it.id != editingId && it.type == type && it.name.trim().equals(trimmed, ignoreCase = true)
            } -> CategoryNameValidation.DUPLICATE
            else -> CategoryNameValidation.VALID
        }
    }

    companion object {
        const val NAME_MAX_LENGTH = 30
    }
}
