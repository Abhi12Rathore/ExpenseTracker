package com.example.expensetrackerpro.presentation.exportBackup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.ExportDestination
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
    getExportFileName: GetExportFileNameUseCase
) : ViewModel() {

    private val filter = MutableStateFlow<DateFilter>(getCurrentPeriod(PeriodGranularity.MONTH))

    // The export in progress and its outcome; what the period holds is merged in below
    private val screenState = MutableStateFlow(
        ExportBackupUiState(
            dateRange = getDateRange(filter.value),
            suggestedFileName = getExportFileName()
        )
    )

    // The chosen period with the transactions an export of it would write, newest first
    private val selection = filter.flatMapLatest { selected ->
        val range = getDateRange(selected)
        getTransactions(range).map { transactions -> Triple(selected, range, transactions) }
    }

    val uiState: StateFlow<ExportBackupUiState> =
        combine(selection, screenState) { (selected, range, transactions), state ->
            state.copy(
                period = selected.toExportPeriod(),
                dateRange = range,
                transactionCount = transactions.size,
                oldestMillis = transactions.lastOrNull()?.dateMillis,
                newestMillis = transactions.firstOrNull()?.dateMillis
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), screenState.value)

    fun onEvent(event: ExportBackupEvent) {
        when (event) {
            is ExportBackupEvent.PeriodSelected -> selectPeriod(event.period)
            is ExportBackupEvent.CustomRangeSelected -> filter.value = DateFilter.Custom(
                getCustomDateRange(event.startUtcMillis, event.endUtcMillis)
            )
            is ExportBackupEvent.DestinationPicked -> export(ExportDestination(event.uri))
            ExportBackupEvent.MessageShown -> screenState.update { it.copy(message = null) }
        }
    }

    private fun selectPeriod(period: ExportPeriod) {
        filter.value = when (period) {
            ExportPeriod.MONTH -> getCurrentPeriod(PeriodGranularity.MONTH)
            ExportPeriod.YEAR -> getCurrentPeriod(PeriodGranularity.YEAR)
            ExportPeriod.ALL_TIME -> DateFilter.Custom(DateRange.ALL)
            // Has no dates of its own: it is selected by choosing a range
            ExportPeriod.CUSTOM -> return
        }
    }

    private fun DateFilter.toExportPeriod(): ExportPeriod = when (this) {
        is DateFilter.Period ->
            if (granularity == PeriodGranularity.YEAR) ExportPeriod.YEAR else ExportPeriod.MONTH
        is DateFilter.Custom ->
            if (range == DateRange.ALL) ExportPeriod.ALL_TIME else ExportPeriod.CUSTOM
    }

    private fun export(destination: ExportDestination) {
        // The button is disabled in both cases; this keeps a late or repeated pick out too
        if (screenState.value.isExporting || uiState.value.transactionCount == 0) return
        val range = getDateRange(filter.value)

        screenState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val message = when (val result = exportTransactions(range, destination)) {
                is Outcome.Success -> ExportBackupMessage.Exported(result.value)
                is Outcome.Failure -> ExportBackupMessage.Failed(result.error.toMessageRes())
            }
            screenState.update { it.copy(isExporting = false, message = message) }
        }
    }
}
