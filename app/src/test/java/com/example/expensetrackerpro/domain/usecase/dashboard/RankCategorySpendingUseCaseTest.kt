package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryRanking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankCategorySpendingUseCaseTest {

    private val rankCategorySpending = RankCategorySpendingUseCase()

    private fun category(id: Long, amount: Double) =
        CategoryBreakdown(categoryId = id, categoryName = "Category $id", iconName = "Food", amount = amount)

    @Test
    fun noCategories_isAnEmptyRanking() {
        assertEquals(CategoryRanking(), rankCategorySpending(emptyList()))
    }

    @Test
    fun sharesAreEachCategorysPartOfTheTotal() {
        val ranking = rankCategorySpending(listOf(category(1, 600.0), category(2, 300.0), category(3, 100.0)))

        assertEquals(1000.0, ranking.total, 0.001)
        assertEquals(3, ranking.categoryCount)
        assertEquals(listOf(0.6f, 0.3f, 0.1f), ranking.top.map { it.fraction })
        assertTrue(ranking.others.isEmpty())
    }

    @Test
    fun ranksHighestFirst_whateverOrderTheyArriveIn() {
        val ranking = rankCategorySpending(listOf(category(1, 50.0), category(2, 500.0), category(3, 200.0)))

        assertEquals(listOf(2L, 3L, 1L), ranking.top.map { it.category.categoryId })
    }

    @Test
    fun beyondTheTopFive_theRestAreGroupedAsOthers() {
        val breakdown = (1..8).map { category(it.toLong(), amount = 900.0 - it * 100) }

        val ranking = rankCategorySpending(breakdown)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), ranking.top.map { it.category.categoryId })
        assertEquals(listOf(6L, 7L, 8L), ranking.others.map { it.category.categoryId })
        assertEquals(8, ranking.categoryCount)
        // 300 + 200 + 100 of 3600
        assertEquals(600.0, ranking.othersAmount, 0.001)
        assertEquals(600f / 3600f, ranking.othersFraction, 0.0001f)
    }

    @Test
    fun exactlyOneLeftOver_isListedInsteadOfGrouped() {
        val ranking = rankCategorySpending((1..6).map { category(it.toLong(), amount = 700.0 - it * 100) })

        assertEquals(6, ranking.top.size)
        assertTrue(ranking.others.isEmpty())
    }

    @Test
    fun topCount_setsHowManyLead() {
        val ranking = rankCategorySpending((1..5).map { category(it.toLong(), 100.0) }, topCount = 2)

        assertEquals(2, ranking.top.size)
        assertEquals(3, ranking.others.size)
    }

    @Test
    fun zeroTotal_givesZeroShares() {
        val ranking = rankCategorySpending(listOf(category(1, 0.0)))

        assertEquals(listOf(0f), ranking.top.map { it.fraction })
        assertEquals(0f, ranking.othersFraction, 0f)
    }
}
