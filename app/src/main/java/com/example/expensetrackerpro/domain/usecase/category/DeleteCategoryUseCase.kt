package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /** Returns false when the category still has transactions and was not deleted. */
    suspend operator fun invoke(category: Category): Boolean =
        categoryRepository.deleteCategory(category)
}
