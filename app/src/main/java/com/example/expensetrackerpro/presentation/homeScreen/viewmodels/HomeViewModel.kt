package com.example.expensetrackerpro.presentation.homeScreen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CashFlowPoint
import com.example.expensetrackerpro.domain.models.FinancialInsight
import com.example.expensetrackerpro.domain.models.MonthlyTrend
import com.example.expensetrackerpro.domain.repository.DashboardRepository
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.utils.InsightType
import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {


    private val _uiState =
        MutableStateFlow(HomeUiState())


    val uiState =
        _uiState.asStateFlow()



    // saare StateFlows yahan initialize hone do


    private val transactions =
        repository.getAllTransactions()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val financialSummary: StateFlow<FinancialSummaryData> =
        transactions
            .map { list ->

                val income =
                    list.filter {
                        it.transactionType == TransactionType.INCOME
                    }
                        .sumOf {
                            it.amount
                        }


                val expense =
                    list.filter {
                        it.transactionType == TransactionType.EXPENSE
                    }
                        .sumOf {
                            it.amount
                        }


                FinancialSummaryData(
                    income = income,
                    expense = expense,
                    savings = income - expense
                )

            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                FinancialSummaryData()
            )


    val recentTransactions =
        transactions
            .map {
                it.take(5)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val categorySpending =
        dashboardRepository.getCategoryBreakdown()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val dailyCashFlow =
        dashboardRepository.getCashFlow()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )


    val monthlyTrend =
        dashboardRepository.getMonthlyTrend()
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
        )
        { summary, categories, monthly ->


            buildList {


                // Saving insight

                if(summary.savings > 0){

                    add(

                        FinancialInsight(

                            title = "Great Saving! 🎉",

                            description =
                                "You saved ₹${summary.savings.toInt()} this month",

                            type = InsightType.POSITIVE

                        )

                    )

                }



                // Highest expense category

                categories.firstOrNull()?.let {

                    add(

                        FinancialInsight(

                            title = "Highest Spending",

                            description =
                                "${it.categoryName} is your biggest expense category",

                            type = InsightType.WARNING

                        )

                    )

                }



                // Monthly comparison

                if(monthly.size >= 2){


                    val current =
                        monthly.last()


                    val previous =
                        monthly[monthly.size - 2]



                    if(current.expense < previous.expense){


                        add(

                            FinancialInsight(

                                title = "Spending Improved 📉",

                                description =
                                    "Your expenses are lower than last month",

                                type = InsightType.POSITIVE

                            )

                        )

                    }
                    else{


                        add(

                            FinancialInsight(

                                title = "Watch Spending ⚠️",

                                description =
                                    "Your expenses increased compared to last month",

                                type = InsightType.WARNING

                            )

                        )

                    }

                }


            }


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

                    financialSummary = values[0] as FinancialSummaryData,

                    categoryExpense = values[1] as List<CategoryBreakdown>,

                    dailyCashFlow = values[2] as List<CashFlowPoint>,

                    monthlyTrend = values[3] as List<MonthlyTrend>,

                    recentTransactions = values[4] as List<Transaction>,

                    insights = values[5] as List<FinancialInsight>

                )

            }
                .collect { state ->

                    _uiState.value = state

                }

        }

    }




}


data class FinancialSummaryData(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val savings: Double = 0.0,
    val savingsRate: Double = 0.0
)