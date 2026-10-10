package com.example.expensetrackerpro.domain.models

/** One type's categories as Manage Categories lists them, with how many each type has. */
data class CategorySections(
    // Unaffected by the search, so the tabs keep their totals
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    // Used in the period, highest amount first
    val mostUsed: List<CategoryUsage> = emptyList(),
    // Not used in the period, by name
    val unused: List<CategoryUsage> = emptyList()
) {
    val isEmpty: Boolean get() = mostUsed.isEmpty() && unused.isEmpty()
}
