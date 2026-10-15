package com.example.expensetrackerpro.domain.repository

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.CategoryMemory
import kotlinx.coroutines.flow.Flow

/**
 * The categories the user picked for titles, so quick add picks them next time. A cache, not the
 * user's data: it is not in backups, and it goes with its category.
 */
interface CategoryMemoryRepository {

    fun observeAll(): Flow<List<CategoryMemory>>

    /** Remembers [memory], replacing what was remembered for its title and type. */
    suspend fun remember(memory: CategoryMemory): Outcome<Unit>
}
