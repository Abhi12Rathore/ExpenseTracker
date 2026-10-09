package com.example.expensetrackerpro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.domain.usecase.theme.SetThemeModeUseCase
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsEvent
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getThemeMode: GetThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase
) : ViewModel() {

    // Dialog state; the saved theme is merged in below
    private val screenState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> =
        combine(getThemeMode(), screenState) { themeMode, state ->
            state.copy(themeMode = themeMode)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.ThemeClicked ->
                screenState.update { it.copy(isThemeDialogOpen = true) }
            SettingsEvent.ThemeDialogDismissed -> closeThemeDialog()
            is SettingsEvent.ThemeModeSelected -> selectTheme(event.mode)
        }
    }

    private fun selectTheme(mode: ThemeMode) {
        closeThemeDialog()
        viewModelScope.launch { setThemeMode(mode) }
    }

    private fun closeThemeDialog() {
        screenState.update { it.copy(isThemeDialogOpen = false) }
    }
}
