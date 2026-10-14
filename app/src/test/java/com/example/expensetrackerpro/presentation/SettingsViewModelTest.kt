package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.usecase.currency.ChangeCurrencyUseCase
import com.example.expensetrackerpro.domain.models.CurrencyChange
import com.example.expensetrackerpro.domain.currency.AppCurrency
import com.example.expensetrackerpro.domain.usecase.currency.ForgetCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.currency.UndoCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.currency.ObserveCurrencyChangeUseCase
import com.example.expensetrackerpro.domain.usecase.currency.ObserveCurrencyUseCase
import com.example.expensetrackerpro.fakes.FakeCurrencyRepository
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.presentation.settings.RegionalSettingsController
import com.example.expensetrackerpro.domain.usecase.language.SetAppLanguageUseCase
import com.example.expensetrackerpro.domain.usecase.language.ResolveAppLocaleUseCase
import com.example.expensetrackerpro.domain.usecase.language.ForgetLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.UndoLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.ObserveLanguageChangeUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguageOptionsUseCase
import com.example.expensetrackerpro.fakes.FakeDeviceLanguages
import com.example.expensetrackerpro.fakes.FakeAppLanguageRepository
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.models.AppLockSettings
import com.example.expensetrackerpro.domain.models.AuthResult
import com.example.expensetrackerpro.domain.models.BiometricStatus
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.CategoryCounts
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.models.TransactionType
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
import com.example.expensetrackerpro.fakes.FakeAppDataRepository
import com.example.expensetrackerpro.fakes.FakeAppLockRepository
import com.example.expensetrackerpro.fakes.FakeBiometricAvailability
import com.example.expensetrackerpro.fakes.FakeCategoryRepository
import com.example.expensetrackerpro.fakes.FakeSampleDataRepository
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.fakes.FakeUserProfileRepository
import com.example.expensetrackerpro.fakes.category
import com.example.expensetrackerpro.presentation.settings.SettingsViewModel
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsMessage
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
    private val appData = FakeAppDataRepository()
    private val profile = FakeUserProfileRepository()

    // Two built-in categories and one the user added
    private val categories = FakeCategoryRepository(
        listOf(
            category(id = 1, name = "Food").copy(isCustom = false),
            category(id = 2, name = "Salary", type = TransactionType.INCOME).copy(isCustom = false),
            category(id = 3, name = "Pets")
        )
    )

    private val languages = FakeAppLanguageRepository()
    private val currencies = FakeCurrencyRepository()
    private val deviceLanguages = FakeDeviceLanguages()

    private fun viewModel(
        repository: FakeThemePreferenceRepository = FakeThemePreferenceRepository(),
        isDebug: Boolean = true
    ) = SettingsViewModel(
        GetThemeModeUseCase(repository),
        SetThemeModeUseCase(repository),
        GetAppLockSettingsUseCase(appLockRepository),
        SetAppLockSettingsUseCase(appLockRepository),
        GetBiometricStatusUseCase(biometricAvailability),
        GetUserProfileUseCase(profile),
        SetUserNameUseCase(profile),
        GetCategoryCountsUseCase(categories),
        LoadSampleDataUseCase(sampleData),
        DeleteAllDataUseCase(appData),
        RegionalSettingsController(
            GetLanguageOptionsUseCase(languages, deviceLanguages, ResolveAppLocaleUseCase()),
            ObserveLanguageChangeUseCase(languages),
            UndoLanguageChangeUseCase(languages),
            ForgetLanguageChangeUseCase(languages),
            ObserveCurrencyUseCase(currencies),
            ObserveCurrencyChangeUseCase(currencies),
            UndoCurrencyChangeUseCase(currencies),
            ForgetCurrencyChangeUseCase(currencies)
        ),
        BuildInfo(isDebug = isDebug, versionName = "1.0")
    )

    private val hindi = LanguageChoice.Specific(AppLanguage.HINDI)

    private fun setAppLanguage(choice: LanguageChoice) =
        SetAppLanguageUseCase(languages, deviceLanguages, ResolveAppLocaleUseCase())(choice)

    @Test
    fun showsTheLanguageTheAppIsIn_andReadsItAgainWhenTheScreenResumes() = runTest {
        languages.stored = hindi
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(hindi, viewModel.uiState.value.language)

        // Changed on the Language screen, or in the phone's settings for the app
        languages.stored = LanguageChoice.SystemDefault
        viewModel.onEvent(SettingsEvent.ScreenResumed)

        assertEquals(LanguageChoice.SystemDefault, viewModel.uiState.value.language)
    }

    @Test
    fun aLanguageChange_isOfferedAnUndo_whichPutsThePreviousLanguageBack() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        assertNull(viewModel.uiState.value.languageChange)

        setAppLanguage(hindi)

        assertEquals(
            LanguageChange(previous = LanguageChoice.SystemDefault, language = AppLanguage.HINDI),
            viewModel.uiState.value.languageChange
        )

        viewModel.onEvent(SettingsEvent.LanguageUndoClicked)

        assertEquals(LanguageChoice.SystemDefault, languages.stored)
        // Going back is not itself offered an Undo
        assertNull(viewModel.uiState.value.languageChange)
    }

    @Test
    fun aLanguageChangeThatWasShown_staysAndIsNoLongerOffered() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        setAppLanguage(hindi)

        viewModel.onEvent(SettingsEvent.LanguageChangeShown)

        assertEquals(hindi, languages.stored)
        assertNull(viewModel.uiState.value.languageChange)

        // With nothing left to undo, a late Undo changes nothing
        viewModel.onEvent(SettingsEvent.LanguageUndoClicked)
        assertEquals(hindi, languages.stored)
    }

    @Test
    fun showsTheCurrency_andFollowsAChangeMadeElsewhere() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        assertEquals(AppCurrency.INR, viewModel.uiState.value.currency)

        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)

        assertEquals(AppCurrency.USD, viewModel.uiState.value.currency)
    }

    @Test
    fun aCurrencyChange_isOfferedAnUndo_whichPutsThePreviousCurrencyBack() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)

        assertEquals(CurrencyChange(AppCurrency.INR, AppCurrency.USD), viewModel.uiState.value.currencyChange)

        viewModel.onEvent(SettingsEvent.CurrencyUndoClicked)

        assertEquals(AppCurrency.INR, viewModel.uiState.value.currency)
        assertNull(viewModel.uiState.value.currencyChange)
    }

    @Test
    fun aCurrencyChangeThatWasShown_staysAndIsNoLongerOffered() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        ChangeCurrencyUseCase(currencies)(AppCurrency.USD)

        viewModel.onEvent(SettingsEvent.CurrencyChangeShown)
        viewModel.onEvent(SettingsEvent.CurrencyUndoClicked)

        assertEquals(AppCurrency.USD, viewModel.uiState.value.currency)
        assertNull(viewModel.uiState.value.currencyChange)
    }

    @Test
    fun showsTheSavedThemeMode() = runTest {
        val viewModel = viewModel(FakeThemePreferenceRepository(ThemeMode.DARK))
        subscribe(viewModel.uiState)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun themeModeSelected_savesTheModeStraightAway() = runTest {
        val repository = FakeThemePreferenceRepository()
        val viewModel = viewModel(repository)
        subscribe(viewModel.uiState)

        ThemeMode.entries.forEach { mode ->
            viewModel.onEvent(SettingsEvent.ThemeModeSelected(mode))

            assertEquals(mode, repository.current)
            assertEquals(mode, viewModel.uiState.value.themeMode)
        }
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

    // --- Profile

    @Test
    fun showsTheSavedName() = runTest {
        profile.setName("Riya")
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals("Riya", viewModel.uiState.value.profile.name)
        assertEquals('R', viewModel.uiState.value.profile.initial)
        assertNull(viewModel.uiState.value.profileNameDraft)
    }

    @Test
    fun profileClicked_opensTheDialogOnTheSavedName_andSavingStoresTheEdit() = runTest {
        profile.setName("Riya")
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.ProfileClicked)
        assertEquals("Riya", viewModel.uiState.value.profileNameDraft)

        viewModel.onEvent(SettingsEvent.ProfileNameChanged("  Riya Sen "))
        // Only a draft so far
        assertEquals("Riya", profile.current.name)

        viewModel.onEvent(SettingsEvent.ProfileNameSaved)
        val state = viewModel.uiState.value

        assertEquals("Riya Sen", profile.current.name)
        assertEquals("Riya Sen", state.profile.name)
        assertNull(state.profileNameDraft)
    }

    @Test
    fun profileDialogDismissed_keepsTheSavedName() = runTest {
        profile.setName("Riya")
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.ProfileClicked)
        viewModel.onEvent(SettingsEvent.ProfileNameChanged("Someone else"))

        viewModel.onEvent(SettingsEvent.ProfileDialogDismissed)
        // Nothing is waiting to be saved any more
        viewModel.onEvent(SettingsEvent.ProfileNameSaved)

        assertNull(viewModel.uiState.value.profileNameDraft)
        assertEquals("Riya", profile.current.name)
    }

    @Test
    fun aNameTypedPastTheLimit_isCut_andAnEmptiedOneClearsTheName() = runTest {
        profile.setName("Riya")
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.ProfileClicked)

        viewModel.onEvent(SettingsEvent.ProfileNameChanged("a".repeat(45)))
        assertEquals("a".repeat(30), viewModel.uiState.value.profileNameDraft)

        viewModel.onEvent(SettingsEvent.ProfileNameChanged(""))
        viewModel.onEvent(SettingsEvent.ProfileNameSaved)

        assertEquals("", profile.current.name)
        assertNull(viewModel.uiState.value.profile.initial)
    }

    // --- Data

    @Test
    fun showsHowManyCategoriesThereAre_andHowManyAreCustom() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals(CategoryCounts(total = 3, custom = 1), viewModel.uiState.value.categoryCounts)

        categories.insertCategory(category(id = 0, name = "Gym"))

        assertEquals(CategoryCounts(total = 4, custom = 2), viewModel.uiState.value.categoryCounts)
    }

    @Test
    fun showsTheBuildsVersion() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        assertEquals("1.0", viewModel.uiState.value.versionName)
    }

    // --- Danger zone

    private fun SettingsViewModel.typeToConfirm(text: String) =
        onEvent(SettingsEvent.DeleteAllDataTextChanged(text))

    @Test
    fun deleteAllData_opensADialogThatDeletesNothingYet() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        val state = viewModel.uiState.value

        assertEquals("", state.deleteAllConfirmation)
        assertFalse(state.isDeleteAllConfirmed)
        assertEquals(0, appData.deleteCalls)
    }

    @Test
    fun deleteAllData_isOnlyConfirmedByTheExactWord() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)

        listOf("", "DELET", "delete", "Delete", "DELETE!", "DELETE ALL").forEach { text ->
            viewModel.typeToConfirm(text)
            assertFalse(text, viewModel.uiState.value.isDeleteAllConfirmed)

            // Confirming anyway does nothing
            viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
            assertEquals(text, viewModel.uiState.value.deleteAllConfirmation)
        }
        assertEquals(0, appData.deleteCalls)

        // The space a keyboard adds after a word does not count against it
        viewModel.typeToConfirm("DELETE ")
        assertTrue(viewModel.uiState.value.isDeleteAllConfirmed)
    }

    @Test
    fun deleteAllData_confirmedWithTheWord_deletesOnce_andSaysSo() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        viewModel.typeToConfirm("DELETE")

        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
        // The dialog is gone, so a second tap has nothing to confirm
        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
        val state = viewModel.uiState.value

        assertEquals(1, appData.deleteCalls)
        assertEquals(0, sampleData.loadCalls)
        assertNull(state.deleteAllConfirmation)
        assertFalse(state.isDataActionRunning)
        assertEquals(SettingsMessage.AllDataDeleted, state.message)

        viewModel.onEvent(SettingsEvent.MessageShown)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun deleteAllData_dismissed_forgetsWhatWasTyped() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        viewModel.typeToConfirm("DELETE")

        viewModel.onEvent(SettingsEvent.DeleteAllDataDismissed)
        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
        assertEquals(0, appData.deleteCalls)

        // Opening it again starts from nothing
        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        assertEquals("", viewModel.uiState.value.deleteAllConfirmation)
        assertFalse(viewModel.uiState.value.isDeleteAllConfirmed)
    }

    @Test
    fun deleteAllData_textTypedWhileTheDialogIsClosed_isIgnored() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.typeToConfirm("DELETE")
        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)

        assertNull(viewModel.uiState.value.deleteAllConfirmation)
        assertEquals(0, appData.deleteCalls)
    }

    @Test
    fun deleteAllData_thatFails_saysSo() = runTest {
        appData.fail = true
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        viewModel.typeToConfirm("DELETE")

        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)

        assertEquals(
            SettingsMessage.Failed(R.string.error_storage_failure),
            viewModel.uiState.value.message
        )
        assertFalse(viewModel.uiState.value.isDataActionRunning)
    }

    @Test
    fun deleteAllData_isThereInReleaseBuildsToo() = runTest {
        val viewModel = viewModel(isDebug = false)
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        viewModel.typeToConfirm("DELETE")
        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)

        assertEquals(1, appData.deleteCalls)
        assertEquals(SettingsMessage.AllDataDeleted, viewModel.uiState.value.message)
    }

    // --- Developer section

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

        viewModel.onEvent(SettingsEvent.LoadSampleDataClicked)

        assertTrue(viewModel.uiState.value.isLoadSampleDialogOpen)
        assertEquals(0, sampleData.loadCalls)

        viewModel.onEvent(SettingsEvent.LoadSampleDataConfirmed)
        val state = viewModel.uiState.value

        assertEquals(1, sampleData.loadCalls)
        assertEquals(0, appData.deleteCalls)
        assertFalse(state.isLoadSampleDialogOpen)
        assertFalse(state.isDataActionRunning)
        assertEquals(SettingsMessage.SampleDataLoaded(240), state.message)

        viewModel.onEvent(SettingsEvent.MessageShown)
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun loadSampleData_dismissed_changesNothing() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.LoadSampleDataClicked)
        viewModel.onEvent(SettingsEvent.LoadSampleDataDismissed)
        // Nothing is waiting to be confirmed any more
        viewModel.onEvent(SettingsEvent.LoadSampleDataConfirmed)

        assertFalse(viewModel.uiState.value.isLoadSampleDialogOpen)
        assertNull(viewModel.uiState.value.message)
        assertEquals(0, sampleData.loadCalls)
    }

    @Test
    fun aDataActionThatFails_saysWhy() = runTest {
        appData.fail = true
        appData.failure = DomainError.CategoryInUse
        sampleData.fail = true
        sampleData.failure = DomainError.NotFound
        val viewModel = viewModel()
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.DeleteAllDataClicked)
        viewModel.typeToConfirm("DELETE")
        viewModel.onEvent(SettingsEvent.DeleteAllDataConfirmed)
        assertEquals(
            SettingsMessage.Failed(R.string.error_category_in_use),
            viewModel.uiState.value.message
        )

        viewModel.onEvent(SettingsEvent.LoadSampleDataClicked)
        viewModel.onEvent(SettingsEvent.LoadSampleDataConfirmed)
        assertEquals(
            SettingsMessage.Failed(R.string.error_category_not_found),
            viewModel.uiState.value.message
        )
    }

    @Test
    fun loadSampleData_thatFails_saysSo() = runTest {
        val viewModel = viewModel()
        subscribe(viewModel.uiState)
        sampleData.fail = true

        viewModel.onEvent(SettingsEvent.LoadSampleDataClicked)
        viewModel.onEvent(SettingsEvent.LoadSampleDataConfirmed)

        assertEquals(
            SettingsMessage.Failed(R.string.error_storage_failure),
            viewModel.uiState.value.message
        )
        assertFalse(viewModel.uiState.value.isDataActionRunning)
    }

    @Test
    fun inAReleaseBuild_loadingSampleDataDoesNothing() = runTest {
        val viewModel = viewModel(isDebug = false)
        subscribe(viewModel.uiState)

        viewModel.onEvent(SettingsEvent.LoadSampleDataClicked)
        viewModel.onEvent(SettingsEvent.LoadSampleDataConfirmed)

        assertFalse(viewModel.uiState.value.isLoadSampleDialogOpen)
        assertNull(viewModel.uiState.value.message)
        assertEquals(0, sampleData.loadCalls)
    }
}
