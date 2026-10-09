package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository(
    initial: List<Category> = emptyList(),
    // Ids of categories that still have transactions, so can't be deleted
    private val idsInUse: Set<Int> = emptySet()
) : CategoryRepository {

    private val categories = MutableStateFlow(initial)

    val current: List<Category> get() = categories.value

    override fun getAllCategories(): Flow<List<Category>> = categories

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categories.map { list -> list.filter { it.type == type } }

    // Like Room, a category without an id is given the next free one
    override suspend fun insertCategory(category: Category): Int {
        val id = if (category.id != 0) category.id else (categories.value.maxOfOrNull { it.id } ?: 0) + 1
        categories.update { list -> list.filterNot { it.id == id } + category.copy(id = id) }
        return id
    }

    override suspend fun updateCategory(category: Category) {
        categories.update { list -> list.map { if (it.id == category.id) category else it } }
    }

    override suspend fun deleteCategory(category: Category): Boolean {
        if (category.id in idsInUse) return false
        categories.update { list -> list.filterNot { it.id == category.id } }
        return true
    }
}
