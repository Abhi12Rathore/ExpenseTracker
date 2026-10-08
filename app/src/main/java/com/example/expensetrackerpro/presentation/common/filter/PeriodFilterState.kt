package com.example.expensetrackerpro.presentation.common.filter

import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity

/** Everything the period filter bar shows: the selection, the days it covers and whether › is enabled. */
data class PeriodFilterState(
    val filter: DateFilter = DateFilter.Period(PeriodGranularity.MONTH, 0L),
    val dateRange: DateRange = DateRange.ALL,
    val canStepForward: Boolean = false
)
