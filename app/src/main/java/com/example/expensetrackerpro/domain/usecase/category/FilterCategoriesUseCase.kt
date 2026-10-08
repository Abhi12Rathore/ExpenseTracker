package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import javax.inject.Inject

/**
 * Keeps categories of [type]. The name search only applies once [query]
 * has at least [MIN_QUERY_LENGTH] characters.
 */
class FilterCategoriesUseCase @Inject constructor() {

    operator fun invoke(
        categories: List<Category>,
        type: TransactionType,
        query: String
    ): List<Category> = categories.filter { category ->
        val matchesType = category.type == type
        val matchesSearch = if (query.length >= MIN_QUERY_LENGTH) {
            category.name.contains(query, ignoreCase = true)
        } else {
            true
        }

        matchesType && matchesSearch
    }

    private companion object {
        const val MIN_QUERY_LENGTH = 3
    }
}
