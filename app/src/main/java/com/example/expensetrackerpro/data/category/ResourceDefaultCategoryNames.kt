package com.example.expensetrackerpro.data.category

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.category.DefaultCategoryNames
import com.example.expensetrackerpro.domain.models.BuiltInCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** The built-in categories' names, from the string resources of whichever language is asked for. */
@Singleton
class ResourceDefaultCategoryNames @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultCategoryNames {

    // A context per language asked for; there are only ever a few
    private val localizedContexts = ConcurrentHashMap<Locale, Context>()

    override fun nameOf(category: BuiltInCategory, locale: Locale): String =
        localizedContexts.getOrPut(locale) { context.localizedFor(locale) }.getString(category.nameRes())

    // Reads the resources of [locale] whatever language the app is shown in
    private fun Context.localizedFor(locale: Locale): Context =
        createConfigurationContext(Configuration(resources.configuration).apply { setLocale(locale) })

    @StringRes
    private fun BuiltInCategory.nameRes(): Int = when (this) {
        BuiltInCategory.FOOD_DINING -> R.string.category_default_food_dining
        BuiltInCategory.GROCERIES -> R.string.category_default_groceries
        BuiltInCategory.TRANSPORT -> R.string.category_default_transport
        BuiltInCategory.FUEL -> R.string.category_default_fuel
        BuiltInCategory.SHOPPING -> R.string.category_default_shopping
        BuiltInCategory.BILLS_UTILITIES -> R.string.category_default_bills_utilities
        BuiltInCategory.RENT_HOME -> R.string.category_default_rent_home
        BuiltInCategory.HEALTH -> R.string.category_default_health
        BuiltInCategory.EDUCATION -> R.string.category_default_education
        BuiltInCategory.ENTERTAINMENT -> R.string.category_default_entertainment
        BuiltInCategory.TRAVEL -> R.string.category_default_travel
        BuiltInCategory.SUBSCRIPTIONS -> R.string.category_default_subscriptions
        BuiltInCategory.PERSONAL_CARE -> R.string.category_default_personal_care
        BuiltInCategory.GIFTS_DONATIONS -> R.string.category_default_gifts_donations
        BuiltInCategory.EMI_LOANS -> R.string.category_default_emi_loans
        BuiltInCategory.OTHER -> R.string.category_default_other
        BuiltInCategory.SALARY -> R.string.category_default_salary
        BuiltInCategory.FREELANCE -> R.string.category_default_freelance
        BuiltInCategory.BUSINESS -> R.string.category_default_business
        BuiltInCategory.INVESTMENTS -> R.string.category_default_investments
        BuiltInCategory.INTEREST -> R.string.category_default_interest
        BuiltInCategory.RENTAL_INCOME -> R.string.category_default_rental_income
        BuiltInCategory.REFUNDS_CASHBACK -> R.string.category_default_refunds_cashback
        BuiltInCategory.GIFTS_RECEIVED -> R.string.category_default_gifts_received
        BuiltInCategory.OTHER_INCOME -> R.string.category_default_other_income
    }
}
