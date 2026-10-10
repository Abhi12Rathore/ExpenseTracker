package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.utils.InsightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GenerateSmartInsightsUseCaseTest {

    private val generateSmartInsights = GenerateSmartInsightsUseCase()

    private val saving = FinancialSummary(totalBalance = rupees(1200), totalIncome = rupees(2000), totalExpense = rupees(800))
    private val food = CategoryBreakdown(categoryId = 1, categoryName = "Food", icon = CategoryIcon.RESTAURANT, amount = rupees(500))
    private val bills = CategoryBreakdown(categoryId = 2, categoryName = "Bills", icon = CategoryIcon.RECEIPT_LONG, amount = rupees(300))

    private fun month(month: Int, expense: Money) =
        MonthlyTrend(year = 2026, month = month, income = Money.ZERO, expense = expense)

    @Test
    fun nothingToReport_isEmpty() {
        assertTrue(generateSmartInsights(FinancialSummary(), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun positiveBalance_addsSavedWithBalance() {
        assertEquals(
            listOf(SmartInsight.Saved(rupees(1200))),
            generateSmartInsights(saving, emptyList(), emptyList())
        )
    }

    @Test
    fun zeroOrNegativeBalance_hasNoSavedInsight() {
        val breakEven = FinancialSummary(totalBalance = Money.ZERO)
        val overspent = FinancialSummary(totalBalance = rupees(-50))

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
            generateSmartInsights(FinancialSummary(), emptyList(), listOf(month(7, rupees(100)))).isEmpty()
        )
    }

    @Test
    fun lowerExpenseThanPreviousMonth_isImproved() {
        assertEquals(
            listOf(SmartInsight.SpendingImproved),
            generateSmartInsights(
                FinancialSummary(),
                emptyList(),
                listOf(month(6, rupees(500)), month(7, rupees(400)))
            )
        )
    }

    @Test
    fun higherOrEqualExpense_isIncreased() {
        val higher = listOf(month(6, rupees(500)), month(7, rupees(650)))
        val equal = listOf(month(6, rupees(500)), month(7, rupees(500)))

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
        val trend = listOf(month(5, rupees(100)), month(6, rupees(900)), month(7, rupees(400)))

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
            listOf(month(6, rupees(900)), month(7, rupees(800)))
        )

        assertEquals(
            listOf(
                SmartInsight.Saved(rupees(1200)),
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
