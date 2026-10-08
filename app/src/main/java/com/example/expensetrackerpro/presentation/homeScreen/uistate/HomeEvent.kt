package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent

sealed interface HomeEvent {
    data class Filter(val event: PeriodFilterEvent) : HomeEvent
}
