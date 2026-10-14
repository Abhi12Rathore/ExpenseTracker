package com.example.expensetrackerpro.domain.usecase.currency

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.models.CurrencyChangeExample
import com.example.expensetrackerpro.domain.models.DisplaySettings
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.usecase.app.ObserveDisplaySettingsUseCase
import com.example.expensetrackerpro.domain.usecase.app.PrepareAppUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.fakes.FakeAppLocaleProvider
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeCurrencyNames
import com.example.expensetrackerpro.fakes.FakeCurrencyRepository
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.fakes.FakeTransactionRepository
import com.example.expensetrackerpro.fakes.localizeDefaultCategories
import com.example.expensetrackerpro.fakes.rupees
import com.example.expensetrackerpro.fakes.transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class CurrencyUseCasesTest {

    private val currencies = FakeCurrencyRepository(AppCurrency.INR)

    @Test
    fun change_setsTheCurrency_andRemembersTheOneBefore() = runTest {
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)

        assertEquals(AppCurrency.USD, currencies.currency.value)
        assertEquals(CurrencyChange(AppCurrency.INR, AppCurrency.USD), currencies.pendingChange.value)
    }

    @Test
    fun change_thatCantBeStored_failsWithoutAnUndo() = runTest {
        currencies.failWrites = true

        assertEquals(Outcome.Failure(DomainError.StorageFailure("disk full")), ChangeCurrencyUseCase(currencies)(AppCurrency.USD))
        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun undo_thatCantBeStored_failsAndKeepsItsUndo() = runTest {
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)
        currencies.failWrites = true

        assertEquals(Outcome.Failure(DomainError.StorageFailure("disk full")), UndoCurrencyChangeUseCase(currencies)())
        assertEquals(AppCurrency.USD, currencies.currency.value)
        assertEquals(CurrencyChange(AppCurrency.INR, AppCurrency.USD), currencies.pendingChange.value)
    }

    @Test
    fun change_toTheCurrentOne_doesNothing() = runTest {
        ChangeCurrencyUseCase(currencies)(AppCurrency.INR)

        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun undo_goesBack_andIsNotItselfOfferedAnUndo() = runTest {
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)

        UndoCurrencyChangeUseCase(currencies)()

        assertEquals(AppCurrency.INR, currencies.currency.value)
        assertNull(currencies.pendingChange.value)
    }

    @Test
    fun undo_onceForgotten_doesNothing() = runTest {
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)
        ForgetCurrencyChangeUseCase(currencies)()

        UndoCurrencyChangeUseCase(currencies)()

        assertEquals(AppCurrency.USD, currencies.currency.value)
    }

    @Test
    fun theChangeExample_isTheLatestTransaction_andHowManyThereAre() = runTest {
        val transactions = FakeTransactionRepository(
            listOf(
                transaction(id = 1, amount = rupees(100), dateMillis = 100L),
                transaction(id = 2, amount = rupees(450), dateMillis = 300L),
                transaction(id = 3, amount = rupees(200), dateMillis = 200L)
            )
        )

        assertEquals(CurrencyChangeExample(3, rupees(450)), GetCurrencyChangeExampleUseCase(transactions)())
        assertNull(GetCurrencyChangeExampleUseCase(FakeTransactionRepository())())
    }

    @Test
    fun search_matchesCodeAndNames_ignoringCaseAndAccents() {
        val locale = FakeAppLocaleProvider(Locale.GERMAN)
        val search = SearchCurrenciesUseCase(FakeCurrencyNames(), locale)

        assertEquals(AppCurrency.entries.toSet(), search("  "))
        assertEquals(setOf(AppCurrency.CHF), search("chf"))
        // German name, then English
        assertEquals(setOf(AppCurrency.CHF), search("FRANKEN"))
        assertEquals(setOf(AppCurrency.CHF), search("franc"))
        assertEquals(setOf(AppCurrency.BRL), search("réal"))
        assertEquals(emptySet<AppCurrency>(), search("zloty"))
    }

    @Test
    fun prepareApp_readsTheCurrency_beforeAnythingIsShown() = runTest {
        val fresh = FakeCurrencyRepository(AppCurrency.INR, stored = null, region = AppCurrency.EUR)

        PrepareAppUseCase(LoadCurrencyUseCase(fresh), localizeDefaultCategories(FakeCategoryRepository()))()

        assertEquals(1, fresh.loads)
        assertEquals(AppCurrency.EUR, fresh.currency.value)
    }

    @Test
    fun displaySettings_followTheThemeAndTheCurrency() = runTest {
        val theme = FakeThemePreferenceRepository()
        val observe = ObserveDisplaySettingsUseCase(GetThemeModeUseCase(theme), ObserveCurrencyUseCase(currencies))

        assertEquals(DisplaySettings(ThemeMode.SYSTEM, AppCurrency.INR), observe().first())

        currencies.setCurrency(AppCurrency.JPY)
        theme.setThemeMode(ThemeMode.DARK)

        assertEquals(DisplaySettings(ThemeMode.DARK, AppCurrency.JPY), observe().first())
    }
}
