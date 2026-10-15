package com.example.expensetrackerpro.presentation.exportBackup

import androidx.lifecycle.SavedStateHandle
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.usecase.backup.CreateBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.ObserveLastBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.PreviewBackupUseCase
import com.example.expensetrackerpro.domain.usecase.backup.RestoreBackupUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.exportBackup.uistate.BackupUiState
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Creating a backup and restoring from one: what the Backup card and the confirmation sheet
 * show. One belongs to each [ExportBackupViewModel], which forwards the screen's events to it.
 *
 * Of a file picked to restore from only the URI is kept, and in the saved state: the file is
 * read again when the restore is confirmed, and the sheet survives the process being killed.
 */
class BackupController @Inject constructor(
    private val createBackup: CreateBackupUseCase,
    private val previewBackup: PreviewBackupUseCase,
    private val restoreBackup: RestoreBackupUseCase,
    private val observeLastBackup: ObserveLastBackupUseCase
) {
    // Everything but the last backup, which is only followed while the screen is showing
    private val _state = MutableStateFlow(BackupUiState())

    val state: Flow<BackupUiState> =
        combine(_state, observeLastBackup()) { state, lastBackup -> state.copy(lastBackup = lastBackup) }

    /** The state before anything has been read. */
    val initialState: BackupUiState get() = _state.value

    /** True while a backup is being written, or a file read or restored. */
    val isBusy: Boolean get() = _state.value.isBusy

    private lateinit var scope: CoroutineScope
    private lateinit var savedStateHandle: SavedStateHandle

    private var restoreSource: DocumentUri?
        get() = savedStateHandle.get<String>(KEY_RESTORE_URI)?.let(::DocumentUri)
        set(value) {
            savedStateHandle[KEY_RESTORE_URI] = value?.uri
        }

    /** Works for as long as [scope] lives. A restore that was awaiting confirmation is offered again. */
    fun start(scope: CoroutineScope, savedStateHandle: SavedStateHandle) {
        this.scope = scope
        this.savedStateHandle = savedStateHandle

        _state.update { it.copy(switchCurrency = savedStateHandle[KEY_SWITCH_CURRENCY] ?: true) }
        restoreSource?.let(::preview)
    }

    fun onBackupDestinationPicked(destination: DocumentUri) {
        if (_state.value.isBusy) return

        _state.update { it.copy(isCreating = true) }
        scope.launch {
            val message = when (val result = createBackup(destination)) {
                is Outcome.Success -> ExportBackupMessage.BackupSaved(destination.uri)
                is Outcome.Failure -> ExportBackupMessage.Failed(result.error.toMessageRes())
            }
            _state.update { it.copy(isCreating = false, message = message) }
        }
    }

    fun onRestoreFilePicked(source: DocumentUri) {
        if (_state.value.isBusy) return

        restoreSource = source
        // Each file is offered its own currency afresh
        onCurrencySwitchToggled(true)
        preview(source)
    }

    /** Reads and checks the file. Only a backup that can be restored gets as far as the sheet. */
    private fun preview(source: DocumentUri) {
        _state.update { it.copy(isReading = true) }
        scope.launch {
            when (val result = previewBackup(source)) {
                is Outcome.Success ->
                    _state.update { it.copy(isReading = false, restorePreview = result.value) }
                is Outcome.Failure -> {
                    restoreSource = null
                    _state.update {
                        it.copy(isReading = false, message = ExportBackupMessage.Failed(result.error.toMessageRes()))
                    }
                }
            }
        }
    }

    fun onRestoreConfirmed() {
        val source = restoreSource ?: return
        // Only what the sheet showed is confirmed, and only once
        if (_state.value.restorePreview == null || _state.value.isRestoring) return

        _state.update { it.copy(isRestoring = true) }
        scope.launch {
            val switchCurrency = _state.value.switchCurrency
            val message = when (val result = restoreBackup(source, switchCurrency)) {
                is Outcome.Success ->
                    ExportBackupMessage.Restored(result.value.transactionCount, result.value.switchedTo)
                is Outcome.Failure -> ExportBackupMessage.Failed(result.error.toMessageRes())
            }
            restoreSource = null
            _state.update { it.copy(isRestoring = false, restorePreview = null, message = message) }
        }
    }

    fun onCurrencySwitchToggled(switch: Boolean) {
        savedStateHandle[KEY_SWITCH_CURRENCY] = switch
        _state.update { it.copy(switchCurrency = switch) }
    }

    fun onRestoreDismissed() {
        // Once the restore is running it can't be called off
        if (_state.value.isRestoring) return

        restoreSource = null
        _state.update { it.copy(restorePreview = null) }
    }

    fun onMessageShown() {
        _state.update { it.copy(message = null) }
    }

    private companion object {
        const val KEY_RESTORE_URI = "restore_uri"
        const val KEY_SWITCH_CURRENCY = "restore_switch_currency"
    }
}
