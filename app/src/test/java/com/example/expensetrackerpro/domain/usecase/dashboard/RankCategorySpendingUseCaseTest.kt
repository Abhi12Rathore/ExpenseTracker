package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankCategorySpendingUseCaseTest {

    private val rankCategorySpending = RankCategorySpendingUseCase()

    private fun category(id: Int, amount: Money) =
        CategoryBreakdown(categoryId = CategoryId(id), categoryName = "Category $id", icon = CategoryIcon.RESTAURANT, amount = amount)

    @Test
    fun noCategories_isAnEmptyRanking() {
        assertEquals(CategoryRanking(), rankCategorySpending(emptyList()))
    }

    @Test
    fun sharesAreEachCategorysPartOfTheTotal() {
        val ranking = rankCategorySpending(listOf(category(1, rupees(600)), category(2, rupees(300)), category(3, rupees(100))))

        assertEquals(rupees(1000), ranking.total)
        assertEquals(3, ranking.categoryCount)
        assertEquals(listOf(0.6f, 0.3f, 0.1f), ranking.top.map { it.fraction })
        assertTrue(ranking.others.isEmpty())
    }

    @Test
    fun ranksHighestFirst_whateverOrderTheyArriveIn() {
        val ranking = rankCategorySpending(listOf(category(1, rupees(50)), category(2, rupees(500)), category(3, rupees(200))))

        assertEquals(listOf(2, 3, 1), ranking.top.map { it.category.categoryId.value })
    }

    @Test
    fun beyondTheTopFive_theRestAreGroupedAsOthers() {
        val breakdown = (1..8).map { category(it, amount = rupees(900L - it * 100)) }

        val ranking = rankCategorySpending(breakdown)

        assertEquals(listOf(1, 2, 3, 4, 5), ranking.top.map { it.category.categoryId.value })
        assertEquals(listOf(6, 7, 8), ranking.others.map { it.category.categoryId.value })
        assertEquals(8, ranking.categoryCount)
        // 300 + 200 + 100 of 3600
        assertEquals(rupees(600), ranking.othersAmount)
        assertEquals(600f / 3600f, ranking.othersFraction, 0.0001f)
    }

    @Test
    fun exactlyOneLeftOver_isListedInsteadOfGrouped() {
        val ranking = rankCategorySpending((1..6).map { category(it, amount = rupees(700L - it * 100)) })

        assertEquals(6, ranking.top.size)
        assertTrue(ranking.others.isEmpty())
    }

    @Test
    fun topCount_setsHowManyLead() {
        val ranking = rankCategorySpending((1..5).map { category(it, rupees(100)) }, topCount = 2)

        assertEquals(2, ranking.top.size)
        assertEquals(3, ranking.others.size)
    }

    @Test
    fun zeroTotal_givesZeroShares() {
        val ranking = rankCategorySpending(listOf(category(1, Money.ZERO)))

        assertEquals(listOf(0f), ranking.top.map { it.fraction })
        assertEquals(0f, ranking.othersFraction, 0f)
    }
}
