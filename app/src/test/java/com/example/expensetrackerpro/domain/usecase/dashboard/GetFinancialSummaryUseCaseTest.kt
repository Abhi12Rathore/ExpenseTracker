package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetFinancialSummaryUseCaseTest {

    private val repository = FakeTransactionRepository()
    private val getFinancialSummary = GetFinancialSummaryUseCase(
        repository,
        CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase())
    )

    @Test
    fun noTransactions_emitsEmptySummary() = runTest {
        assertEquals(FinancialSummary(), getFinancialSummary().first())
    }

    @Test
    fun summarisesAllStoredTransactions() = runTest {
        repository.saveTransaction(transaction(id = 1, amount = 2000.0, type = TransactionType.INCOME))
        repository.saveTransaction(transaction(id = 2, amount = 500.0, type = TransactionType.EXPENSE))

        assertEquals(
            FinancialSummary(
                totalBalance = 1500.0,
                totalIncome = 2000.0,
                totalExpense = 500.0,
                savingsRate = 75f
            ),
            getFinancialSummary().first()
        )
    }

    @Test
    fun reflectsLaterChanges() = runTest {
        repository.saveTransaction(transaction(id = 1, amount = 100.0, type = TransactionType.INCOME))
        assertEquals(100.0, getFinancialSummary().first().totalBalance, 0.001)

        repository.saveTransaction(transaction(id = 2, amount = 40.0, type = TransactionType.EXPENSE))
        assertEquals(60.0, getFinancialSummary().first().totalBalance, 0.001)
    }
}
