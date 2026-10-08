package com.example.expensetrackerpro.presentation.common.filter

import com.example.expensetrackerpro.domain.models.PeriodGranularity

sealed interface PeriodFilterEvent {
    data class GranularitySelected(val granularity: PeriodGranularity) : PeriodFilterEvent
    data object PreviousPeriod : PeriodFilterEvent
    data object NextPeriod : PeriodFilterEvent
    // Both are the picker's values: the chosen dates as midnight UTC
    data class CustomRangeSelected(val startUtcMillis: Long, val endUtcMillis: Long) : PeriodFilterEvent
}
