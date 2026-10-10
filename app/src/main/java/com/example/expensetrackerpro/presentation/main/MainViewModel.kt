package com.example.expensetrackerpro.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ObserveDeletedTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.RestoreTransactionUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.lock.AppLockController
import com.example.expensetrackerpro.presentation.main.uistate.MainEffect
import com.example.expensetrackerpro.presentation.main.uistate.MainEvent
import com.example.expensetrackerpro.presentation.main.uistate.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    getThemeMode: GetThemeModeUseCase,
    observeDeletedTransactions: ObserveDeletedTransactionsUseCase,
    private val restoreTransaction: RestoreTransactionUseCase,
    private val appLockController: AppLockController
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    // A Channel, not state: each effect is handled once, and waits if nobody is collecting yet
    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Keeps collecting so a change made in Settings re-themes the app immediately
            getThemeMode().collect { mode ->
                _uiState.update { it.copy(themeMode = mode, isThemeLoaded = true) }
            }
        }
        viewModelScope.launch {
            // The first load opens the database (creating and seeding it on first run)
            try {
                getCategories().first()
            } finally {
                // Released even if the load fails, so the splash can never get stuck
                _uiState.update { it.copy(isDataLoaded = true) }
            }
        }
        viewModelScope.launch {
            appLockController.state.collect { lock ->
                _uiState.update {
                    it.copy(
                        isLockStateKnown = lock.isLockStateKnown,
                        isAppLockEnabled = lock.isAppLockEnabled,
                        isLocked = lock.isLocked,
                        isPromptRequested = lock.isPromptRequested,
                        lockMessage = lock.lockMessage
                    )
                }
            }
        }
        appLockController.start(viewModelScope)
        viewModelScope.launch {
            // Listening for as long as the app runs, so a delete on any screen is offered an Undo
            observeDeletedTransactions().collect { deleted ->
                _effects.send(MainEffect.ShowUndoDelete(deleted))
            }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.AppBackgrounded -> appLockController.onAppBackgrounded()
            MainEvent.AppForegrounded -> appLockController.onAppForegrounded()
            MainEvent.UnlockClicked -> appLockController.onUnlockClicked()
            MainEvent.PromptLaunched -> appLockController.onPromptLaunched()
            is MainEvent.AuthFinished -> appLockController.onAuthFinished(event.result)
            is MainEvent.UndoDelete -> undoDelete(event.transaction)
        }
    }

    private fun undoDelete(transaction: Transaction) {
        viewModelScope.launch {
            restoreTransaction(transaction).onFailure {
                _effects.send(MainEffect.ShowRestoreFailed(it.toMessageRes()))
            }
        }
    }

}
