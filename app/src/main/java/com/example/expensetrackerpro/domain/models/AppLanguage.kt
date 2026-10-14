package com.example.expensetrackerpro.domain.models

import com.example.expensetrackerpro.domain.currency.SymbolPlacement

/**
 * The languages the app is translated into. [nativeName] and [badge] are written in the
 * language itself and are the same whatever the app is shown in, so its speakers can find it.
 */
enum class AppLanguage(
    /** The BCP 47 tag, as in `res/xml/locales_config.xml`. */
    val tag: String,
    val nativeName: String,
    val englishName: String,
    /** Two letters of its script, shown where a flag would be: flags are countries, not languages. */
    val badge: String,
    /** Where amounts in this language put the currency's symbol: `$5.00` or `5,00 $`. */
    val symbolPlacement: SymbolPlacement = SymbolPlacement.BEFORE,
    /** Whether a symbol written before the amount is followed by a space even when it is a sign: `R$ 5,00`. */
    val spaceAfterSymbol: Boolean = false
) {
    ENGLISH(tag = "en", nativeName = "English", englishName = "English", badge = "En"),
    HINDI(tag = "hi", nativeName = "हिन्दी", englishName = "Hindi", badge = "हि"),
    SPANISH(tag = "es", nativeName = "Español", englishName = "Spanish", badge = "Es", symbolPlacement = SymbolPlacement.AFTER),
    FRENCH(tag = "fr", nativeName = "Français", englishName = "French", badge = "Fr", symbolPlacement = SymbolPlacement.AFTER),
    GERMAN(tag = "de", nativeName = "Deutsch", englishName = "German", badge = "De", symbolPlacement = SymbolPlacement.AFTER),

    // Written for Brazil, and the only Portuguese the app has, so it serves every Portuguese phone
    PORTUGUESE_BRAZIL(
        tag = "pt-BR",
        nativeName = "Português (Brasil)",
        englishName = "Portuguese",
        badge = "Pt",
        spaceAfterSymbol = true
    );

    /** The language without its region, e.g. `pt`. No two entries share one. */
    val languageCode: String get() = tag.languageCode()

    companion object {
        /**
         * The language of a tag such as `hi`, `hi-IN` or `pt_PT`, or null when the app is not
         * translated into it. Only the language counts, as it does when Android picks the texts.
         */
        fun findByTag(tag: String): AppLanguage? {
            val language = tag.languageCode()

            return entries.find { it.languageCode == language }
        }

        private fun String.languageCode(): String = substringBefore('-').substringBefore('_').lowercase()
    }
}

/** What the user picked on the Language screen. */
sealed interface LanguageChoice {
    /** Follow the phone's language. */
    data object SystemDefault : LanguageChoice

    data class Specific(val language: AppLanguage) : LanguageChoice
}

/** A language change that has just been applied and can still be undone. */
data class LanguageChange(
    val previous: LanguageChoice,
    /** The language the app is shown in once the change has taken effect. */
    val language: AppLanguage
)

/** What the Language screen lists. */
data class LanguageOptions(
    val current: LanguageChoice,
    /** The language the app is shown in while it follows the phone. */
    val systemLanguage: AppLanguage,
    /** The phone's own name for its first language, e.g. `English (India)`. */
    val systemLanguageName: String,
    /** The phone's other languages the app is translated into, most preferred first. */
    val suggested: List<AppLanguage>,
    /** Every language that is not among [suggested]. */
    val others: List<AppLanguage>
)

/** A small Home summary, for showing what the app looks like in [language] before switching to it. */
data class LanguagePreview(
    val language: AppLanguage,
    val dayPeriod: DayPeriod?,
    val userName: String,
    val summary: FinancialSummary,
    /** This month's biggest categories, named as they would be in [language]. */
    val categories: List<CategoryBreakdown>
)
