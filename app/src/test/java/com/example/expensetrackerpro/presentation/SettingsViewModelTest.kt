package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.usecase.sample.ClearAllDataUseCase
import com.example.expensetrackerpro.domain.usecase.sample.LoadSampleDataUseCase
import com.example.expensetrackerpro.fakes.FakeSampleDataRepository
import com.example.expensetrackerpro.presentation.settings.uistate.DeveloperAction
import com.example.expensetrackerpro.presentation.settings.uistate.DeveloperMessage
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.usecase.applock.GetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.applock.GetBiometricStatusUseCase
import com.example.expensetrackerpro.domain.usecase.applock.SetAppLockSettingsUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.theme.SetThemeModeUseCase
import com.example.expensetrackerpro.fakes.FakeAppLockRepository
import com.example.expensetrackerpro.fakes.FakeBiometricAvailability
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.presentation.settings.SettingsViewModel
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appLockRepository = FakeAppLockRepository()
    private val biometricAvailability = FakeBiometricAvailability()

    private val sampleData = FakeSampleDataRepository()

    private fun viewModel(
        repository: FakeThemePreferenceRepository = FakeThemePreferenceRepository(),
        isDebug: Boolean = true
    ) = SettingsViewModel(
        GetThemeModeUseCase(repository),
        SetThemeModeUseCase(repository),
        GetAppLockSettingsUseCase(appLockRepository),
        SetAppLockSettingsUseCase(appLockRepository),
        GetBiometricStatusUseCase(biometricAvailability),
        LoadSampleDataUseCase(sampleData),
        ClearAllDataUseCase(sampleData),
        BuildInfo(isDebug = isDebug)
    )

    @Test
    fun showsTheSavedThemeMode() = runTest {
        val viewModel = viewModel(FakeThemePreferenceRepository(ThemeMode.DARK))
        subscribe(viewModel.uiState)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
    }

    @Test
    fun themeClicked_opensTheDialog() = runTest {
        val viewModel = viewModel(FakeThemePreferenceRepository())
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.ThemeClicked)

        assertTrue(viewModel.uiState.value.isThemeDialogOpen)
    }

    @Test
    fun themeDialogDismissed_closesTheDialogWithoutChangingTheMode() = runTest {
        val repository = FakeThemePreferenceRepository(ThemeMode.LIGHT)
        val viewModel = viewModel(repository)
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.ThemeClicked)

        viewModel.onEvent(SettingsEvent.ThemeDialogDismissed)

        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
        assertEquals(ThemeMode.LIGHT, repository.current)
    }

    @Test
    fun themeModeSelected_savesTheModeAndClosesTheDialog() = runTest {
        val repository = FakeThemePreferenceRepository()
        val viewModel = viewModel(repository)
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.ThemeClicked)

        viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, repository.current)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.isThemeDialogOpen)
    }

    @Test
    fun showsTheSavedAppLockSettings() = runTest {
        appLockRepository.setSettings(AppLockSettings(enabled = true, timeout = LockTimeout.MINUTES_5))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(
            AppLockSettings(enabled = true, timeout = LockTimeout.MINUTES_5),
            viewModel.uiState.value.appLock
        )
        assertTrue(viewModel.uiState.value.isAppLockAvailable)
    }

    @Test
    fun appLockToggledOn_asksForAuthenticationBeforeSavingAnything() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.AppLockToggled(true))

        assertTrue(viewModel.uiState.value.isAuthRequested)
        assertFalse(viewModel.uiState.value.appLock.enabled)
        assertFalse(appLockRepository.current.enabled)
    }

    @Test
    fun appLockPromptLaunched_consumesTheRequest() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.AppLockToggled(true))

        viewModel.onEvent(SettingsEvent.AppLockPromptLaunched)

        assertFalse(viewModel.uiState.value.isAuthRequested)
    }

    @Test
    fun enableAuthSucceeded_turnsTheLockOnAndKeepsTheTimeout() = runTest {
        appLockRepository.setSettings(AppLockSettings(enabled = false, timeout = LockTimeout.MINUTE_1))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.AppLockToggled(true))
        viewModel.onEvent(SettingsEvent.AppLockPromptLaunched)

        viewModel.onEvent(SettingsEvent.AppLockAuthFinished(AuthResult.SUCCESS))

        val expected = AppLockSettings(enabled = true, timeout = LockTimeout.MINUTE_1)
        assertEquals(expected, appLockRepository.current)
        assertEquals(expected, viewModel.uiState.value.appLock)
        assertNull(viewModel.uiState.value.appLockError)
    }

    @Test
    fun enableAuthFailed_leavesTheLockOffAndReportsWhy() = runTest {
        listOf(
            AuthResult.LOCKOUT,
            AuthResult.LOCKOUT_PERMANENT,
            AuthResult.NO_CREDENTIAL,
            AuthResult.ERROR
        ).forEach { result ->
            val viewModel = viewModel()
            subscribe(viewModel.uiState)
            viewModel.onEvent(SettingsEvent.AppLockToggled(true))
            viewModel.onEvent(SettingsEvent.AppLockPromptLaunched)

            viewModel.onEvent(SettingsEvent.AppLockAuthFinished(result))

            assertFalse(appLockRepository.current.enabled)
            assertFalse(viewModel.uiState.value.appLock.enabled)
            assertEquals(result, viewModel.uiState.value.appLockError)
        }
    }

    @Test
    fun enableAuthCancelled_leavesTheLockOffWithoutAnError() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.AppLockToggled(true))
        viewModel.onEvent(SettingsEvent.AppLockPromptLaunched)

        viewModel.onEvent(SettingsEvent.AppLockAuthFinished(AuthResult.CANCELED))

        assertFalse(appLockRepository.current.enabled)
        assertNull(viewModel.uiState.value.appLockError)
    }

    @Test
    fun appLockErrorShown_clearsTheError() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.AppLockToggled(true))
        viewModel.onEvent(SettingsEvent.AppLockAuthFinished(AuthResult.ERROR))

        viewModel.onEvent(SettingsEvent.AppLockErrorShown)

        assertNull(viewModel.uiState.value.appLockError)
    }

    @Test
    fun noScreenLock_appLockIsUnavailableAndTogglingDoesNothing() = runTest {
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.AppLockToggled(true))

        assertFalse(viewModel.uiState.value.isAppLockAvailable)
        assertFalse(viewModel.uiState.value.isAuthRequested)
        assertFalse(appLockRepository.current.enabled)
    }

    @Test
    fun screenLockRemovedAfterTheScreenOpened_togglingOnNoticesAndDoesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL

        viewModel.onEvent(SettingsEvent.AppLockToggled(true))

        assertFalse(viewModel.uiState.value.isAuthRequested)
        assertFalse(viewModel.uiState.value.isAppLockAvailable)
    }

    @Test
    fun noBiometricsButAScreenLock_appLockIsStillAvailable() = runTest {
        listOf(BiometricStatus.NO_HARDWARE, BiometricStatus.NONE_ENROLLED).forEach { status ->
            biometricAvailability.current = status
            val viewModel = viewModel()
            subscribe(viewModel.uiState)

            viewModel.onEvent(SettingsEvent.AppLockToggled(true))

            assertTrue(viewModel.uiState.value.isAppLockAvailable)
            assertTrue(viewModel.uiState.value.isAuthRequested)
        }
    }

    @Test
    fun screenResumed_picksUpAScreenLockSetInSystemSettings() = runTest {
        biometricAvailability.current = BiometricStatus.NO_DEVICE_CREDENTIAL
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        biometricAvailability.current = BiometricStatus.AVAILABLE

        viewModel.onEvent(SettingsEvent.ScreenResumed)

        assertEquals(BiometricStatus.AVAILABLE, viewModel.uiState.value.biometricStatus)
        assertTrue(viewModel.uiState.value.isAppLockAvailable)
    }

    @Test
    fun appLockToggledOff_turnsTheLockOffWithoutAuthentication() = runTest {
        appLockRepository.setSettings(AppLockSettings(enabled = true, timeout = LockTimeout.MINUTES_5))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.AppLockToggled(false))

        assertEquals(
            AppLockSettings(enabled = false, timeout = LockTimeout.MINUTES_5),
            appLockRepository.current
        )
        assertFalse(viewModel.uiState.value.isAuthRequested)
    }

    @Test
    fun lockAfterClicked_opensTheDialog_andDismissClosesItUnchanged() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.LockAfterClicked)
        assertTrue(viewModel.uiState.value.isLockTimeoutDialogOpen)

        viewModel.onEvent(SettingsEvent.LockTimeoutDialogDismissed)

        assertFalse(viewModel.uiState.value.isLockTimeoutDialogOpen)
        assertEquals(LockTimeout.SECONDS_30, appLockRepository.current.timeout)
    }

    @Test
    fun lockTimeoutSelected_savesTheTimeoutAndClosesTheDialog() = runTest {
        appLockRepository.setSettings(AppLockSettings(enabled = true))
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.LockAfterClicked)

        viewModel.onEvent(SettingsEvent.LockTimeoutSelected(LockTimeout.IMMEDIATELY))

        assertEquals(
            AppLockSettings(enabled = true, timeout = LockTimeout.IMMEDIATELY),
            appLockRepository.current
        )
        assertEquals(LockTimeout.IMMEDIATELY, viewModel.uiState.value.appLock.timeout)
        assertFalse(viewModel.uiState.value.isLockTimeoutDialogOpen)
    }

    // --- Developer section

    private fun SettingsViewModel.click(action: DeveloperAction) =
        onEvent(SettingsEvent.DeveloperActionClicked(action))

    @Test
    fun developerSection_isShownInDebugBuildsOnly() = runTest {
        val debug = viewModel(isDebug = true)
        val release = viewModel(isDebug = false)
        subscribe(debug.uiState)
        subscribe(release.uiState)

        assertTrue(debug.uiState.value.isDeveloperSectionVisible)
        assertFalse(release.uiState.value.isDeveloperSectionVisible)
    }

    @Test
    fun loadSampleData_asksFirst_andOnlyLoadsOnceConfirmed() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.click(DeveloperAction.LOAD_SAMPLE_DATA)

        assertEquals(DeveloperAction.LOAD_SAMPLE_DATA, viewModel.uiState.value.pendingDeveloperAction)
        assertEquals(0, sampleData.loadCalls)

        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)
        val state = viewModel.uiState.value

        assertEquals(1, sampleData.loadCalls)
        assertEquals(0, sampleData.clearCalls)
        assertNull(state.pendingDeveloperAction)
        assertFalse(state.isDeveloperActionRunning)
        assertEquals(DeveloperMessage.SampleDataLoaded(240), state.developerMessage)

        viewModel.onEvent(SettingsEvent.DeveloperMessageShown)
        assertNull(viewModel.uiState.value.developerMessage)
    }

    @Test
    fun loadSampleData_dismissed_changesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.click(DeveloperAction.LOAD_SAMPLE_DATA)
        viewModel.onEvent(SettingsEvent.DeveloperActionDismissed)
        // Nothing is waiting to be confirmed any more
        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)

        assertNull(viewModel.uiState.value.pendingDeveloperAction)
        assertNull(viewModel.uiState.value.developerMessage)
        assertEquals(0, sampleData.loadCalls)
    }

    @Test
    fun clearAllData_asksFirst_thenClears() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.click(DeveloperAction.CLEAR_ALL_DATA)
        assertEquals(0, sampleData.clearCalls)

        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)

        assertEquals(1, sampleData.clearCalls)
        assertEquals(0, sampleData.loadCalls)
        assertEquals(DeveloperMessage.AllDataCleared, viewModel.uiState.value.developerMessage)
    }

    @Test
    fun developerAction_thatFails_saysSo() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        sampleData.fail = true

        viewModel.click(DeveloperAction.LOAD_SAMPLE_DATA)
        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)
        assertEquals(DeveloperMessage.Failed, viewModel.uiState.value.developerMessage)

        viewModel.click(DeveloperAction.CLEAR_ALL_DATA)
        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)
        assertEquals(DeveloperMessage.Failed, viewModel.uiState.value.developerMessage)
        assertFalse(viewModel.uiState.value.isDeveloperActionRunning)
    }

    @Test
    fun inAReleaseBuild_developerActionsDoNothing() = runTest {
        val viewModel = viewModel(isDebug = false)
        subscribe(viewModel.uiState)

        viewModel.click(DeveloperAction.LOAD_SAMPLE_DATA)
        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)
        viewModel.click(DeveloperAction.CLEAR_ALL_DATA)
        viewModel.onEvent(SettingsEvent.DeveloperActionConfirmed)

        assertNull(viewModel.uiState.value.pendingDeveloperAction)
        assertNull(viewModel.uiState.value.developerMessage)
        assertEquals(0, sampleData.loadCalls)
        assertEquals(0, sampleData.clearCalls)
    }
}
