package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class ReassignAndDeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /**
     * Moves the transactions of [category] to [target], then deletes [category].
     * Returns false, changing nothing, when [target] is the same category or of another type.
     */
    suspend operator fun invoke(category: Category, target: Category): Boolean {
        if (category.id == target.id || category.type != target.type) return false

        return categoryRepository.deleteCategoryReassigning(category, target)
    }
}
