package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryDraft
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.SaveCategoryResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Checks a draft against what is stored, then adds it or updates the category it edits. */
class SaveCategoryUseCase @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val validateCategoryName: ValidateCategoryNameUseCase,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase
) {

    suspend operator fun invoke(draft: CategoryDraft): SaveCategoryResult {
        val existing = getCategories().first()
        val stored = draft.id?.let { id ->
            existing.find { it.id == id } ?: return SaveCategoryResult.NotFound
        }
        // An edit keeps its type: its transactions were saved as that type
        val type = stored?.type ?: draft.type

        val validation = validateCategoryName(draft.name, type, draft.id, existing)
        if (validation != CategoryNameValidation.VALID) return SaveCategoryResult.Invalid(validation)

        val name = draft.name.trim()
        if (stored != null) {
            // Built-in or custom stays as it was
            updateCategory(stored.copy(name = name, icon = draft.icon))
            return SaveCategoryResult.Saved(stored.id)
        }

        return SaveCategoryResult.Saved(
            addCategory(Category(name = name, type = type, icon = draft.icon, isCustom = true))
        )
    }
}
