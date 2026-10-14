package com.example.expensetrackerpro.presentation.currency

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.usecase.currency.ChangeCurrencyUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyChangeExampleUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.currency.GetCurrencyPreviewUseCase
import com.example.expensetrackerpro.domain.usecase.currency.SearchCurrenciesUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.currency.uistate.CurrencyEvent
import com.example.expensetrackerpro.presentation.currency.uistate.CurrencyUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    getCurrencyOptions: GetCurrencyOptionsUseCase,
    getCurrencyPreview: GetCurrencyPreviewUseCase,
    private val searchCurrencies: SearchCurrenciesUseCase,
    private val getCurrencyChangeExample: GetCurrencyChangeExampleUseCase,
    private val changeCurrency: ChangeCurrencyUseCase,
    // Keeps an unapplied selection and the search through rotation and process death; leaving the screen drops them
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        getCurrencyOptions().let { options ->
            val query = savedStateHandle.get<String>(KEY_QUERY).orEmpty()
            CurrencyUiState(
                current = options.current,
                // A code the app no longer offers is ignored, and the screen opens on the current currency
                selected = savedStateHandle.get<String>(KEY_SELECTED)?.let(AppCurrency::findByCode) ?: options.current,
                suggested = options.suggested,
                others = options.others,
                query = query,
                matches = searchCurrencies(query)
            )
        }
    )
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    // Asking or applying: a second tap meanwhile does nothing
    private var applyJob: Job? = null

    init {
        // The figures are the same in every currency: only how they are written changes
        viewModelScope.launch {
            val preview = getCurrencyPreview()
            _uiState.update { it.copy(preview = preview) }
        }
    }

    fun onEvent(event: CurrencyEvent) {
        when (event) {
            is CurrencyEvent.CurrencySelected -> select(event.currency)
            is CurrencyEvent.QueryChanged -> search(event.query)
            CurrencyEvent.ApplyClicked -> requestApply()
            CurrencyEvent.ConfirmClicked -> if (_uiState.value.confirmation != null) apply()
            CurrencyEvent.ConfirmDismissed -> _uiState.update { it.copy(confirmation = null) }
            CurrencyEvent.ErrorShown -> _uiState.update { it.copy(errorRes = null) }
        }
    }

    private fun select(currency: AppCurrency) {
        // Only the selection changes: the lists stay as they are, so the screen's scroll never moves
        _uiState.update { it.copy(selected = currency) }
        savedStateHandle[KEY_SELECTED] = currency.code
    }

    private fun search(query: String) {
        _uiState.update { it.copy(query = query, matches = searchCurrencies(query)) }
        savedStateHandle[KEY_QUERY] = query
    }

    // Nothing to explain to someone without a transaction yet, so the change is made at once
    private fun requestApply() {
        if (!_uiState.value.isApplyVisible || applyJob?.isActive == true) return

        applyJob = viewModelScope.launch {
            when (val example = getCurrencyChangeExample()) {
                null -> applySelection()
                else -> _uiState.update { it.copy(confirmation = example) }
            }
        }
    }

    private fun apply() {
        if (applyJob?.isActive == true) return
        applyJob = viewModelScope.launch { applySelection() }
    }

    private suspend fun applySelection() {
        val state = _uiState.value
        if (state.isApplied) return

        when (val result = changeCurrency(state.selected)) {
            is Outcome.Success -> _uiState.update { it.copy(confirmation = null, isApplied = true) }
            // The screen stays open on the selection, so it can be tried again
            is Outcome.Failure -> _uiState.update {
                it.copy(confirmation = null, errorRes = result.error.toMessageRes())
            }
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_currency"
        const val KEY_QUERY = "currency_query"
    }
}
