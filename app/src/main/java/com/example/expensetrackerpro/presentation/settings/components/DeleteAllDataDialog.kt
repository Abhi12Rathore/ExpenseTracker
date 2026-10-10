package com.example.expensetrackerpro.presentation.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.presentation.settings.uistate.SettingsUiState
import com.example.expensetrackerpro.ExpenseTrackerProTheme

/** Deleting everything can't be undone, so the button only works once the word has been typed. */
@Composable
fun DeleteAllDataDialog(
    confirmation: String,
    isConfirmEnabled: Boolean,
    onConfirmationChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val word = SettingsUiState.DELETE_CONFIRMATION_WORD

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.settings_delete_all_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.settings_delete_all_dialog_message))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = onConfirmationChange,
                    label = { Text(stringResource(R.string.settings_delete_all_dialog_field, word)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.settings_delete_all_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_dialog_cancel)) }
        }
    )
}

@PreviewLightDark
@Composable
private fun DeleteAllDataDialogPreview() {
    ExpenseTrackerProTheme {
        DeleteAllDataDialog(
            confirmation = "DELE",
            isConfirmEnabled = false,
            onConfirmationChange = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
