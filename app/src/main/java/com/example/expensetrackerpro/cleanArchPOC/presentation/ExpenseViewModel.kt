package com.example.expensetrackerpro.cleanArchPOC.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.cleanArchPOC.domain.model.Expense
import com.example.expensetrackerpro.cleanArchPOC.domain.result.RefreshExpensesResult
import com.example.expensetrackerpro.cleanArchPOC.domain.useCase.ObserveExpensesUseCase
import com.example.expensetrackerpro.cleanArchPOC.domain.useCase.RefreshExpensesUseCase
import com.example.expensetrackerpro.cleanArchPOC.presentation.error.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val observeExpensesUseCase: ObserveExpensesUseCase,
    private val refreshExpensesUseCase: RefreshExpensesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ExpenseUiState()
    )

    val uiState: StateFlow<ExpenseUiState> =
        _uiState.asStateFlow()

    init {
        observeExpenses()
        refreshExpenses()
    }

    private fun observeExpenses() {
        viewModelScope.launch {
            observeExpensesUseCase().collect { expenses ->
                _uiState.update { currentState ->
                    currentState.copy(
                        expenses = expenses
                    )
                }
            }
        }
    }

    private fun refreshExpenses() {
        viewModelScope.launch {
            // api loading......

            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    refreshError = null
                )
            }

            val result = refreshExpensesUseCase()
            when (result) {
                is RefreshExpensesResult.Success -> {
                    _uiState.update {
                        it.copy(isRefreshing = false, refreshError = null)
                    }
                }

                is RefreshExpensesResult.Failure -> {
                    _uiState.update {
                        it.copy(isRefreshing = false, refreshError = result.error.toUiError())
                    }
                }
            }
        }
    }
}
/*
* The Domain layer contains business rules and contracts, so it should remain independent of infrastructure
* implementation details. If Domain directly depends on Data, the business logic becomes coupled to concrete
* repositories, networking, databases, DTOs, entities, and other infrastructure details. As a result,
* infrastructure changes can ripple into the Domain layer.

Instead, Domain defines abstractions, such as repository interfaces, and the Data layer implements those contracts.
* This keeps the business logic independent of the actual data source.

If Domain directly depends on Data, it increases coupling, reduces change isolation and testability,
* mixes responsibilities, and can also create circular dependencies when these layers are separated into
*  Gradle modules.”
* */