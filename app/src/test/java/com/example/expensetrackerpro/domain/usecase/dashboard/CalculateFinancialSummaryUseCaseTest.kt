package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.rupees
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
                transaction(id = 1, amount = rupees(600), type = TransactionType.INCOME),
                transaction(id = 2, amount = rupees(400), type = TransactionType.INCOME),
                transaction(id = 3, amount = rupees(150), type = TransactionType.EXPENSE),
                transaction(id = 4, amount = rupees(100), type = TransactionType.EXPENSE)
            )
        )

        assertEquals(rupees(1000), summary.totalIncome)
        assertEquals(rupees(250), summary.totalExpense)
        assertEquals(rupees(750), summary.totalBalance)
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
                transaction(id = 1, amount = rupees(100), type = TransactionType.INCOME),
                transaction(id = 2, amount = rupees(300), type = TransactionType.EXPENSE)
            )
        )

        assertEquals(rupees(-200), summary.totalBalance)
        assertEquals(0f, summary.savingsRate, 0.001f)
    }

    @Test
    fun expensesOnly_haveNoIncome() {
        val summary = calculateFinancialSummary(
            listOf(transaction(amount = rupees(80), type = TransactionType.EXPENSE))
        )

        assertEquals(Money.ZERO, summary.totalIncome)
        assertEquals(rupees(80), summary.totalExpense)
        assertEquals(rupees(-80), summary.totalBalance)
    }
}
