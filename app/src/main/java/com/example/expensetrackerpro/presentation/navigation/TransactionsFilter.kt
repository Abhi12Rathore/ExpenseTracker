package com.example.expensetrackerpro.presentation.navigation

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity

/** A category Transactions is narrowed to, with the name it is shown under. */
data class CategoryFilter(val id: Long, val name: String)

/** What Transactions opens filtered to when it is reached from a dashboard chart. */
data class TransactionsFilter(
    val dateFilter: DateFilter,
    val category: CategoryFilter? = null
) {
    companion object {

        /** The filter the Transactions route was opened with, or null when it was opened plainly. */
        fun from(savedStateHandle: SavedStateHandle): TransactionsFilter? {
            val start = savedStateHandle.get<Long>(Screens.Transactions.ARG_START) ?: Screens.Transactions.NO_VALUE
            val end = savedStateHandle.get<Long>(Screens.Transactions.ARG_END) ?: Screens.Transactions.NO_VALUE
            if (start == Screens.Transactions.NO_VALUE) return null

            val granularity = savedStateHandle.get<String>(Screens.Transactions.ARG_GRANULARITY)
                ?.let { name -> PeriodGranularity.entries.find { it.name == name } }
            val categoryId = savedStateHandle.get<Long>(Screens.Transactions.ARG_CATEGORY_ID)
                ?: Screens.Transactions.NO_VALUE
            val categoryName = savedStateHandle.get<String>(Screens.Transactions.ARG_CATEGORY_NAME)

            return TransactionsFilter(
                dateFilter = when {
                    granularity != null -> DateFilter.Period(granularity, start)
                    else -> DateFilter.Custom(DateRange(start, end))
                },
                category = categoryName
                    ?.takeIf { categoryId != Screens.Transactions.NO_VALUE }
                    ?.let { CategoryFilter(categoryId, it) }
            )
        }
    }
}
