package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import javax.inject.Inject

class SetAppLanguageUseCase @Inject constructor(
    private val appLanguageRepository: AppLanguageRepository,
    private val deviceLanguages: DeviceLanguages,
    private val resolveAppLocale: ResolveAppLocaleUseCase
) {

    /** Switches the app to [choice] and remembers what it was shown in, so the switch can be undone. */
    operator fun invoke(choice: LanguageChoice) {
        val previous = appLanguageRepository.choice()
        if (previous == choice) return

        val locale = resolveAppLocale(choice, deviceLanguages.locales())
        val language = AppLanguage.findByTag(locale.language) ?: AppLanguage.ENGLISH
        // Before the switch: the screens are drawn again as soon as it is made
        appLanguageRepository.setPendingChange(LanguageChange(previous = previous, language = language))
        appLanguageRepository.setChoice(choice)
    }
}
