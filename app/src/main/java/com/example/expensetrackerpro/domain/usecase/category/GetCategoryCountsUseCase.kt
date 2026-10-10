package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategoryCounts
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCategoryCountsUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {

    operator fun invoke(): Flow<CategoryCounts> =
        categoryRepository.getAllCategories()
            .map { categories -> CategoryCounts(total = categories.size, custom = categories.count { it.isCustom }) }
            .distinctUntilChanged()
}
