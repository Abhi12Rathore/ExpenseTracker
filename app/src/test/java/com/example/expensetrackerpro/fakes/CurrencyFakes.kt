package com.example.expensetrackerpro.fakes

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.currency.CurrencyNames
import com.example.expensetrackerpro.domain.currency.CurrencyProvider
import com.example.expensetrackerpro.domain.currency.NumberSymbols
import com.example.expensetrackerpro.domain.currency.RegionCurrency
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.locale.AppLocaleProvider
import com.example.expensetrackerpro.domain.locale.NumberSymbolsProvider
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.repository.CurrencyRepository
import com.example.expensetrackerpro.domain.usecase.transaction.SanitizeAmountInputUseCase
import com.example.expensetrackerpro.utils.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** Amounts shown in [currency] until a test changes it; counts how often it was asked. */
class FakeCurrencyProvider(var currency: AppCurrency = AppCurrency.INR) : CurrencyProvider {

    var reads = 0
        private set

    override fun current(): AppCurrency {
        reads++
        return currency
    }
}

/**
 * The separators each of the app's languages writes numbers with, as Android gives them, so
 * tests don't depend on the JVM's locale data. French groups with a narrow no-break space.
 */
class FakeNumberSymbols : NumberSymbolsProvider {

    override fun symbolsFor(locale: Locale): NumberSymbols = when (locale.language) {
        "es", "de", "pt" -> NumberSymbols(decimalSeparator = ',', groupingSeparator = '.')
        "fr" -> NumberSymbols(decimalSeparator = ',', groupingSeparator = NARROW_NO_BREAK_SPACE)
        else -> NumberSymbols.ENGLISH
    }

    companion object {
        const val NARROW_NO_BREAK_SPACE = ' '
    }
}

/** The no-break space the formatter puts between an amount and a symbol written apart from it. */
const val NBSP = ' '

/** Keeps the currency in memory, where the app stores it in DataStore. */
class FakeCurrencyRepository(
    initial: AppCurrency = AppCurrency.INR,
    // What load() finds stored; null for a fresh install, which then takes the region's or INR
    var stored: AppCurrency? = initial,
    private val region: AppCurrency? = null
) : CurrencyRepository, CurrencyProvider {

    private val _currency = MutableStateFlow(initial)
    override val currency: StateFlow<AppCurrency> = _currency

    private val _pendingChange = MutableStateFlow<CurrencyChange?>(null)
    override val pendingChange: StateFlow<CurrencyChange?> = _pendingChange

    var loads = 0
        private set

    // A preferences file that can't be written: every change fails and nothing is stored
    var failWrites = false

    override fun current(): AppCurrency = _currency.value

    override suspend fun load() {
        loads++
        _currency.value = stored ?: (region ?: AppCurrency.INR).also { stored = it }
    }

    override suspend fun setCurrency(currency: AppCurrency): Outcome<Unit> {
        if (failWrites) return Outcome.Failure(DomainError.StorageFailure("disk full"))
        stored = currency
        _currency.value = currency
        return Outcome.Success(Unit)
    }

    override fun setPendingChange(change: CurrencyChange?) {
        _pendingChange.value = change
    }
}

/** The phone set to a region whose currency is [currency]. */
class FakeRegionCurrency(var currency: AppCurrency? = null) : RegionCurrency {

    override fun currency(): AppCurrency? = currency
}

/** English names as the app has them, and a few German ones; any other language falls back to English. */
class FakeCurrencyNames : CurrencyNames {

    override fun nameOf(currency: AppCurrency, locale: Locale): String =
        (if (locale.language == "de") german[currency] else null) ?: english.getValue(currency)

    companion object {
        val english: Map<AppCurrency, String> = mapOf(
            AppCurrency.INR to "Indian Rupee",
            AppCurrency.USD to "US Dollar",
            AppCurrency.EUR to "Euro",
            AppCurrency.GBP to "British Pound",
            AppCurrency.AED to "UAE Dirham",
            AppCurrency.SAR to "Saudi Riyal",
            AppCurrency.QAR to "Qatari Riyal",
            AppCurrency.SGD to "Singapore Dollar",
            AppCurrency.AUD to "Australian Dollar",
            AppCurrency.CAD to "Canadian Dollar",
            AppCurrency.NZD to "New Zealand Dollar",
            AppCurrency.JPY to "Japanese Yen",
            AppCurrency.CNY to "Chinese Yuan",
            AppCurrency.HKD to "Hong Kong Dollar",
            AppCurrency.MYR to "Malaysian Ringgit",
            AppCurrency.THB to "Thai Baht",
            AppCurrency.IDR to "Indonesian Rupiah",
            AppCurrency.NPR to "Nepalese Rupee",
            AppCurrency.LKR to "Sri Lankan Rupee",
            AppCurrency.BDT to "Bangladeshi Taka",
            AppCurrency.PKR to "Pakistani Rupee",
            AppCurrency.ZAR to "South African Rand",
            AppCurrency.BRL to "Brazilian Real",
            AppCurrency.CHF to "Swiss Franc"
        )

        val german: Map<AppCurrency, String> = mapOf(
            AppCurrency.USD to "US-Dollar",
            AppCurrency.CHF to "Schweizer Franken",
            AppCurrency.JPY to "Japanischer Yen"
        )
    }
}

/** A formatter for [currency] in [locale], with the separators Android gives that language. */
fun currencyFormatter(
    currency: AppCurrency = AppCurrency.INR,
    locale: Locale = Locale.ENGLISH
): CurrencyFormatter = CurrencyFormatter(
    currencyProvider = { currency },
    localeProvider = { locale },
    numberSymbolsProvider = FakeNumberSymbols()
)

fun sanitizeAmountInput(
    currencyProvider: CurrencyProvider = CurrencyProvider { AppCurrency.INR },
    localeProvider: AppLocaleProvider = AppLocaleProvider { Locale.ENGLISH }
) = SanitizeAmountInputUseCase(currencyProvider, localeProvider, FakeNumberSymbols())
