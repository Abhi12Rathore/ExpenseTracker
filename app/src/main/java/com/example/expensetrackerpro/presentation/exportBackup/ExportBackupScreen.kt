package com.example.expensetrackerpro.presentation.exportBackup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.utils.nameRes
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.DayLabel
import com.example.expensetrackerpro.domain.models.LastBackup
import com.example.expensetrackerpro.presentation.common.filter.components.CustomRangePickerDialog
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.exportBackup.components.RestoreBackupSheet
import com.example.expensetrackerpro.presentation.exportBackup.uistate.BackupUiState
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupEvent
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupMessage
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportBackupUiState
import com.example.expensetrackerpro.presentation.exportBackup.uistate.ExportPeriod
import com.example.expensetrackerpro.presentation.settings.components.SettingsItem
import com.example.expensetrackerpro.presentation.settings.components.SettingsSectionCard
import com.example.expensetrackerpro.ui.theme.extendedColors
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter

private const val CSV_MIME_TYPE = "text/csv"
private const val BACKUP_MIME_TYPE = "application/json"

// Some file providers list a .json file as a plain or unknown file
private val RESTORE_MIME_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/octet-stream", "text/plain")

private val NoteShape = RoundedCornerShape(16.dp)

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

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        if (uri != null) viewModel.onEvent(ExportBackupEvent.BackupDestinationPicked(uri.toString()))
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.onEvent(ExportBackupEvent.RestoreFilePicked(uri.toString()))
    }

    val context = LocalContext.current

    ExportBackupContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onExportClick = { saveAsLauncher.launch(uiState.suggestedFileName) },
        onCreateBackupClick = { saveBackupLauncher.launch(uiState.suggestedBackupFileName) },
        onRestoreClick = { openBackupLauncher.launch(RESTORE_MIME_TYPES) },
        onShareFile = context::shareFile,
        onNavigateBack = onNavigateBack
    )
}

/** Offers a file the user just saved to the apps that can send or open it. */
private fun Context.shareFile(uri: String, mimeType: String) {
    val file = uri.toUri()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, file)
        // The receiving app is let in through the clip data, for this one file only
        clipData = ClipData.newRawUri(null, file)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    startActivity(Intent.createChooser(send, null))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBackupContent(
    state: ExportBackupUiState,
    onEvent: (ExportBackupEvent) -> Unit,
    onExportClick: () -> Unit,
    onCreateBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    // A saved file's URI and type, when Share is tapped on its snackbar
    onShareFile: (uri: String, mimeType: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.pendingMessage
    val messageText = when (message) {
        is ExportBackupMessage.Exported -> pluralStringResource(
            R.plurals.export_success,
            message.transactionCount,
            message.transactionCount
        )
        is ExportBackupMessage.BackupSaved -> stringResource(R.string.backup_saved)
        is ExportBackupMessage.Restored -> when (val currency = message.switchedTo) {
            null -> pluralStringResource(R.plurals.restore_success, message.transactionCount, message.transactionCount)
            else -> pluralStringResource(
                R.plurals.restore_success_currency,
                message.transactionCount,
                message.transactionCount,
                stringResource(currency.nameRes())
            )
        }
        is ExportBackupMessage.Failed -> stringResource(message.messageRes)
        null -> null
    }
    val shareLabel = stringResource(R.string.export_share)
    LaunchedEffect(message) {
        if (messageText == null) return@LaunchedEffect
        // Only a file that was saved can be shared
        val sharedFile = when (message) {
            is ExportBackupMessage.Exported -> message.uri to CSV_MIME_TYPE
            is ExportBackupMessage.BackupSaved -> message.uri to BACKUP_MIME_TYPE
            else -> null
        }
        val result = snackbarHostState.showSnackbar(
            message = messageText,
            actionLabel = shareLabel.takeIf { sharedFile != null },
            // With an action a snackbar would otherwise stay until it is dismissed
            duration = if (sharedFile != null) SnackbarDuration.Long else SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed && sharedFile != null) {
            onShareFile(sharedFile.first, sharedFile.second)
        }
        onEvent(ExportBackupEvent.MessageShown)
    }

    state.backup.restorePreview?.let { preview ->
        RestoreBackupSheet(
            preview = preview,
            isRestoring = state.backup.isRestoring,
            switchCurrency = state.backup.switchCurrency,
            onSwitchCurrencyChange = { onEvent(ExportBackupEvent.RestoreCurrencySwitchToggled(it)) },
            onConfirm = { onEvent(ExportBackupEvent.RestoreConfirmed) },
            onCancel = { onEvent(ExportBackupEvent.RestoreDismissed) }
        )
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

            SettingsSectionCard(title = stringResource(R.string.backup_section_title)) {
                SettingsItem(
                    icon = Icons.Default.Inventory2,
                    iconTint = MaterialTheme.extendedColors.categoryPalette.blue,
                    title = stringResource(R.string.backup_title),
                    subtitle = stringResource(R.string.backup_subtitle),
                    below = {
                        BackupControls(
                            state = state.backup,
                            enabled = !state.isBusy,
                            onCreateBackupClick = onCreateBackupClick,
                            onRestoreClick = onRestoreClick
                        )
                    }
                )
            }

            PrivacyNote()
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
            enabled = !state.isBusy
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
            ButtonLabel(text = stringResource(R.string.export_button), isWorking = state.isExporting)
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

/** When the last backup was made, and the buttons that create one and restore from one. */
@Composable
private fun BackupControls(
    state: BackupUiState,
    enabled: Boolean,
    onCreateBackupClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.lastBackup?.let { lastBackup ->
            val done = MaterialTheme.extendedColors.categoryPalette.teal
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = done, modifier = Modifier.size(18.dp))
                Text(
                    text = lastBackupText(lastBackup),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = done
                )
            }
        }
        Button(
            onClick = onCreateBackupClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            ButtonLabel(text = stringResource(R.string.backup_create_button), isWorking = state.isCreating)
        }
        OutlinedButton(
            onClick = onRestoreClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            ButtonLabel(text = stringResource(R.string.backup_restore_button), isWorking = state.isReading)
        }
    }
}

