package com.example.expensetrackerpro.domain.usecase.categorymemory

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryMemoryRepository
import javax.inject.Inject

/** Remembers that the user files [title] under [categoryId], so quick add picks it next time. */
class RememberCategoryUseCase @Inject constructor(
    private val repository: CategoryMemoryRepository
) {
    suspend operator fun invoke(title: String, type: TransactionType, categoryId: CategoryId): Outcome<Unit> {
        val key = CategoryMemory.keyOf(title)
        // Nothing to find it by
        if (key.isEmpty()) return Outcome.Success(Unit)

        return repository.remember(CategoryMemory(key, type, categoryId))
    }
}
