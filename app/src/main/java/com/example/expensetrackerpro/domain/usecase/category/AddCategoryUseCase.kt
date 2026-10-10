package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class AddCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    /** Returns the id the new category was stored under. */
    suspend operator fun invoke(category: Category): Outcome<Int> =
        categoryRepository.insertCategory(category)
}
