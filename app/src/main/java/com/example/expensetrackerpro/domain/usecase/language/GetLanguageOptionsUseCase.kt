package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.LanguageOptions
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import java.util.Locale
import javax.inject.Inject

/** What the Language screen offers: following the phone and the phone's own languages first, then the rest. */
class GetLanguageOptionsUseCase @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository,
    private val deviceLanguages: DeviceLanguages,
    private val resolveAppLocale: ResolveAppLocaleUseCase
) {

    operator fun invoke(): LanguageOptions {
        val deviceLocales = deviceLanguages.locales()
        val systemLocale = resolveAppLocale(LanguageChoice.SystemDefault, deviceLocales)
        // Named after the phone's first language, in that language, even one the app lacks
        val firstLocale = deviceLocales.firstOrNull() ?: systemLocale

        val systemLanguage = AppLanguage.findByTag(systemLocale.language) ?: AppLanguage.ENGLISH
        // "System default" already stands for the first of them
        val suggested = deviceLocales.mapNotNull { AppLanguage.findByTag(it.language) }.distinct() - systemLanguage

        return LanguageOptions(
            current = appLanguageRepository.choice(),
            systemLanguage = systemLanguage,
            systemLanguageName = firstLocale.getDisplayName(firstLocale).capitalized(firstLocale),
            suggested = suggested,
            others = AppLanguage.entries - suggested.toSet()
        )
    }

    // Some languages write their own name without a capital, e.g. "español (España)"
    private fun String.capitalized(locale: Locale): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}
