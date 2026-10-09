package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.utils.InsightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateSmartInsightsUseCaseTest {

    private val generateSmartInsights = GenerateSmartInsightsUseCase()

    private val saving = FinancialSummary(totalBalance = 1200.0, totalIncome = 2000.0, totalExpense = 800.0)
    private val food = CategoryBreakdown(categoryId = 1, categoryName = "Food", icon = CategoryIcon.RESTAURANT, amount = 500.0)
    private val bills = CategoryBreakdown(categoryId = 2, categoryName = "Bills", icon = CategoryIcon.RECEIPT_LONG, amount = 300.0)

    private fun month(month: Int, expense: Double) =
        MonthlyTrend(year = 2026, month = month, income = 0.0, expense = expense)

    @Test
    fun nothingToReport_isEmpty() {
        assertTrue(generateSmartInsights(FinancialSummary(), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun positiveBalance_addsSavedWithBalance() {
        assertEquals(
            listOf(SmartInsight.Saved(1200.0)),
            generateSmartInsights(saving, emptyList(), emptyList())
        )
    }

    @Test
    fun zeroOrNegativeBalance_hasNoSavedInsight() {
        val breakEven = FinancialSummary(totalBalance = 0.0)
        val overspent = FinancialSummary(totalBalance = -50.0)

        assertTrue(generateSmartInsights(breakEven, emptyList(), emptyList()).isEmpty())
        assertTrue(generateSmartInsights(overspent, emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun firstCategory_isReportedAsHighestSpending() {
        assertEquals(
            listOf(SmartInsight.HighestSpending("Food")),
            generateSmartInsights(FinancialSummary(), listOf(food, bills), emptyList())
        )
    }

    @Test
    fun singleMonth_hasNoComparison() {
        assertTrue(
            generateSmartInsights(FinancialSummary(), emptyList(), listOf(month(7, 100.0))).isEmpty()
        )
    }

    @Test
    fun lowerExpenseThanPreviousMonth_isImproved() {
        assertEquals(
            listOf(SmartInsight.SpendingImproved),
            generateSmartInsights(
                FinancialSummary(),
                emptyList(),
                listOf(month(6, 500.0), month(7, 400.0))
            )
        )
    }

    @Test
    fun higherOrEqualExpense_isIncreased() {
        val higher = listOf(month(6, 500.0), month(7, 650.0))
        val equal = listOf(month(6, 500.0), month(7, 500.0))

        assertEquals(
            listOf(SmartInsight.SpendingIncreased),
            generateSmartInsights(FinancialSummary(), emptyList(), higher)
        )
        assertEquals(
            listOf(SmartInsight.SpendingIncreased),
            generateSmartInsights(FinancialSummary(), emptyList(), equal)
        )
    }

    @Test
    fun comparesOnlyTheLastTwoMonths() {
        val trend = listOf(month(5, 100.0), month(6, 900.0), month(7, 400.0))

        assertEquals(
            listOf(SmartInsight.SpendingImproved),
            generateSmartInsights(FinancialSummary(), emptyList(), trend)
        )
    }

    @Test
    fun allRulesMatch_keepsSavingCategoryTrendOrder() {
        val insights = generateSmartInsights(
            saving,
            listOf(food, bills),
            listOf(month(6, 900.0), month(7, 800.0))
        )

        assertEquals(
            listOf(
                SmartInsight.Saved(1200.0),
                SmartInsight.HighestSpending("Food"),
                SmartInsight.SpendingImproved
            ),
            insights
        )
        assertEquals(
            listOf(InsightType.POSITIVE, InsightType.WARNING, InsightType.POSITIVE),
            insights.map { it.type }
        )
    }
}
