package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.rupees
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
        repository.saveTransaction(transaction(id = 1, amount = rupees(2000), type = TransactionType.INCOME))
        repository.saveTransaction(transaction(id = 2, amount = rupees(500), type = TransactionType.EXPENSE))

        assertEquals(
            FinancialSummary(
                totalBalance = rupees(1500),
                totalIncome = rupees(2000),
                totalExpense = rupees(500),
                savingsRate = 75f
            ),
            getFinancialSummary().first()
        )
    }

    @Test
    fun summarisesOnlyTransactionsInRange() = runTest {
        repository.saveTransaction(
            transaction(id = 1, amount = rupees(2000), type = TransactionType.INCOME, dateMillis = 50L)
        )
        repository.saveTransaction(
            transaction(id = 2, amount = rupees(500), type = TransactionType.EXPENSE, dateMillis = 150L)
        )
        repository.saveTransaction(
            transaction(id = 3, amount = rupees(300), type = TransactionType.INCOME, dateMillis = 200L)
        )

        val summary = getFinancialSummary(DateRange(startMillis = 100L, endMillis = 200L)).first()

        assertEquals(Money.ZERO, summary.totalIncome)
        assertEquals(rupees(500), summary.totalExpense)
        assertEquals(rupees(-500), summary.totalBalance)
    }

    @Test
    fun reflectsLaterChanges() = runTest {
        repository.saveTransaction(transaction(id = 1, amount = rupees(100), type = TransactionType.INCOME))
        assertEquals(rupees(100), getFinancialSummary().first().totalBalance)

        repository.saveTransaction(transaction(id = 2, amount = rupees(40), type = TransactionType.EXPENSE))
        assertEquals(rupees(60), getFinancialSummary().first().totalBalance)
    }
}
