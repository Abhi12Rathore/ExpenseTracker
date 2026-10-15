package com.example.expensetrackerpro.domain.usecase.categorymemory

import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.repository.CategoryMemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Every category the user picked for a title, of both types. */
class ObserveCategoryMemoryUseCase @Inject constructor(
    private val repository: CategoryMemoryRepository
) {
    operator fun invoke(): Flow<List<CategoryMemory>> = repository.observeAll()
}
