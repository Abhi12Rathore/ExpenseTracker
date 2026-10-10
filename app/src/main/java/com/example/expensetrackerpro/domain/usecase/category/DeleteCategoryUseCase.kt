package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /** Fails with CategoryInUse, deleting nothing, when the category still has transactions. */
    suspend operator fun invoke(category: Category): Outcome<Unit> =
        categoryRepository.deleteCategory(category)
}
