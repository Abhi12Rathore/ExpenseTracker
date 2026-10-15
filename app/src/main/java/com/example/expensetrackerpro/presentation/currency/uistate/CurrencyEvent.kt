package com.example.expensetrackerpro.presentation.currency.uistate

import com.example.expensetrackerpro.domain.currency.AppCurrency

sealed interface CurrencyEvent {
    data class CurrencySelected(val currency: AppCurrency) : CurrencyEvent

    data class QueryChanged(val query: String) : CurrencyEvent

    // The button under the list: asks first when there are transactions to explain the change with
    data object ApplyClicked : CurrencyEvent

    data object ConfirmClicked : CurrencyEvent

    data object ConfirmDismissed : CurrencyEvent

    data object ErrorShown : CurrencyEvent
}
