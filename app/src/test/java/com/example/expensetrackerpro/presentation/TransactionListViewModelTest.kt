package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.CalculateSavingsRateUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.transaction
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel.TransactionListViewModel
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import com.example.expensetrackerpro.utils.ExpenseFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class TransactionListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = System.currentTimeMillis()
    private val salary = transaction(id = 1, amount = 1000.0, type = TransactionType.INCOME, dateMillis = now)
    private val lunch = transaction(id = 2, amount = 250.0, type = TransactionType.EXPENSE, dateMillis = now)
    private val oldRent = transaction(id = 3, amount = 400.0, type = TransactionType.EXPENSE, dateMillis = 1L)

    private val repository = FakeTransactionRepository(listOf(salary, lunch, oldRent))

    private fun viewModel() = TransactionListViewModel(
        getTransactions = GetTransactionsUseCase(repository),
        calculateFinancialSummary = CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase()),
        deleteTransaction = DeleteTransactionUseCase(repository)
    )

    @Test
    fun emptyRepository_isDefaultState() = runTest {
        val viewModel = TransactionListViewModel(
            GetTransactionsUseCase(FakeTransactionRepository()),
            CalculateFinancialSummaryUseCase(CalculateSavingsRateUseCase()),
            DeleteTransactionUseCase(FakeTransactionRepository())
        )
        subscribe(viewModel.uiState)

        assertEquals(TransactionListUiState(), viewModel.uiState.value)
    }

    @Test
    fun allFilter_showsEverythingNewestFirstWithTotals() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        val state = viewModel.uiState.value

        assertEquals(ExpenseFilter.ALL, state.selectedFilter)
        assertEquals(listOf(salary, lunch, oldRent), state.transactions)
        assertEquals(1000.0, state.totalIncome, 0.001)
        assertEquals(650.0, state.totalExpense, 0.001)
        assertEquals(350.0, state.totalBalance, 0.001)
    }

    @Test
    fun monthFilter_dropsOlderTransactionsAndRecalculatesTotals() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.FilterSelected(ExpenseFilter.THIS_MONTH))
        val state = viewModel.uiState.value

        assertEquals(ExpenseFilter.THIS_MONTH, state.selectedFilter)
        assertEquals(listOf(salary, lunch), state.transactions)
        assertEquals(250.0, state.totalExpense, 0.001)
        assertEquals(750.0, state.totalBalance, 0.001)
    }

    @Test
    fun deleteRequested_thenConfirmed_removesAndClosesDialog() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteRequested(lunch))
        assertEquals(lunch, viewModel.uiState.value.transactionToDelete)

        viewModel.onEvent(TransactionListEvent.DeleteConfirmed)
        val state = viewModel.uiState.value

        assertNull(state.transactionToDelete)
        assertEquals(listOf(salary, oldRent), state.transactions)
        assertEquals(400.0, state.totalExpense, 0.001)
    }

    @Test
    fun deleteDismissed_keepsTransaction() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteRequested(lunch))
        viewModel.onEvent(TransactionListEvent.DeleteDismissed)
        val state = viewModel.uiState.value

        assertNull(state.transactionToDelete)
        assertEquals(3, state.transactions.size)
    }

    @Test
    fun deleteConfirmed_withoutRequest_doesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(TransactionListEvent.DeleteConfirmed)

        assertEquals(3, viewModel.uiState.value.transactions.size)
    }
}
