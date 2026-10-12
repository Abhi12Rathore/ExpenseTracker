package com.example.expensetrackerpro.presentation.exportBackup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.presentation.common.filter.components.CustomRangePickerDialog
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupEvent
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupMessage
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupUiState
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportPeriod
import com.example.expensetrackerpro.presentation.settings.components.SettingsItem
import com.example.expensetrackerpro.presentation.settings.components.SettingsSectionCard
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.utils.DateFormatter

private const val CSV_MIME_TYPE = "text/csv"

@Composable
fun ExportBackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExportBackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // The system "Save as" screen. It gives access to the one file, so no storage permission is needed
    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { uri ->
        // Null when the user backed out, which needs no message
        if (uri != null) viewModel.onEvent(ExportBackupEvent.DestinationPicked(uri.toString()))
    }

    ExportBackupContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onExportClick = { saveAsLauncher.launch(uiState.suggestedFileName) },
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBackupContent(
    state: ExportBackupUiState,
    onEvent: (ExportBackupEvent) -> Unit,
    onExportClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = when (val message = state.message) {
        is ExportBackupMessage.Exported -> pluralStringResource(
            R.plurals.export_success,
            message.transactionCount,
            message.transactionCount
        )
        is ExportBackupMessage.Failed -> stringResource(message.messageRes)
        null -> null
    }
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        onEvent(ExportBackupEvent.MessageShown)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.export_backup_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.export_backup_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSectionCard(title = stringResource(R.string.export_section_title)) {
                SettingsItem(
                    icon = Icons.Default.BarChart,
                    iconTint = MaterialTheme.extendedColors.categoryPalette.green,
                    title = stringResource(R.string.export_csv_title),
                    subtitle = stringResource(R.string.export_csv_subtitle),
                    below = {
                        ExportCsvControls(state = state, onEvent = onEvent, onExportClick = onExportClick)
                    }
                )
            }
        }
    }
}

/** The period choice, what it holds and the button that exports it. */
@Composable
private fun ExportCsvControls(
    state: ExportBackupUiState,
    onEvent: (ExportBackupEvent) -> Unit,
    onExportClick: () -> Unit
) {
    var showRangePicker by remember { mutableStateOf(false) }
    val periodLabels = ExportPeriod.entries.associateWith { stringResource(it.labelRes()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.export_period_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PillSegmentedControl(
            options = ExportPeriod.entries,
            selected = state.period,
            label = { periodLabels.getValue(it) },
            onSelect = { period ->
                // Custom is only selected once its dates have been chosen
                if (period == ExportPeriod.CUSTOM) {
                    showRangePicker = true
                } else {
                    onEvent(ExportBackupEvent.PeriodSelected(period))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isExporting
        )
        Text(
            text = exportSummary(state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = onExportClick,
            enabled = state.canExport,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            if (state.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.export_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showRangePicker) {
        CustomRangePickerDialog(
            onConfirm = { start, end ->
                onEvent(ExportBackupEvent.CustomRangeSelected(start, end))
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false }
        )
    }
}

/** "142 transactions · 1 Jan – 11 Oct 2026" */
@Composable
private fun exportSummary(state: ExportBackupUiState): String {
    val count = pluralStringResource(
        R.plurals.transaction_count,
        state.transactionCount,
        state.transactionCount
    )
    val oldest = state.oldestMillis
    val newest = state.newestMillis
    val range = when {
        // The range end is exclusive, so the last day shown is the one before it
        state.period != ExportPeriod.ALL_TIME -> DateFormatter.formatDayRange(
            state.dateRange.startMillis,
            state.dateRange.endMillis - 1,
            withYear = true
        )
        // All time has no days of its own, so it is named by what it holds
        oldest != null && newest != null -> DateFormatter.formatDayRange(oldest, newest, withYear = true)
        else -> stringResource(R.string.filter_all_time)
    }

    return stringResource(R.string.export_summary, count, range)
}

private fun ExportPeriod.labelRes(): Int = when (this) {
    ExportPeriod.MONTH -> R.string.filter_month
    ExportPeriod.YEAR -> R.string.filter_year
    ExportPeriod.ALL_TIME -> R.string.filter_all_time
    ExportPeriod.CUSTOM -> R.string.filter_custom
}

private const val PREVIEW_DAY_MILLIS = 86_400_000L

private val previewState = ExportBackupUiState(
    period = ExportPeriod.ALL_TIME,
    transactionCount = 142,
    oldestMillis = PreviewData.DATE_MILLIS - 281 * PREVIEW_DAY_MILLIS,
    newestMillis = PreviewData.DATE_MILLIS,
    suggestedFileName = "expenses_2025-10-09.csv"
)

// October 2025, the month of the preview date
private val previewMonth = DateRange(
    PreviewData.DATE_MILLIS - 8 * PREVIEW_DAY_MILLIS,
    PreviewData.DATE_MILLIS + 23 * PREVIEW_DAY_MILLIS
)

@PreviewLightDark
@Composable
private fun ExportBackupContentPreview() {
    ExpenseTrackerProTheme {
        ExportBackupContent(state = previewState, onEvent = {}, onExportClick = {}, onNavigateBack = {})
    }
}

@PreviewLightDark
@Composable
private fun ExportBackupContentMonthPreview() {
    ExpenseTrackerProTheme {
        ExportBackupContent(
            state = previewState.copy(
                period = ExportPeriod.MONTH,
                dateRange = previewMonth,
                transactionCount = 1
            ),
            onEvent = {},
            onExportClick = {},
            onNavigateBack = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ExportBackupContentEmptyPreview() {
    ExpenseTrackerProTheme {
        ExportBackupContent(
            state = ExportBackupUiState(period = ExportPeriod.ALL_TIME),
            onEvent = {},
            onExportClick = {},
            onNavigateBack = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ExportBackupContentExportingPreview() {
    ExpenseTrackerProTheme {
        ExportBackupContent(
            state = previewState.copy(isExporting = true),
            onEvent = {},
            onExportClick = {},
            onNavigateBack = {}
        )
    }
}
