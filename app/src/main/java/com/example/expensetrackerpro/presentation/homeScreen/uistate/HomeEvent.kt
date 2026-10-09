package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent

sealed interface HomeEvent {
    data class Filter(val event: PeriodFilterEvent) : HomeEvent
    // A month picked from the year's chart: the dashboard narrows to that month
    data class MonthSelected(val monthStartMillis: Long) : HomeEvent
}
