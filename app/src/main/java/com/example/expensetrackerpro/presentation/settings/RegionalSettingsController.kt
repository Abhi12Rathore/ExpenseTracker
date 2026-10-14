package com.example.expensetrackerpro.presentation.settings

import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.usecase.currency.ForgetCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.currency.ObserveCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.currency.ObserveCurrencyUseCase
import com.example.expensetrackerpro.domain.usecase.currency.UndoCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.ForgetLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguageOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.language.ObserveLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.UndoLanguageChangeUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Settings shows about how the app writes things: its language and its currency. */
data class RegionalSettingsState(
    val language: LanguageChoice = LanguageChoice.SystemDefault,
    // A change that was just applied, while it can still be undone
    val languageChange: LanguageChange? = null,
    val currency: AppCurrency = AppCurrency.INR,
    val currencyChange: CurrencyChange? = null
)

/**
 * The Language and Currency rows on Settings and the Undo offered after either changes. One
 * belongs to each [SettingsViewModel], which shows its state and forwards its events.
 */
class RegionalSettingsController @Inject constructor(
    private val getLanguageOptions: GetLanguageOptionsUseCase,
    observeLanguageChange: ObserveLanguageChangeUseCase,
    private val undoLanguageChange: UndoLanguageChangeUseCase,
    private val forgetLanguageChange: ForgetLanguageChangeUseCase,
    observeCurrency: ObserveCurrencyUseCase,
    observeCurrencyChange: ObserveCurrencyChangeUseCase,
    private val undoCurrencyChange: UndoCurrencyChangeUseCase,
    private val forgetCurrencyChange: ForgetCurrencyChangeUseCase
) {
    private val choice = MutableStateFlow(getLanguageOptions().current)

    val state: Flow<RegionalSettingsState> =
        combine(
            choice,
            observeLanguageChange(),
            observeCurrency(),
            observeCurrencyChange()
        ) { choice, languageChange, currency, currencyChange ->
            RegionalSettingsState(choice, languageChange, currency, currencyChange)
        }

    /** Reads the language again: it changes on another screen, or in the phone's settings for the app. */
    fun refresh() {
        choice.value = getLanguageOptions().current
    }

    fun undoLanguage() = undoLanguageChange()

    /** The Undo was offered and not taken. */
    fun onLanguageChangeShown() = forgetLanguageChange()

    fun undoCurrency(scope: CoroutineScope) {
        scope.launch { undoCurrencyChange() }
    }

    /** The Undo was offered and not taken. */
    fun onCurrencyChangeShown() = forgetCurrencyChange()
}
