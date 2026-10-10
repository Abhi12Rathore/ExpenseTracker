package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.map
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Checks a draft against what is stored, then adds it or updates the category it edits. */
class SaveCategoryUseCase @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val validateCategoryName: ValidateCategoryNameUseCase,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase
) {

    /**
     * Returns the id the category is stored under. Fails with NotFound when the category being
     * edited is gone, with DuplicateName or InvalidInput when the name can't be used, and with
     * StorageFailure when the database could not write it.
     */
    suspend operator fun invoke(draft: CategoryDraft): Outcome<Int> {
        val existing = getCategories().first()
        val stored = draft.id?.let { id ->
            existing.find { it.id == id } ?: return Outcome.Failure(DomainError.NotFound)
        }
        // An edit keeps its type: its transactions were saved as that type
        val type = stored?.type ?: draft.type

        when (validateCategoryName(draft.name, type, draft.id, existing)) {
            CategoryNameValidation.VALID -> Unit
            CategoryNameValidation.BLANK ->
                return Outcome.Failure(DomainError.InvalidInput(InvalidInputReason.NAME_BLANK))
            CategoryNameValidation.TOO_LONG ->
                return Outcome.Failure(DomainError.InvalidInput(InvalidInputReason.NAME_TOO_LONG))
            CategoryNameValidation.DUPLICATE -> return Outcome.Failure(DomainError.DuplicateName)
        }

        val name = draft.name.trim()
        if (stored != null) {
            // Built-in or custom stays as it was
            return updateCategory(stored.copy(name = name, icon = draft.icon)).map { stored.id }
        }

        return addCategory(Category(name = name, type = type, icon = draft.icon, isCustom = true))
    }
}
