package com.example.expensetrackerpro.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.theme.GetThemeModeUseCase
import com.example.expensetrackerpro.presentation.main.uistate.MainUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    getThemeMode: GetThemeModeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

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
    }
}
