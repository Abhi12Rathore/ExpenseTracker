package com.example.expensetrackerpro.presentation.homeScreen.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDashboardUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.StepPeriodUseCase
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeEvent
import com.example.expensetrackerpro.presentation.homeScreen.uistate.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    getDashboard: GetDashboardUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val stepPeriod: StepPeriodUseCase,
    private val getCustomDateRange: GetCustomDateRangeUseCase
) : ViewModel() {

    private val filter = MutableStateFlow<DateFilter>(getCurrentPeriod(PeriodGranularity.MONTH))

    val uiState: StateFlow<HomeUiState> =
        filter.flatMapLatest { selected ->
            val filterState = selected.toState()

            getDashboard(selected).map { dashboard ->
                HomeUiState(filter = filterState, dashboard = dashboard)
            }
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                HomeUiState(filter = filter.value.toState(), isLoading = true)
            )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Filter -> onFilterEvent(event.event)
            is HomeEvent.MonthSelected ->
                filter.value = DateFilter.Period(PeriodGranularity.MONTH, event.monthStartMillis)
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
