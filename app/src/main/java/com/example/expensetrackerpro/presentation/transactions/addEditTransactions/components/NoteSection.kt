package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R

private val NoteShape = RoundedCornerShape(12.dp)

/**
 * A one-line "add a note" row until it is tapped or a note exists, then a multi-line field that
 * grows from three to six lines, with a character counter and a way to remove the note.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteSection(
    note: String,
    maxLength: Int,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onNoteChange: (String) -> Unit,
    onRemove: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isFromBill: Boolean = false
) {
    // Purely visual: tapping the row opens the field with the keyboard up, while a note that
    // was already there is only shown
    var focusWhenExpanded by remember { mutableStateOf(false) }

    if (!isExpanded) {
        Surface(
            onClick = {
                focusWhenExpanded = true
                onExpand()
            },
            modifier = modifier.fillMaxWidth(),
            shape = NoteShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = stringResource(R.string.transaction_note_add),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (focusWhenExpanded) {
            focusRequester.requestFocus()
            focusWhenExpanded = false
        }
    }
    // Keeps the field, counter included, above the keyboard as it gains focus and grows
    LaunchedEffect(isFocused, note) {
        if (isFocused) bringIntoViewRequester.bringIntoView()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        if (isFromBill) FromBillTag()

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    isFocused = it.isFocused
                    onFocusChange(it.isFocused)
                },
            label = { Text(stringResource(R.string.transaction_note)) },
            minLines = 3,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            shape = NoteShape,
            colors = billFieldColors(isFromBill)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    // The field leaves the screen without reporting that it lost focus
                    onFocusChange(false)
                    onRemove()
                }
            ) {
                Text(stringResource(R.string.transaction_note_remove))
            }
            Text(
                text = stringResource(R.string.transaction_note_counter, note.length, maxLength),
                modifier = Modifier.padding(end = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
