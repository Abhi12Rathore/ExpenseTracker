package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.LanguagePreview
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.dashboard.GetDashboardUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * The user's own Home figures for this month, as they would read in another language. Nothing
 * is changed: the built-in categories are only named as that language names them.
 */
class GetLanguagePreviewUseCase @Inject constructor(
    private val getDashboard: GetDashboardUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val names: DefaultCategoryNames,
    private val deviceLanguages: DeviceLanguages,
    private val resolveAppLocale: ResolveAppLocaleUseCase
) {

    suspend operator fun invoke(choice: LanguageChoice): LanguagePreview {
        val locale = resolveAppLocale(choice, deviceLanguages.locales())
        val dashboard = getDashboard(getCurrentPeriod(PeriodGranularity.MONTH)).first()
        val builtInKeys = getCategories().first().associate { it.id to it.builtInKey }

        return LanguagePreview(
            language = AppLanguage.findByTag(locale.language) ?: AppLanguage.ENGLISH,
            dayPeriod = dashboard.dayPeriod,
            userName = dashboard.profile.name,
            summary = dashboard.summary,
            categories = dashboard.categoryRanking.top.take(CATEGORY_COUNT).map { share ->
                val builtIn = builtInKeys[share.category.categoryId]
                // A renamed or custom category reads the same in every language
                if (builtIn == null) share.category else share.category.copy(categoryName = names.nameOf(builtIn, locale))
            }
        )
    }

    private companion object {
        const val CATEGORY_COUNT = 2
    }
}
