package com.example.expensetrackerpro.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity

/** A category History is narrowed to, with the name it is shown under. */
data class CategoryFilter(val id: Long, val name: String)

/** What History opens filtered to when it is reached from a dashboard chart. */
data class HistoryFilter(
    val dateFilter: DateFilter,
    val category: CategoryFilter? = null
) {
    companion object {

        /** The filter the History route was opened with, or null when it was opened plainly. */
        fun from(savedStateHandle: SavedStateHandle): HistoryFilter? {
            val start = savedStateHandle.get<Long>(Screens.History.ARG_START) ?: Screens.History.NO_VALUE
            val end = savedStateHandle.get<Long>(Screens.History.ARG_END) ?: Screens.History.NO_VALUE
            if (start == Screens.History.NO_VALUE) return null

            val granularity = savedStateHandle.get<String>(Screens.History.ARG_GRANULARITY)
                ?.let { name -> PeriodGranularity.entries.find { it.name == name } }
            val categoryId = savedStateHandle.get<Long>(Screens.History.ARG_CATEGORY_ID)
                ?: Screens.History.NO_VALUE
            val categoryName = savedStateHandle.get<String>(Screens.History.ARG_CATEGORY_NAME)

            return HistoryFilter(
                dateFilter = when {
                    granularity != null -> DateFilter.Period(granularity, start)
                    else -> DateFilter.Custom(DateRange(start, end))
                },
                category = categoryName
                    ?.takeIf { categoryId != Screens.History.NO_VALUE }
                    ?.let { CategoryFilter(categoryId, it) }
            )
        }
    }
}
