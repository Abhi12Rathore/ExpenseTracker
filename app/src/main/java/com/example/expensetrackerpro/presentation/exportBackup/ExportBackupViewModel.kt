package com.example.expensetrackerpro.presentation.exportBackup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.ExportFile
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.domain.usecase.export.ExportTransactionsUseCase
import com.example.expensetrackerpro.domain.usecase.export.GetExportFileNameUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCurrentPeriodUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetCustomDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.filter.GetDateRangeUseCase
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupEvent
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupMessage
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupUiState
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExportBackupViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    private val getCurrentPeriod: GetCurrentPeriodUseCase,
    private val getDateRange: GetDateRangeUseCase,
    private val getCustomDateRange: GetCustomDateRangeUseCase,
    private val exportTransactions: ExportTransactionsUseCase,
    getExportFileName: GetExportFileNameUseCase,
    private val backupController: BackupController,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Restored after process death, which can happen while the "Save as" picker is in front
    private val filter = MutableStateFlow(restoredFilter())

    // The export in progress and its outcome; what the period holds is merged in below
    private val screenState = MutableStateFlow(
        ExportBackupUiState(
            period = filter.value.toExportPeriod(),
            dateRange = getDateRange(filter.value),
            suggestedFileName = getExportFileName(ExportFile.TRANSACTIONS_CSV),
            suggestedBackupFileName = getExportFileName(ExportFile.BACKUP)
        )
    )

    init {
        backupController.start(viewModelScope, savedStateHandle)
    }

    // The chosen period with the transactions an export of it would write, newest first
    private val selection = filter.flatMapLatest { selected ->
        val range = getDateRange(selected)
        getTransactions(range).map { transactions -> Triple(selected, range, transactions) }
    }

    val uiState: StateFlow<ExportBackupUiState> =
        combine(selection, screenState, backupController.state) { (selected, range, transactions), state, backup ->
            state.copy(
                backup = backup,
                period = selected.toExportPeriod(),
                dateRange = range,
                transactionCount = transactions.size,
                oldestMillis = transactions.lastOrNull()?.dateMillis,
                newestMillis = transactions.firstOrNull()?.dateMillis
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = screenState.value.copy(backup = backupController.initialState)
        )

    fun onEvent(event: ExportBackupEvent) {
        when (event) {
            is ExportBackupEvent.PeriodSelected -> selectPeriod(event.period)
            is ExportBackupEvent.CustomRangeSelected -> select(
                DateFilter.Custom(getCustomDateRange(event.startUtcMillis, event.endUtcMillis))
            )
            is ExportBackupEvent.DestinationPicked -> export(DocumentUri(event.uri))
            is ExportBackupEvent.BackupDestinationPicked ->
                if (!screenState.value.isExporting) {
                    backupController.onBackupDestinationPicked(DocumentUri(event.uri))
                }
            is ExportBackupEvent.RestoreFilePicked ->
                if (!screenState.value.isExporting) {
                    backupController.onRestoreFilePicked(DocumentUri(event.uri))
                }
            ExportBackupEvent.RestoreConfirmed -> backupController.onRestoreConfirmed()
            ExportBackupEvent.RestoreDismissed -> backupController.onRestoreDismissed()
            ExportBackupEvent.MessageShown -> clearShownMessage()
        }
    }

    // The export's message is shown first, so it is the first to go
    private fun clearShownMessage() {
        if (screenState.value.message != null) {
            screenState.update { it.copy(message = null) }
        } else {
            backupController.onMessageShown()
        }
    }

    private fun selectPeriod(period: ExportPeriod) {
        // Custom has no dates of its own: it is selected by choosing a range
        period.toFilter(customRange = null)?.let(::select)
    }

    private fun select(selected: DateFilter) {
        filter.value = selected

        savedStateHandle[KEY_PERIOD] = selected.toExportPeriod().name
        val customRange = (selected as? DateFilter.Custom)?.range
        savedStateHandle[KEY_CUSTOM_START] = customRange?.startMillis
        savedStateHandle[KEY_CUSTOM_END] = customRange?.endMillis
    }

    /** The period that was selected when the state was saved, or the current month. */
    private fun restoredFilter(): DateFilter {
        val period = savedStateHandle.get<String>(KEY_PERIOD)
            ?.let { name -> ExportPeriod.entries.find { it.name == name } }
        val start = savedStateHandle.get<Long>(KEY_CUSTOM_START)
        val end = savedStateHandle.get<Long>(KEY_CUSTOM_END)
        val customRange = if (start != null && end != null) DateRange(start, end) else null

        return period?.toFilter(customRange) ?: getCurrentPeriod(PeriodGranularity.MONTH)
    }

    /** Month and Year are the ones we are in now. Null for Custom without a [customRange]. */
    private fun ExportPeriod.toFilter(customRange: DateRange?): DateFilter? = when (this) {
        ExportPeriod.MONTH -> getCurrentPeriod(PeriodGranularity.MONTH)
        ExportPeriod.YEAR -> getCurrentPeriod(PeriodGranularity.YEAR)
        ExportPeriod.ALL_TIME -> DateFilter.Custom(DateRange.ALL)
        ExportPeriod.CUSTOM -> customRange?.let { DateFilter.Custom(it) }
    }

    private fun DateFilter.toExportPeriod(): ExportPeriod = when (this) {
        is DateFilter.Period ->
            if (granularity == PeriodGranularity.YEAR) ExportPeriod.YEAR else ExportPeriod.MONTH
        is DateFilter.Custom ->
            if (range == DateRange.ALL) ExportPeriod.ALL_TIME else ExportPeriod.CUSTOM
    }

    private fun export(destination: DocumentUri) {
        // The count is not checked: after process death the pick arrives before it has loaded,
        // and the file already exists by then. An empty period is written as a header alone.
        if (screenState.value.isExporting || backupController.isBusy) return
        val range = getDateRange(filter.value)

        screenState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val message = when (val result = exportTransactions(range, destination)) {
                is Outcome.Success -> ExportBackupMessage.Exported(result.value, destination.uri)
                is Outcome.Failure -> ExportBackupMessage.Failed(result.error.toMessageRes())
            }
            screenState.update { it.copy(isExporting = false, message = message) }
        }
    }

    private companion object {
        const val KEY_PERIOD = "export_period"
        const val KEY_CUSTOM_START = "export_custom_start"
        const val KEY_CUSTOM_END = "export_custom_end"
    }
}
