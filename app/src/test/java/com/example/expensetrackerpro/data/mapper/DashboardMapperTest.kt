package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMapperTest {

    @Test
    fun categoryExpense_mapsTotalAmountToAmount() {
        val entity = CategoryExpenseEntity(categoryId = 2, categoryName = "Food", totalAmount = 1250.75)

        assertEquals(
            CategoryBreakdown(categoryId = 2, categoryName = "Food", amount = 1250.75),
            entity.toDomain()
        )
    }

    @Test
    fun dailyCashFlow_keepsIncomeAndExpenseSeparate() {
        val entity = DailyCashFlowEntity(dateMillis = 1_700_000_000_000, income = 500.0, expense = 120.0)

        assertEquals(
            CashFlowPoint(dateMillis = 1_700_000_000_000, income = 500.0, expense = 120.0),
            entity.toDomain()
        )
    }

    @Test
    fun monthlyTrend_keepsYearAndMonthSeparate() {
        val entity = MonthlyTrendEntity(year = 2026, month = 7, income = 9000.0, expense = 4000.0)

        assertEquals(
            MonthlyTrend(year = 2026, month = 7, income = 9000.0, expense = 4000.0),
            entity.toDomain()
        )
    }
}
