package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.DailyExpenseEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DailyExpense
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMapperTest {

    @Test
    fun categoryExpense_mapsTotalAmountToAmount() {
        val entity = CategoryExpenseEntity(categoryId = 2, categoryName = "Food", icon = CategoryIcon.RESTAURANT, totalPaise = 1250_75)

        assertEquals(
            CategoryBreakdown(categoryId = 2, categoryName = "Food", icon = CategoryIcon.RESTAURANT, amount = Money(1250_75)),
            entity.toDomain()
        )
    }

    @Test
    fun dailyCashFlow_keepsIncomeAndExpenseSeparate() {
        val entity = DailyCashFlowEntity(dateMillis = 1_700_000_000_000, incomePaise = 500_00, expensePaise = 120_00)

        assertEquals(
            CashFlowPoint(dateMillis = 1_700_000_000_000, income = rupees(500), expense = rupees(120)),
            entity.toDomain()
        )
    }

    @Test
    fun dailyExpense_mapsDayMillisToDateMillis() {
        val entity = DailyExpenseEntity(dayMillis = 1_700_000_000_000, amountPaise = 340_50, transactionCount = 3)

        assertEquals(
            DailyExpense(dateMillis = 1_700_000_000_000, amount = Money(340_50), transactionCount = 3),
            entity.toDomain()
        )
    }

    @Test
    fun monthlyTrend_keepsYearAndMonthSeparate() {
        val entity = MonthlyTrendEntity(year = 2026, month = 7, incomePaise = 9000_00, expensePaise = 4000_00)

        assertEquals(
            MonthlyTrend(year = 2026, month = 7, income = rupees(9000), expense = rupees(4000)),
            entity.toDomain()
        )
    }
}
