package com.example.expensetrackerpro.presentation.homeScreen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCashFlowUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDayPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    getFinancialSummary: GetFinancialSummaryUseCase,
    getRecentTransactions: GetRecentTransactionsUseCase,
    getCategoryBreakdown: GetCategoryBreakdownUseCase,
    getCashFlow: GetCashFlowUseCase,
    getMonthlyTrend: GetMonthlyTrendUseCase,
    private val generateSmartInsights: GenerateSmartInsightsUseCase,
    getDayPeriod: GetDayPeriodUseCase
) : ViewModel() {

    // Read once when the dashboard opens
    private val now = System.currentTimeMillis()
    private val dayPeriod = getDayPeriod(DateFormatter.hourOfDay(now))

    val uiState: StateFlow<HomeUiState> =
        combine(
            getFinancialSummary(),
            getCategoryBreakdown(),
            getCashFlow(),
            getMonthlyTrend(),
            getRecentTransactions()
        ) { summary, categories, cashFlow, monthly, recent ->

            HomeUiState(
                dayPeriod = dayPeriod,
                currentMonthMillis = now,
                financialSummary = summary,
                categoryExpense = categories,
                dailyCashFlow = cashFlow,
                monthlyTrend = monthly,
                recentTransactions = recent,
                insights = generateSmartInsights(summary, categories, monthly)
            )

        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                HomeUiState(dayPeriod = dayPeriod, currentMonthMillis = now)
            )
}