/** A button's text, or a spinner in its place while its action runs. */
@Composable
private fun ButtonLabel(text: String, isWorking: Boolean) {
    if (isWorking) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = LocalContentColor.current,
            strokeWidth = 2.dp
        )
    } else {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** "Last backup: today, 4:20 pm" */
@Composable
private fun lastBackupText(lastBackup: LastBackup): String = when (lastBackup.day) {
    DayLabel.TODAY ->
        stringResource(R.string.backup_last_today, LocalDateFormatter.current.formatTime(lastBackup.millis))
    DayLabel.YESTERDAY ->
        stringResource(R.string.backup_last_yesterday, LocalDateFormatter.current.formatTime(lastBackup.millis))
    DayLabel.OTHER ->
        stringResource(R.string.backup_last_on, LocalDateFormatter.current.formatDateTime(lastBackup.millis))
}

@Composable
private fun PrivacyNote() {
    Surface(
        shape = NoteShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.backup_privacy_note),
                style = MaterialTheme.typography.bodyMedium
            )
        }
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
        state.period != ExportPeriod.ALL_TIME -> LocalDateFormatter.current.formatDayRange(
            state.dateRange.startMillis,
            state.dateRange.endMillis - 1,
            withYear = true
        )
        // All time has no days of its own, so it is named by what it holds
        oldest != null && newest != null -> LocalDateFormatter.current.formatDayRange(oldest, newest, withYear = true)
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
    suggestedFileName = "expenses_2025-10-09.csv",
    backup = BackupUiState(lastBackup = LastBackup(PreviewData.DATE_MILLIS, DayLabel.TODAY))
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
        ExportBackupContent(
            state = previewState,
            onEvent = {},
            onExportClick = {},
            onCreateBackupClick = {},
            onRestoreClick = {},
            onShareFile = { _, _ -> },
            onNavigateBack = {}
        )
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
            onCreateBackupClick = {},
            onRestoreClick = {},
            onShareFile = { _, _ -> },
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
            onCreateBackupClick = {},
            onRestoreClick = {},
            onShareFile = { _, _ -> },
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
            onCreateBackupClick = {},
            onRestoreClick = {},
            onShareFile = { _, _ -> },
            onNavigateBack = {}
        )
    }
}
