package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.DeviceLanguages
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.repository.AppLanguageRepository
import com.example.expensetrackerpro.domain.time.HourFormatProvider
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.usecase.category.LocalizeDefaultCategoriesUseCase
import com.example.expensetrackerpro.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.TimeZone

val HINDI: Locale = Locale.forLanguageTag("hi")

/** The app shown in [locale] until a test changes it. */
class FakeAppLocaleProvider(var locale: Locale = Locale.ENGLISH) : AppLocaleProvider {

    override fun current(): Locale = locale
}

/** The phone set to [list], most preferred first. */
class FakeDeviceLanguages(var list: List<Locale> = listOf(Locale.forLanguageTag("en-IN"))) : DeviceLanguages {

    override fun locales(): List<Locale> = list
}

/**
 * English names as the app has them, and for Hindi the same name behind "हि ", apart from the
 * few in [hindi]. Any other language falls back to English, as resources do.
 */
class FakeDefaultCategoryNames : DefaultCategoryNames {

    override fun nameOf(category: BuiltInCategory, locale: Locale): String {
        val english = english.getValue(category)

        return if (locale.language == "hi") hindi[category] ?: "हि $english" else english
    }

    companion object {
        val english: Map<BuiltInCategory, String> = mapOf(
            BuiltInCategory.FOOD_DINING to "Food & Dining",
            BuiltInCategory.GROCERIES to "Groceries",
            BuiltInCategory.TRANSPORT to "Transport",
            BuiltInCategory.FUEL to "Fuel",
            BuiltInCategory.SHOPPING to "Shopping",
            BuiltInCategory.BILLS_UTILITIES to "Bills & Utilities",
            BuiltInCategory.RENT_HOME to "Rent & Home",
            BuiltInCategory.HEALTH to "Health",
            BuiltInCategory.EDUCATION to "Education",
            BuiltInCategory.ENTERTAINMENT to "Entertainment",
            BuiltInCategory.TRAVEL to "Travel",
            BuiltInCategory.SUBSCRIPTIONS to "Subscriptions",
            BuiltInCategory.PERSONAL_CARE to "Personal Care",
            BuiltInCategory.GIFTS_DONATIONS to "Gifts & Donations",
            BuiltInCategory.EMI_LOANS to "EMI & Loans",
            BuiltInCategory.OTHER to "Other",
            BuiltInCategory.SALARY to "Salary",
            BuiltInCategory.FREELANCE to "Freelance",
            BuiltInCategory.BUSINESS to "Business",
            BuiltInCategory.INVESTMENTS to "Investments",
            BuiltInCategory.INTEREST to "Interest",
            BuiltInCategory.RENTAL_INCOME to "Rental Income",
            BuiltInCategory.REFUNDS_CASHBACK to "Refunds & Cashback",
            BuiltInCategory.GIFTS_RECEIVED to "Gifts Received",
            BuiltInCategory.OTHER_INCOME to "Other Income"
        )

        val hindi: Map<BuiltInCategory, String> = mapOf(
            BuiltInCategory.FOOD_DINING to "खाना-पीना",
            BuiltInCategory.FUEL to "ईंधन",
            BuiltInCategory.OTHER to "अन्य",
            BuiltInCategory.SALARY to "सैलरी"
        )
    }
}

/** Keeps the choice in memory, where the app hands it to Android. */
class FakeAppLanguageRepository(initial: LanguageChoice = LanguageChoice.SystemDefault) : AppLanguageRepository {

    var stored: LanguageChoice = initial

    private val _pendingChange = MutableStateFlow<LanguageChange?>(null)
    override val pendingChange: StateFlow<LanguageChange?> = _pendingChange

    override fun choice(): LanguageChoice = stored

    override fun setChoice(choice: LanguageChoice) {
        stored = choice
    }

    override fun setPendingChange(change: LanguageChange?) {
        _pendingChange.value = change
    }
}

/** The phone's clock setting, 12-hour until a test changes it. */
class FakeHourFormatProvider(var is24Hours: Boolean = false) : HourFormatProvider {

    override fun uses24Hours(): Boolean = is24Hours
}

/** A formatter for [locale] in [timeZone], whatever the machine running the test is set to. */
fun dateFormatter(
    locale: Locale = Locale.US,
    timeZone: TimeZone = KOLKATA,
    hourFormatProvider: HourFormatProvider = FakeHourFormatProvider()
): DateFormatter = DateFormatter(
    localeProvider = { locale },
    timeProvider = FakeTimeProvider(nowMillis = 0L, timeZone = timeZone),
    hourFormatProvider = hourFormatProvider
)

fun localizeDefaultCategories(
    repository: CategoryRepository,
    localeProvider: AppLocaleProvider = FakeAppLocaleProvider()
) = LocalizeDefaultCategoriesUseCase(repository, FakeDefaultCategoryNames(), localeProvider)
