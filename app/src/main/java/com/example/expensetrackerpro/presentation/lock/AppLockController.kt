package com.example.expensetrackerpro.presentation.lock

import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetCurrentTimeMillisUseCase
import com.example.expensetrackerpro.domain.usecase.applock.ShouldLockUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Whether the app is locked, and when it asks for the authentication prompt. One belongs to
 * each [com.example.expensetrackerpro.presentation.main.MainViewModel], which tells it what
 * the app and the prompt are doing.
 */
class AppLockController @Inject constructor(
    private val getAppLockSettings: GetAppLockSettingsUseCase,
    private val getBiometricStatus: GetBiometricStatusUseCase,
    private val shouldLock: ShouldLockUseCase,
    private val getCurrentTimeMillis: GetCurrentTimeMillisUseCase
) {
    private val _state = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = _state.asStateFlow()

    private var appLockSettings = AppLockSettings()

    // Kept in memory only: a new process always starts locked, whatever the timeout
    private var lastBackgroundedAt: Long? = null

    // True between the prompt opening and its result, so it is never opened twice
    private var isPromptInFlight = false

    /** Follows the saved settings for as long as [scope] lives. Nothing is known until the first read. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            getAppLockSettings().collect(::onSettingsChanged)
        }
    }

    fun onAppBackgrounded() {
        lastBackgroundedAt = getCurrentTimeMillis()
    }

    fun onAppForegrounded() {
        val backgroundedAt = lastBackgroundedAt
        lastBackgroundedAt = null

        if (_state.value.isLocked) {
            // Coming back to the lock screen offers the prompt again, unless one is already open
            if (!isPromptInFlight) requestPrompt()
        } else if (shouldLock(appLockSettings, backgroundedAt) && canAuthenticate()) {
            _state.update { it.copy(isLocked = true, isPromptRequested = true, lockMessage = null) }
        }
    }

    fun onUnlockClicked() = requestPrompt()

    fun onPromptLaunched() {
        isPromptInFlight = true
        _state.update { it.copy(isPromptRequested = false) }
    }

    fun onAuthFinished(result: AuthResult) {
        isPromptInFlight = false
        if (!_state.value.isLocked) return

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
            _state.update { it.copy(lockMessage = message) }
        }
    }

    private fun onSettingsChanged(settings: AppLockSettings) {
        appLockSettings = settings
        when {
            // The first read is the app starting, which locks whenever the lock is on
            !_state.value.isLockStateKnown -> {
                val locked = settings.enabled && canAuthenticate()
                _state.update {
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
                _state.update { it.copy(isAppLockEnabled = true) }
            else -> unlock(isAppLockEnabled = false)
        }
    }

    private fun requestPrompt() {
        _state.update { state ->
            if (state.isLocked) state.copy(isPromptRequested = true, lockMessage = null) else state
        }
    }

    private fun unlock(isAppLockEnabled: Boolean) {
        // Before Android 11 the screen lock check is a separate screen, which backgrounds the app
        // while it unlocks. Dropping that timestamp stops the app locking again straight away.
        lastBackgroundedAt = null
        _state.update {
            it.copy(
                isAppLockEnabled = isAppLockEnabled,
                isLocked = false,
                isPromptRequested = false,
                lockMessage = null
            )
        }
    }

    private fun canAuthenticate(): Boolean =
        getBiometricStatus() != BiometricStatus.NO_DEVICE_CREDENTIAL
}
