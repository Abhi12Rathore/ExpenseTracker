package com.example.expensetrackerpro.domain.usecase.category

import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Puts the built-in categories into the app's language. Run when the app starts and whenever
 * its language changes; running it again changes nothing.
 *
 * Only a category that still has its built-in key is renamed, so a name the user gave is never
 * replaced. A built-in whose new name another category of its type already has keeps the name
 * it has now, as two categories of one type can't share a name.
 */
class LocalizeDefaultCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val names: DefaultCategoryNames,
    private val localeProvider: AppLocaleProvider
) {

    suspend operator fun invoke(): Outcome<Unit> {
        val locale = localeProvider.current()
        val categories = categoryRepository.getAllCategories().first()

        val wanted = categories
            .mapNotNull { category -> category.builtInKey?.let { category.id to names.nameOf(it, locale) } }
            .filter { (id, name) -> categories.first { it.id == id }.name != name }
            .toMap()
        val renames = withoutCollisions(wanted, categories)

        return if (renames.isEmpty()) Outcome.Success(Unit) else categoryRepository.renameBuiltInCategories(renames)
    }

    /**
     * [wanted] less every rename that would give two categories of one type the same name.
     * Names are compared as they will be once the renames are made, so dropping one rename
     * can take another with it: hence the loop.
     */
    private fun withoutCollisions(
        wanted: Map<CategoryId, String>,
        categories: List<Category>
    ): Map<CategoryId, String> {
        var renames = wanted
        while (true) {
            val namesAfter = categories.associate { it.id to (renames[it.id] ?: it.name) }
            val colliding = renames.filter { (id, name) ->
                val type = categories.first { it.id == id }.type
                categories.any { other ->
                    other.id != id && other.type == type && namesAfter.getValue(other.id).isSameNameAs(name)
                }
            }.keys
            if (colliding.isEmpty()) return renames
            renames = renames - colliding
        }
    }

    // As ValidateCategoryNameUseCase compares names
    private fun String.isSameNameAs(other: String): Boolean = trim().equals(other.trim(), ignoreCase = true)
}
