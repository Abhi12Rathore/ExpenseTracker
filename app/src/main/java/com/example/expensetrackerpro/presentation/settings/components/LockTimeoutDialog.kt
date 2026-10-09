package com.example.expensetrackerpro.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.LockTimeout
import com.example.expensetrackerpro.presentation.settings.labelRes

@Composable
fun LockTimeoutDialog(
    selectedTimeout: LockTimeout,
    onTimeoutSelected: (LockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.lock_timeout_dialog_title)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                LockTimeout.entries.forEach { timeout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = timeout == selectedTimeout,
                                onClick = { onTimeoutSelected(timeout) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // The whole row is the click target, so the button itself takes no clicks
                        RadioButton(selected = timeout == selectedTimeout, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(timeout.labelRes()),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.lock_timeout_dialog_cancel))
            }
        }
    )
}
