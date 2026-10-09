package com.example.expensetrackerpro.presentation

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.theme.SetThemeModeUseCase
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import com.example.expensetrackerpro.presentation.settings.SettingsViewModel
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.util.MainDispatcherRule
import com.example.expensetrackerpro.util.subscribe
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(repository: FakeThemePreferenceRepository) = SettingsViewModel(
        GetThemeModeUseCase(repository),
        SetThemeModeUseCase(repository)
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
}
