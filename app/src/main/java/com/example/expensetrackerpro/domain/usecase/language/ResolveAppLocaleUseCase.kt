package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChoice
import java.util.Locale
import javax.inject.Inject

/**
 * The locale a choice of language comes to. Following the phone means its first language the
 * app is translated into, which is also how Android picks the app's texts, so a date is never
 * written in a language the words around it are not in. English when there is none.
 *
 * The phone's own locale for the language is used where it has one, so the region's habits are
 * kept: an Indian phone writes `4:20 pm` in English whether English was chosen or followed.
 */
class ResolveAppLocaleUseCase @Inject constructor() {

    operator fun invoke(choice: LanguageChoice, deviceLocales: List<Locale>): Locale = when (choice) {
        is LanguageChoice.Specific ->
            deviceLocales.firstOrNull { AppLanguage.findByTag(it.language) == choice.language }
                ?: Locale.forLanguageTag(choice.language.tag)
        LanguageChoice.SystemDefault ->
            deviceLocales.firstOrNull { AppLanguage.findByTag(it.language) != null } ?: Locale.ENGLISH
    }
}
