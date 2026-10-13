package com.example.expensetrackerpro.presentation.exportBackup.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.BackupPreview
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter

private val CardShape = RoundedCornerShape(20.dp)

/** Asks before a backup replaces everything: what the file holds, and what will be lost. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupSheet(
    preview: BackupPreview,
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val restoring by rememberUpdatedState(isRestoring)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // A restore that is running can't be swiped away
        confirmValueChange = { !restoring }
    )

    ModalBottomSheet(onDismissRequest = onCancel, sheetState = sheetState) {
        RestoreBackupSheetContent(
            preview = preview,
            isRestoring = isRestoring,
            onConfirm = onConfirm,
            onCancel = onCancel
        )
    }
}

@Composable
fun RestoreBackupSheetContent(
    preview: BackupPreview,
    isRestoring: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.restore_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            if (preview.fileName.isNotBlank()) {
                Text(
                    text = preview.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val oldest = preview.oldestMillis
                val newest = preview.newestMillis

                DetailRow(
                    label = stringResource(R.string.restore_sheet_created),
                    value = LocalDateFormatter.current.formatDateTime(preview.createdAtMillis)
                )
                DetailRow(
                    label = stringResource(R.string.restore_sheet_transactions),
                    value = preview.transactionCount.toString()
                )
                DetailRow(
                    label = stringResource(R.string.restore_sheet_custom_categories),
                    value = preview.customCategoryCount.toString()
                )
                // A backup without transactions covers no period
                if (oldest != null && newest != null) {
                    DetailRow(
                        label = stringResource(R.string.restore_sheet_period),
                        value = LocalDateFormatter.current.formatMonthRange(oldest, newest)
                    )
                }
            }
        }

        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.restore_sheet_warning,
                        preview.currentTransactionCount,
                        preview.currentTransactionCount
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onConfirm,
                enabled = !isRestoring,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                if (isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.restore_sheet_confirm),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            TextButton(onClick = onCancel, enabled = !isRestoring) {
                Text(
                    text = stringResource(R.string.restore_sheet_cancel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

private const val PREVIEW_DAY_MILLIS = 86_400_000L

private val previewBackup = BackupPreview(
    fileName = "expensetracker-backup-2025-10-09.json",
    createdAtMillis = PreviewData.DATE_MILLIS,
    transactionCount = 142,
    customCategoryCount = 3,
    oldestMillis = PreviewData.DATE_MILLIS - 281 * PREVIEW_DAY_MILLIS,
    newestMillis = PreviewData.DATE_MILLIS,
    currentTransactionCount = 87
)

@PreviewLightDark
@Composable
private fun RestoreBackupSheetContentPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RestoreBackupSheetContent(
                preview = previewBackup,
                isRestoring = false,
                onConfirm = {},
                onCancel = {},
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RestoreBackupSheetContentRestoringPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RestoreBackupSheetContent(
                preview = previewBackup.copy(
                    fileName = "",
                    transactionCount = 0,
                    oldestMillis = null,
                    newestMillis = null,
                    currentTransactionCount = 1
                ),
                isRestoring = true,
                onConfirm = {},
                onCancel = {},
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
