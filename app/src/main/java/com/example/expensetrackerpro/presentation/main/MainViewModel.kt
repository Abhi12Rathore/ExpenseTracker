package com.example.expensetrackerpro.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetCurrentTimeMillisUseCase
import com.example.expensetrackerpro.domain.usecase.applock.ShouldLockUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.ObserveDeletedTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.RestoreTransactionUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.main.uistate.LockMessage
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
    getAppLockSettings: GetAppLockSettingsUseCase,
    private val getBiometricStatus: GetBiometricStatusUseCase,
    private val shouldLock: ShouldLockUseCase,
    private val getCurrentTimeMillis: GetCurrentTimeMillisUseCase,
    observeDeletedTransactions: ObserveDeletedTransactionsUseCase,
    private val restoreTransaction: RestoreTransactionUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    // A Channel, not state: each effect is handled once, and waits if nobody is collecting yet
    private val _effects = Channel<MainEffect>(Channel.BUFFERED)
    val effects: Flow<MainEffect> = _effects.receiveAsFlow()

    private var appLockSettings = AppLockSettings()

    // Kept in memory only: a new process always starts locked, whatever the timeout
    private var lastBackgroundedAt: Long? = null

    // True between the prompt opening and its result, so it is never opened twice
    private var isPromptInFlight = false

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
            getAppLockSettings().collect(::onAppLockSettingsChanged)
        }
        viewModelScope.launch {
            // Listening for as long as the app runs, so a delete on any screen is offered an Undo
            observeDeletedTransactions().collect { deleted ->
                _effects.send(MainEffect.ShowUndoDelete(deleted))
            }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.AppBackgrounded -> lastBackgroundedAt = getCurrentTimeMillis()
            MainEvent.AppForegrounded -> onAppForegrounded()
            MainEvent.UnlockClicked -> requestPrompt()
            MainEvent.PromptLaunched -> {
                isPromptInFlight = true
                _uiState.update { it.copy(isPromptRequested = false) }
            }
            is MainEvent.AuthFinished -> onAuthFinished(event.result)
            is MainEvent.UndoDelete -> undoDelete(event.transaction)
        }
    }

    private fun onAppLockSettingsChanged(settings: AppLockSettings) {
        appLockSettings = settings
        when {
            // The first read is the app starting, which locks whenever the lock is on
            !_uiState.value.isLockStateKnown -> {
                val locked = settings.enabled && canAuthenticate()
                _uiState.update {
                    it.copy(
                        isLockStateKnown = true,
                        isAppLockEnabled = settings.enabled,
                        isLocked = locked,
                        isPromptRequested = locked
                    )
                }
            }
            settings.enabled ->
                // Turned on in Settings, where the user has just authenticated: no need to lock now
                _uiState.update { it.copy(isAppLockEnabled = true) }
            else -> unlock(isAppLockEnabled = false)
        }
    }

    private fun onAppForegrounded() {
        val backgroundedAt = lastBackgroundedAt
        lastBackgroundedAt = null

        if (_uiState.value.isLocked) {
            // Coming back to the lock screen offers the prompt again, unless one is already open
            if (!isPromptInFlight) requestPrompt()
        } else if (shouldLock(appLockSettings, backgroundedAt) && canAuthenticate()) {
            _uiState.update { it.copy(isLocked = true, isPromptRequested = true, lockMessage = null) }
        }
    }

    private fun onAuthFinished(result: AuthResult) {
        isPromptInFlight = false
        if (!_uiState.value.isLocked) return

        val message = when (result) {
            // With no screen lock left nothing can authenticate, so staying locked would strand the user
            AuthResult.SUCCESS, AuthResult.NO_CREDENTIAL -> null
            AuthResult.CANCELED -> LockMessage.CANCELED
            AuthResult.LOCKOUT -> LockMessage.LOCKOUT
            AuthResult.LOCKOUT_PERMANENT -> LockMessage.LOCKOUT_PERMANENT
            AuthResult.ERROR -> LockMessage.ERROR
        }
        if (message == null) {
            unlock(isAppLockEnabled = appLockSettings.enabled)
        } else {
            _uiState.update { it.copy(lockMessage = message) }
        }
    }

    private fun requestPrompt() {
        _uiState.update { state ->
            if (state.isLocked) state.copy(isPromptRequested = true, lockMessage = null) else state
        }
    }

    private fun unlock(isAppLockEnabled: Boolean) {
        // Before Android 11 the screen lock check is a separate screen, which backgrounds the app
        // while it unlocks. Dropping that timestamp stops the app locking again straight away.
        lastBackgroundedAt = null
        _uiState.update {
            it.copy(
                isAppLockEnabled = isAppLockEnabled,
                isLocked = false,
                isPromptRequested = false,
                lockMessage = null
            )
        }
    }

    private fun undoDelete(transaction: Transaction) {
        viewModelScope.launch {
            restoreTransaction(transaction).onFailure {
                _effects.send(MainEffect.ShowRestoreFailed(it.toMessageRes()))
            }
        }
    }

    private fun canAuthenticate(): Boolean =
        getBiometricStatus() != BiometricStatus.NO_DEVICE_CREDENTIAL
}
