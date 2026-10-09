package com.example.expensetrackerpro.presentation.homeScreen.uistate

import com.example.expensetrackerpro.domain.models.Dashboard
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState

data class HomeUiState(

    // The period every section of the dashboard is filtered to
    val filter: PeriodFilterState = PeriodFilterState(),

    // True until the first data for the dashboard has arrived
    val isLoading: Boolean = false,

    val dashboard: Dashboard = Dashboard()
)
