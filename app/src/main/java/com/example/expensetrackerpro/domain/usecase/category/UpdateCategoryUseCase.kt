package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import javax.inject.Inject

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    suspend operator fun invoke(category: Category): Outcome<Unit> =
        categoryRepository.updateCategory(category)
}
