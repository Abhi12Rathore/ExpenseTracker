package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.InvalidInputReason
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class ReassignAndDeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /**
     * Moves the transactions of [category] to [target], then deletes [category].
     * Fails with InvalidInput, changing nothing, when [target] is the same category or of another type.
     */
    suspend operator fun invoke(category: Category, target: Category): Outcome<Unit> {
        if (category.id == target.id || category.type != target.type) {
            return Outcome.Failure(DomainError.InvalidInput(InvalidInputReason.INCOMPATIBLE_REASSIGN_TARGET))
        }

        return categoryRepository.deleteCategoryReassigning(category, target)
    }
}
