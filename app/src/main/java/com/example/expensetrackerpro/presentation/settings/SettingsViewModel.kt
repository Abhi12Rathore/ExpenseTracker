package com.example.expensetrackerpro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.SetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoryCountsUseCase
import com.example.expensetrackerpro.domain.usecase.data.DeleteAllDataUseCase
import com.example.expensetrackerpro.domain.usecase.profile.GetUserProfileUseCase
import com.example.expensetrackerpro.domain.usecase.profile.SetUserNameUseCase
import com.example.expensetrackerpro.domain.usecase.sample.LoadSampleDataUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.theme.SetThemeModeUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsMessage
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
    getUserProfile: GetUserProfileUseCase,
    private val setUserName: SetUserNameUseCase,
    getCategoryCounts: GetCategoryCountsUseCase,
    private val loadSampleData: LoadSampleDataUseCase,
    private val deleteAllData: DeleteAllDataUseCase,
    private val regionalSettings: RegionalSettingsController,
    private val buildInfo: BuildInfo
) : ViewModel() {

    // Dialog and prompt state; everything that is stored is merged in below
    private val screenState = MutableStateFlow(
        SettingsUiState(
            biometricStatus = getBiometricStatus(),
            versionName = buildInfo.versionName,
            isDeveloperSectionVisible = buildInfo.isDebug
        )
    )

    val uiState: StateFlow<SettingsUiState> =
        combine(
            getThemeMode(),
            getAppLockSettings(),
            getUserProfile(),
            getCategoryCounts(),
            // Language and currency are merged in first: combine takes five flows at most
            combine(screenState, regionalSettings.state) { state, regional ->
                state.copy(
                    language = regional.language,
                    languageChange = regional.languageChange,
                    currency = regional.currency,
                    currencyChange = regional.currencyChange
                )
            }
        ) { themeMode, appLock, profile, categoryCounts, state ->
            state.copy(
                themeMode = themeMode,
                appLock = appLock,
                profile = profile,
                categoryCounts = categoryCounts
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), screenState.value)

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ProfileClicked ->
                // Opens on the stored name, ready to be changed
                screenState.update { it.copy(profileNameDraft = uiState.value.profile.name) }
            is SettingsEvent.ProfileNameChanged -> screenState.update {
                // Left alone while the dialog is closed; longer text, such as a paste, is cut at the limit
                it.copy(profileNameDraft = it.profileNameDraft?.let { _ -> event.name.take(NAME_MAX_LENGTH) })
            }
            SettingsEvent.ProfileNameSaved -> saveProfileName()
            SettingsEvent.ProfileDialogDismissed ->
                screenState.update { it.copy(profileNameDraft = null) }
            is SettingsEvent.ThemeModeSelected ->
                viewModelScope.launch { setThemeMode(event.mode) }
            SettingsEvent.LanguageUndoClicked -> regionalSettings.undoLanguage()
            SettingsEvent.LanguageChangeShown -> regionalSettings.onLanguageChangeShown()
            SettingsEvent.CurrencyUndoClicked -> regionalSettings.undoCurrency(viewModelScope)
            SettingsEvent.CurrencyChangeShown -> regionalSettings.onCurrencyChangeShown()
            SettingsEvent.ScreenResumed -> {
                screenState.update { it.copy(biometricStatus = getBiometricStatus()) }
                regionalSettings.refresh()
            }
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
            SettingsEvent.DeleteAllDataClicked -> screenState.update {
                if (it.isDataActionRunning) it else it.copy(deleteAllConfirmation = "")
            }
            is SettingsEvent.DeleteAllDataTextChanged -> screenState.update {
                it.copy(deleteAllConfirmation = it.deleteAllConfirmation?.let { _ -> event.text })
            }
            SettingsEvent.DeleteAllDataConfirmed -> deleteEverything()
            SettingsEvent.DeleteAllDataDismissed ->
                screenState.update { it.copy(deleteAllConfirmation = null) }
            SettingsEvent.LoadSampleDataClicked -> requestSampleData()
            SettingsEvent.LoadSampleDataConfirmed -> loadSamples()
            SettingsEvent.LoadSampleDataDismissed ->
                screenState.update { it.copy(isLoadSampleDialogOpen = false) }
            SettingsEvent.MessageShown ->
                screenState.update { it.copy(message = null) }
        }
    }

    private fun saveProfileName() {
        val name = screenState.value.profileNameDraft ?: return

        screenState.update { it.copy(profileNameDraft = null) }
        viewModelScope.launch { setUserName(name) }
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

    private fun deleteEverything() {
        val state = screenState.value
        if (!state.isDeleteAllConfirmed || state.isDataActionRunning) return

        screenState.update { it.copy(deleteAllConfirmation = null, isDataActionRunning = true) }
        viewModelScope.launch {
            val message = when (val result = deleteAllData()) {
                is Outcome.Success -> SettingsMessage.AllDataDeleted
                is Outcome.Failure -> SettingsMessage.Failed(result.error.toMessageRes())
            }
            screenState.update { it.copy(isDataActionRunning = false, message = message) }
        }
    }

    private fun requestSampleData() {
        // Release builds never show the section; this keeps the action out of reach there too
        if (!buildInfo.isDebug || screenState.value.isDataActionRunning) return
        screenState.update { it.copy(isLoadSampleDialogOpen = true) }
    }

    private fun loadSamples() {
        val state = screenState.value
        if (!buildInfo.isDebug || !state.isLoadSampleDialogOpen || state.isDataActionRunning) return

        screenState.update { it.copy(isLoadSampleDialogOpen = false, isDataActionRunning = true) }
        viewModelScope.launch {
            val message = when (val result = loadSampleData()) {
                is Outcome.Success -> SettingsMessage.SampleDataLoaded(result.value)
                is Outcome.Failure -> SettingsMessage.Failed(result.error.toMessageRes())
            }
            screenState.update { it.copy(isDataActionRunning = false, message = message) }
        }
    }

    private fun updateAppLock(change: (AppLockSettings) -> AppLockSettings) {
        viewModelScope.launch { setAppLockSettings(change(getAppLockSettings().first())) }
    }

    private companion object {
        const val NAME_MAX_LENGTH = SetUserNameUseCase.NAME_MAX_LENGTH
    }
}
