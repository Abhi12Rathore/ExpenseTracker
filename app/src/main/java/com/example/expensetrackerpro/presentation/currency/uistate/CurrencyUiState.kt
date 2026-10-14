package com.example.expensetrackerpro.presentation.currency.uistate

import androidx.annotation.StringRes
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.models.CurrencyChangeExample
import com.example.expensetrackerpro.domain.models.CurrencyPreview

data class CurrencyUiState(
    // What amounts are shown in now
    val current: AppCurrency = AppCurrency.INR,
    // The row that is ticked; nothing changes until it is applied
    val selected: AppCurrency = AppCurrency.INR,
    // The current currency and the phone region's, then every other one by name. Never reordered while open
    val suggested: List<AppCurrency> = emptyList(),
    val others: List<AppCurrency> = emptyList(),
    val query: String = "",
    // The currencies the search matches; rows outside it are left out, the rest keep their places
    val matches: Set<AppCurrency> = AppCurrency.entries.toSet(),
    // This month's figures, written in the selected currency; null only until they have loaded
    val preview: CurrencyPreview? = null,
    // The example the change is explained with; non-null while the confirmation is showing
    val confirmation: CurrencyChangeExample? = null,
    // True once the change was made, which closes the screen
    val isApplied: Boolean = false,
    // Why the change couldn't be made, until the screen has shown it
    @StringRes val errorRes: Int? = null
) {
    /** The apply button is offered only for a currency other than the current one. */
    val isApplyVisible: Boolean get() = selected != current && !isApplied

    val visibleSuggested: List<AppCurrency> get() = suggested.filter { it in matches }

    val visibleOthers: List<AppCurrency> get() = others.filter { it in matches }
}
