package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesByTypeUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    operator fun invoke(type: TransactionType): Flow<List<Category>> =
        categoryRepository.getCategoriesByType(type)
}
