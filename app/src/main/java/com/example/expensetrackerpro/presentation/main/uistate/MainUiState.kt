package com.example.expensetrackerpro.presentation.main.uistate

data class MainUiState(
    // False while startup work runs; the system splash stays up until it turns true
    val isReady: Boolean = false
)
