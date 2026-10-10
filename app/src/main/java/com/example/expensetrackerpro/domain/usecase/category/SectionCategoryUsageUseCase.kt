package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.CategorySections
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.TransactionType
import javax.inject.Inject

/**
 * Splits the categories of [type] whose name contains [query] into those used in the period,
 * highest amount first, and the rest by name.
 */
class SectionCategoryUsageUseCase @Inject constructor() {

    operator fun invoke(
        usages: List<CategoryUsage>,
        type: TransactionType,
        query: String
    ): CategorySections {
        val search = query.trim()
        val (used, unused) = usages
            .filter { it.category.type == type && it.category.name.contains(search, ignoreCase = true) }
            .partition { it.periodCount > 0 }

        return CategorySections(
            expenseCount = usages.count { it.category.type == TransactionType.EXPENSE },
            incomeCount = usages.count { it.category.type == TransactionType.INCOME },
            mostUsed = used.sortedWith(
                compareByDescending<CategoryUsage> { it.periodAmount }.thenBy { it.category.name.lowercase() }
            ),
            unused = unused.sortedBy { it.category.name.lowercase() }
        )
    }
}
