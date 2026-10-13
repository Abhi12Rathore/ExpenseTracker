package com.example.expensetrackerpro.domain.models

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
    val badge: String
) {
    ENGLISH(tag = "en", nativeName = "English", englishName = "English", badge = "En"),
    HINDI(tag = "hi", nativeName = "हिन्दी", englishName = "Hindi", badge = "हि");

    companion object {
        /** The language of a tag such as `hi` or `hi-IN`, or null when the app is not translated into it. */
        fun findByTag(tag: String): AppLanguage? {
            val language = tag.substringBefore('-').substringBefore('_').lowercase()

            return entries.find { it.tag == language }
        }
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
