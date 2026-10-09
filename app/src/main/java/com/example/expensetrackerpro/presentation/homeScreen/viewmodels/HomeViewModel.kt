package com.example.expensetrackerpro.presentation.homeScreen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodComparison
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.models.SpendingChart
import com.example.expensetrackerpro.domain.usecase.dashboard.GenerateSmartInsightsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetBalanceTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetCategoryBreakdownUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDayPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetFinancialSummaryUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetMonthlyTrendUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetPeriodComparisonUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetRecentTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetSpendingBucketsUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.RankCategorySpendingUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeEvent
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import com.example.expensetrackerpro.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    getFinancialSummary: GetFinancialSummaryUseCase,
    getRecentTransactions: GetRecentTransactionsUseCase,
    private val getCategoryBreakdown: GetCategoryBreakdownUseCase,
    private val getSpendingBuckets: GetSpendingBucketsUseCase,
    private val getBalanceTrend: GetBalanceTrendUseCase,
    private val getPeriodComparison: GetPeriodComparisonUseCase,
    private val rankCategorySpending: RankCategorySpendingUseCase,
    getMonthlyTrend: GetMonthlyTrendUseCase,
    private val generateSmartInsights: GenerateSmartInsightsUseCase,
    getDayPeriod: GetDayPeriodUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val stepPeriod: StepPeriodUseCase,
    private val getCustomDateRange: GetCustomDateRangeUseCase
) : ViewModel() {

    // Read once when the dashboard opens
    private val dayPeriod = getDayPeriod(DateFormatter.hourOfDay(System.currentTimeMillis()))

    private val filter = MutableStateFlow<DateFilter>(getCurrentPeriod(PeriodGranularity.MONTH))

    val uiState: StateFlow<HomeUiState> =
        filter.flatMapLatest { selected ->
            val filterState = selected.toState()
            val range = filterState.dateRange

            combine(
                getFinancialSummary(range),
                getRecentTransactions(range),
                getMonthlyTrend(range),
                getCharts(selected, range)
            ) { summary, recent, monthly, charts ->

                HomeUiState(
                    dayPeriod = dayPeriod,
                    filter = filterState,
                    financialSummary = summary,
                    periodComparison = charts.comparison,
                    balanceTrend = charts.balanceTrend,
                    spending = charts.spending,
                    categoryRanking = rankCategorySpending(charts.categories),
                    monthlyTrend = monthly,
                    recentTransactions = recent,
                    insights = generateSmartInsights(summary, charts.categories, monthly)
                )

            }
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                HomeUiState(dayPeriod = dayPeriod, filter = filter.value.toState(), isLoading = true)
            )

    // Grouped so the dashboard stays within what a single combine can take
    private data class Charts(
        val categories: List<CategoryBreakdown>,
        val spending: SpendingChart,
        val balanceTrend: List<Double>,
        val comparison: PeriodComparison?
    )

    private fun getCharts(selected: DateFilter, range: DateRange): Flow<Charts> =
        combine(
            getCategoryBreakdown(range),
            getSpendingBuckets(range),
            getBalanceTrend(range),
            getPeriodComparison(selected),
            ::Charts
        )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Filter -> onFilterEvent(event.event)
        }
    }

    private fun onFilterEvent(event: PeriodFilterEvent) {
        val current = filter.value

        filter.value = when (event) {
            // Tapping a segment always jumps to the period we are in now
            is PeriodFilterEvent.GranularitySelected -> getCurrentPeriod(event.granularity)
            PeriodFilterEvent.PreviousPeriod -> current.stepped(forward = false)
            PeriodFilterEvent.NextPeriod -> current.stepped(forward = true)
            is PeriodFilterEvent.CustomRangeSelected -> DateFilter.Custom(
                getCustomDateRange(event.startUtcMillis, event.endUtcMillis)
            )
        }
    }

    /** The neighbouring period, or the filter unchanged when there is nowhere to step to. */
    private fun DateFilter.stepped(forward: Boolean): DateFilter =
        (this as? DateFilter.Period)?.let { stepPeriod(it, forward) } ?: this

    private fun DateFilter.toState() = PeriodFilterState(
        filter = this,
        dateRange = getDateRange(this),
        canStepForward = this is DateFilter.Period && stepPeriod(this, forward = true) != null
    )
}
