package com.example.expensetrackerpro.presentation.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.BuildInfo
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.usecase.language.GetLanguageOptionsUseCase
import com.example.expensetrackerpro.domain.usecase.language.GetLanguagePreviewUseCase
import com.example.expensetrackerpro.domain.usecase.language.SetAppLanguageUseCase
import com.example.expensetrackerpro.presentation.language.uistate.LanguageEvent
import com.example.expensetrackerpro.presentation.language.uistate.LanguageUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    getLanguageOptions: GetLanguageOptionsUseCase,
    private val getLanguagePreview: GetLanguagePreviewUseCase,
    private val setAppLanguage: SetAppLanguageUseCase,
    buildInfo: BuildInfo
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        getLanguageOptions().let { options ->
            LanguageUiState(
                current = options.current,
                selected = options.current,
                systemLanguage = options.systemLanguage,
                systemLanguageName = options.systemLanguageName,
                suggested = options.suggested,
                others = options.others,
                versionName = buildInfo.versionName
            )
        }
    )
    val uiState: StateFlow<LanguageUiState> = _uiState.asStateFlow()

    // Only the latest selection's preview is wanted
    private var previewJob: Job? = null

    fun onEvent(event: LanguageEvent) {
        when (event) {
            is LanguageEvent.LanguageSelected -> select(event.choice)
            LanguageEvent.ApplyClicked -> _uiState.update {
                if (it.isApplyVisible) it.copy(isApplied = true) else it
            }
            LanguageEvent.ClosedAfterApply -> applySelection()
        }
    }

    private fun select(choice: LanguageChoice) {
        previewJob?.cancel()
        // The old preview is dropped at once, so it is never shown beside another language's row
        _uiState.update { it.copy(selected = choice, preview = null) }
        if (choice == _uiState.value.current) return

        previewJob = viewModelScope.launch {
            val preview = getLanguagePreview(choice)
            _uiState.update { it.copy(preview = preview) }
        }
    }

    private fun applySelection() {
        val state = _uiState.value
        if (state.isApplied) setAppLanguage(state.selected)
    }
}
