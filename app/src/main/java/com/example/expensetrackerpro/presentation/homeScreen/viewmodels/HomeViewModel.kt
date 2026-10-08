package com.example.expensetrackerpro.presentation.homeScreen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.models.SmartInsight
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCashFlowUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    getFinancialSummary: GetFinancialSummaryUseCase,
    getRecentTransactions: GetRecentTransactionsUseCase,
    getCategoryBreakdown: GetCategoryBreakdownUseCase,
    getCashFlow: GetCashFlowUseCase,
    getMonthlyTrend: GetMonthlyTrendUseCase,
    private val generateSmartInsights: GenerateSmartInsightsUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(HomeUiState())

    val uiState =
        _uiState.asStateFlow()


    val financialSummary: StateFlow<FinancialSummary> =
        getFinancialSummary()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                FinancialSummary()
            )


    val recentTransactions =
        getRecentTransactions()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val categorySpending =
        getCategoryBreakdown()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val dailyCashFlow =
        getCashFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val monthlyTrend =
        getMonthlyTrend()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val insights =
        combine(
            financialSummary,
            categorySpending,
            monthlyTrend
        ) { summary, categories, monthly ->
            generateSmartInsights(summary, categories, monthly)
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    init {
        observeDashboard()
    }


    private fun observeDashboard() {

        viewModelScope.launch {

            combine(
                financialSummary,
                categorySpending,
                dailyCashFlow,
                monthlyTrend,
                recentTransactions,
                insights
            ) { values ->

                HomeUiState(

                    financialSummary = values[0] as FinancialSummary,

                    categoryExpense = values[1] as List<CategoryBreakdown>,

                    dailyCashFlow = values[2] as List<CashFlowPoint>,

                    monthlyTrend = values[3] as List<MonthlyTrend>,

                    recentTransactions = values[4] as List<Transaction>,

                    insights = values[5] as List<SmartInsight>

                )

            }
                .collect { state ->

                    _uiState.value = state

                }

        }

    }
}
