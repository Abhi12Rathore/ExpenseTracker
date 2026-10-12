package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryHint
import javax.inject.Inject

/**
 * The category a hint stands for among [categories], which are those of the transaction's type.
 * Null when none of the hint's categories is there, e.g. it was renamed or deleted.
 */
class ResolveCategoryHintUseCase @Inject constructor() {

    operator fun invoke(hint: CategoryHint, categories: List<Category>): Category? =
        hint.categoryNames.firstNotNullOfOrNull { name ->
            categories.find { it.name.equals(name, ignoreCase = true) }
        }
}
