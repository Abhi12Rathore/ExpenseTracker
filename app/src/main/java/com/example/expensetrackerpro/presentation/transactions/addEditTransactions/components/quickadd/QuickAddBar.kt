package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R

private val BarShape = RoundedCornerShape(28.dp)
private val ButtonSize = 40.dp

/**
 * One bar for every quick way to fill the form: type a sentence, say it, or scan a bill. While
 * there is text, the microphone and camera give way to the button that fills the form.
 */
@Composable
fun QuickAddBar(
    text: String,
    canFill: Boolean,
    isVoiceAvailable: Boolean,
    isScanAvailable: Boolean,
    isReadingBill: Boolean,
    onTextChange: (String) -> Unit,
    onFill: () -> Unit,
    onVoiceClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() }
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(stringResource(R.string.quick_add_hint), maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                BarButtons(
                    hasText = text.isNotBlank(),
                    canFill = canFill,
                    isVoiceAvailable = isVoiceAvailable,
                    isScanAvailable = isScanAvailable,
                    isReadingBill = isReadingBill,
                    onFill = onFill,
                    onVoiceClick = onVoiceClick,
                    onScanClick = onScanClick
                )
            },
            maxLines = 3,
            shape = BarShape,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (canFill) onFill() })
        )

        Text(
            text = stringResource(if (isReadingBill) R.string.bill_scan_reading else R.string.quick_add_helper),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BarButtons(
    hasText: Boolean,
    canFill: Boolean,
    isVoiceAvailable: Boolean,
    isScanAvailable: Boolean,
    isReadingBill: Boolean,
    onFill: () -> Unit,
    onVoiceClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasText) {
            FilledIconButton(onClick = onFill, enabled = canFill, modifier = Modifier.size(ButtonSize)) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.quick_add_fill))
            }
            return@Row
        }

        if (isVoiceAvailable) {
            FilledTonalIconButton(onClick = onVoiceClick, modifier = Modifier.size(ButtonSize)) {
                Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.quick_add_voice))
            }
        }
        if (isScanAvailable || isReadingBill) {
            FilledTonalIconButton(
                onClick = onScanClick,
                enabled = !isReadingBill,
                modifier = Modifier.size(ButtonSize)
            ) {
                if (isReadingBill) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = stringResource(R.string.bill_scan_title))
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickAddBarPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickAddBar(
                    text = "",
                    canFill = false,
                    isVoiceAvailable = true,
                    isScanAvailable = true,
                    isReadingBill = false,
                    onTextChange = {},
                    onFill = {},
                    onVoiceClick = {},
                    onScanClick = {}
                )
                QuickAddBar(
                    text = "Spent 450 on swiggy yesterday, dinner with Rahul",
                    canFill = true,
                    isVoiceAvailable = true,
                    isScanAvailable = true,
                    isReadingBill = false,
                    onTextChange = {},
                    onFill = {},
                    onVoiceClick = {},
                    onScanClick = {}
                )
            }
        }
    }
}
