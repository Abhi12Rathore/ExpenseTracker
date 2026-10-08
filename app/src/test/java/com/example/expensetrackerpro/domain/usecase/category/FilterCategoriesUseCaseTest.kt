package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterCategoriesUseCaseTest {

    private val filterCategories = FilterCategoriesUseCase()

    private val food = category(id = 1, name = "Food", type = TransactionType.EXPENSE)
    private val transport = category(id = 2, name = "Transport", type = TransactionType.EXPENSE)
    private val salary = category(id = 3, name = "Salary", type = TransactionType.INCOME)
    private val all = listOf(food, transport, salary)

    @Test
    fun emptyQuery_filtersByTypeOnly() {
        assertEquals(listOf(food, transport), filterCategories(all, TransactionType.EXPENSE, ""))
        assertEquals(listOf(salary), filterCategories(all, TransactionType.INCOME, ""))
    }

    @Test
    fun queryShorterThanThreeCharacters_isIgnored() {
        assertEquals(listOf(food, transport), filterCategories(all, TransactionType.EXPENSE, "tr"))
    }

    @Test
    fun threeCharacterQuery_matchesNameIgnoringCase() {
        assertEquals(listOf(transport), filterCategories(all, TransactionType.EXPENSE, "TRA"))
    }

    @Test
    fun query_matchesAnywhereInName() {
        assertEquals(listOf(transport), filterCategories(all, TransactionType.EXPENSE, "port"))
    }

    @Test
    fun query_neverCrossesTypes() {
        assertTrue(filterCategories(all, TransactionType.EXPENSE, "Salary").isEmpty())
    }

    @Test
    fun noMatch_isEmpty() {
        assertTrue(filterCategories(all, TransactionType.EXPENSE, "xyz").isEmpty())
    }
}
