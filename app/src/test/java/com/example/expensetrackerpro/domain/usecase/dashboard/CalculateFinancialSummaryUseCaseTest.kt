package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateFinancialSummaryUseCaseTest {

    private val calculateFinancialSummary =
        CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())

    @Test
    fun sumsIncomeAndExpenseSeparately() {
        val summary = calculateFinancialSummary(
            listOf(
                transaction(id = 1, amount = 600.0, type = TransactionType.INCOME),
                transaction(id = 2, amount = 400.0, type = TransactionType.INCOME),
                transaction(id = 3, amount = 150.0, type = TransactionType.EXPENSE),
                transaction(id = 4, amount = 100.0, type = TransactionType.EXPENSE)
            )
        )

        assertEquals(1000.0, summary.totalIncome, 0.001)
        assertEquals(250.0, summary.totalExpense, 0.001)
        assertEquals(750.0, summary.totalBalance, 0.001)
        assertEquals(75f, summary.savingsRate, 0.001f)
    }

    @Test
    fun emptyList_isAllZero() {
        assertEquals(FinancialSummary(), calculateFinancialSummary(emptyList()))
    }

    @Test
    fun overspending_givesNegativeBalanceAndZeroSavingsRate() {
        val summary = calculateFinancialSummary(
            listOf(
                transaction(id = 1, amount = 100.0, type = TransactionType.INCOME),
                transaction(id = 2, amount = 300.0, type = TransactionType.EXPENSE)
            )
        )

        assertEquals(-200.0, summary.totalBalance, 0.001)
        assertEquals(0f, summary.savingsRate, 0.001f)
    }

    @Test
    fun expensesOnly_haveNoIncome() {
        val summary = calculateFinancialSummary(
            listOf(transaction(amount = 80.0, type = TransactionType.EXPENSE))
        )

        assertEquals(0.0, summary.totalIncome, 0.001)
        assertEquals(80.0, summary.totalExpense, 0.001)
        assertEquals(-80.0, summary.totalBalance, 0.001)
    }
}
