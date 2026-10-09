package com.example.expensetrackerpro.presentation.settings

import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.usecase.sample.ClearAllDataUseCase
import com.example.expensetrackerpro.domain.usecase.sample.LoadSampleDataUseCase
import com.example.expensetrackerpro.presentation.settings.uistate.DeveloperAction
import com.example.expensetrackerpro.presentation.settings.uistate.DeveloperMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.SetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.theme.SetThemeModeUseCase
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getAppLockSettings: GetAppLockSettingsUseCase,
    private val setAppLockSettings: SetAppLockSettingsUseCase,
    private val getBiometricStatus: GetBiometricStatusUseCase,
    private val loadSampleData: LoadSampleDataUseCase,
    private val clearAllData: ClearAllDataUseCase,
    private val buildInfo: BuildInfo
) : ViewModel() {

    // Dialog and prompt state; the saved theme and app lock settings are merged in below
    private val screenState = MutableStateFlow(
        SettingsUiState(
            biometricStatus = getBiometricStatus(),
            isDeveloperSectionVisible = buildInfo.isDebug
        )
    )

    val uiState: StateFlow<SettingsUiState> =
        combine(getThemeMode(), getAppLockSettings(), screenState) { themeMode, appLock, state ->
            state.copy(themeMode = themeMode, appLock = appLock)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), screenState.value)

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ThemeClicked ->
                screenState.update { it.copy(isThemeDialogOpen = true) }
            SettingsEvent.ThemeDialogDismissed -> closeThemeDialog()
            is SettingsEvent.ThemeModeSelected -> selectTheme(event.mode)
            SettingsEvent.ScreenResumed ->
                screenState.update { it.copy(biometricStatus = getBiometricStatus()) }
            is SettingsEvent.AppLockToggled -> toggleAppLock(event.enabled)
            SettingsEvent.AppLockPromptLaunched ->
                screenState.update { it.copy(isAuthRequested = false) }
            is SettingsEvent.AppLockAuthFinished -> finishEnablingAppLock(event.result)
            SettingsEvent.AppLockErrorShown ->
                screenState.update { it.copy(appLockError = null) }
            SettingsEvent.LockAfterClicked ->
                screenState.update { it.copy(isLockTimeoutDialogOpen = true) }
            SettingsEvent.LockTimeoutDialogDismissed -> closeLockTimeoutDialog()
            is SettingsEvent.LockTimeoutSelected -> selectLockTimeout(event.timeout)
            is SettingsEvent.DeveloperActionClicked -> requestDeveloperAction(event.action)
            SettingsEvent.DeveloperActionConfirmed -> runDeveloperAction()
            SettingsEvent.DeveloperActionDismissed ->
                screenState.update { it.copy(pendingDeveloperAction = null) }
            SettingsEvent.DeveloperMessageShown ->
                screenState.update { it.copy(developerMessage = null) }
        }
    }

    private fun selectTheme(mode: ThemeMode) {
        closeThemeDialog()
        viewModelScope.launch { setThemeMode(mode) }
    }

    private fun closeThemeDialog() {
        screenState.update { it.copy(isThemeDialogOpen = false) }
    }

    private fun toggleAppLock(enabled: Boolean) {
        if (!enabled) {
            updateAppLock { it.copy(enabled = false) }
            return
        }
        // Re-read here: the screen lock may have been removed since the switch was drawn
        val status = getBiometricStatus()
        val state = screenState.updateAndGet { it.copy(biometricStatus = status) }
        if (!state.isAppLockAvailable) return
        // Turning the lock on has to be confirmed by the person it will lock out
        screenState.update { it.copy(isAuthRequested = true, appLockError = null) }
    }

    private fun finishEnablingAppLock(result: AuthResult) {
        when (result) {
            AuthResult.SUCCESS -> updateAppLock { it.copy(enabled = true) }
            // The user backed out themselves, so there is nothing to explain
            AuthResult.CANCELED -> Unit
            else -> screenState.update {
                it.copy(appLockError = result, biometricStatus = getBiometricStatus())
            }
        }
    }

    private fun selectLockTimeout(timeout: LockTimeout) {
        closeLockTimeoutDialog()
        updateAppLock { it.copy(timeout = timeout) }
    }

    private fun closeLockTimeoutDialog() {
        screenState.update { it.copy(isLockTimeoutDialogOpen = false) }
    }

    private fun requestDeveloperAction(action: DeveloperAction) {
        // Release builds never show the section; this keeps the actions out of reach there too
        if (!buildInfo.isDebug || screenState.value.isDeveloperActionRunning) return
        screenState.update { it.copy(pendingDeveloperAction = action) }
    }

    private fun runDeveloperAction() {
        val action = screenState.value.pendingDeveloperAction
        if (!buildInfo.isDebug || action == null || screenState.value.isDeveloperActionRunning) return

        screenState.update { it.copy(pendingDeveloperAction = null, isDeveloperActionRunning = true) }
        viewModelScope.launch {
            val message = when (action) {
                DeveloperAction.LOAD_SAMPLE_DATA ->
                    loadSampleData()?.let { DeveloperMessage.SampleDataLoaded(it) } ?: DeveloperMessage.Failed
                DeveloperAction.CLEAR_ALL_DATA ->
                    if (clearAllData()) DeveloperMessage.AllDataCleared else DeveloperMessage.Failed
            }
            screenState.update { it.copy(isDeveloperActionRunning = false, developerMessage = message) }
        }
    }

    private fun updateAppLock(change: (AppLockSettings) -> AppLockSettings) {
        viewModelScope.launch { setAppLockSettings(change(getAppLockSettings().first())) }
    }
}
