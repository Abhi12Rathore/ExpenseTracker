package com.example.expensetrackerpro.presentation.language

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.AppLanguage
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
    buildInfo: BuildInfo,
    // Keeps an unapplied selection through rotation and process death; leaving the screen drops it
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        getLanguageOptions().let { options ->
            LanguageUiState(
                current = options.current,
                selected = savedStateHandle.get<String>(KEY_SELECTED)?.toChoice() ?: options.current,
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

    init {
        loadPreview(_uiState.value.selected)
    }

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
        // Only the selection changes: the lists stay as they are, so the screen's scroll never moves
        _uiState.update { it.copy(selected = choice) }
        savedStateHandle[KEY_SELECTED] = choice.toSaved()
        loadPreview(choice)
    }

    // The previous preview stays until this one is ready, so the card cross-fades rather than empties
    private fun loadPreview(choice: LanguageChoice) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val preview = getLanguagePreview(choice)
            _uiState.update { it.copy(preview = preview) }
        }
    }

    private fun applySelection() {
        val state = _uiState.value
        if (state.isApplied) setAppLanguage(state.selected)
    }

    private fun LanguageChoice.toSaved(): String = when (this) {
        LanguageChoice.SystemDefault -> SYSTEM_DEFAULT_SAVED
        is LanguageChoice.Specific -> language.tag
    }

    // A tag the app no longer has is ignored, and the screen opens on the current language
    private fun String.toChoice(): LanguageChoice? =
        if (this == SYSTEM_DEFAULT_SAVED) {
            LanguageChoice.SystemDefault
        } else {
            AppLanguage.entries.find { it.tag == this }?.let(LanguageChoice::Specific)
        }

    private companion object {
        const val KEY_SELECTED = "selected_language"
        const val SYSTEM_DEFAULT_SAVED = ""
    }
}
