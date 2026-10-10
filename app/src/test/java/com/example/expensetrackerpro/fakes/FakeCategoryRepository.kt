package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository(
    initial: List<Category> = emptyList(),
    // Ids of categories that still have a transaction, so can't simply be deleted
    idsInUse: Set<Int> = emptySet()
) : CategoryRepository {

    private val categories = MutableStateFlow(initial)

    // By category id; a category without an entry has never been used
    private val usage = MutableStateFlow(idsInUse.associateWith { Usage(totalCount = 1) })

    val current: List<Category> get() = categories.value

    /** The range usage was last asked for. */
    var usageRange: DateRange? = null
        private set

    /** While true the reassigning delete fails, as a failing database would make it. */
    var failReassign = false

    /** Says how much the category [id] has been used, ever and in the period asked for. */
    fun setUsage(id: Int, totalCount: Int, periodCount: Int = 0, periodAmount: Double = 0.0) {
        usage.update { it + (id to Usage(totalCount, periodCount, periodAmount)) }
    }

    fun totalCountOf(id: Int): Int = usage.value[id]?.totalCount ?: 0

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
        if (totalCountOf(category.id) > 0) return false
        categories.update { list -> list.filterNot { it.id == category.id } }
        return true
    }

    override fun getCategoryUsage(range: DateRange): Flow<List<CategoryUsage>> {
        usageRange = range

        return combine(categories, usage) { list, used ->
            list.map { category ->
                val entry = used[category.id] ?: Usage()
                CategoryUsage(category, entry.totalCount, entry.periodCount, entry.periodAmount)
            }
        }
    }

    override suspend fun deleteCategoryReassigning(category: Category, target: Category): Boolean {
        if (failReassign || categories.value.none { it.id == target.id }) return false

        usage.update { used ->
            val moved = used[category.id] ?: Usage()
            val kept = used[target.id] ?: Usage()
            used - category.id + (
                target.id to Usage(
                    totalCount = kept.totalCount + moved.totalCount,
                    periodCount = kept.periodCount + moved.periodCount,
                    periodAmount = kept.periodAmount + moved.periodAmount
                )
            )
        }
        categories.update { list -> list.filterNot { it.id == category.id } }
        return true
    }

    private data class Usage(
        val totalCount: Int = 0,
        val periodCount: Int = 0,
        val periodAmount: Double = 0.0
    )
}
