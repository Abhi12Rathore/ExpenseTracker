package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.utils.DateFormatter

/** "Today", "Yesterday", or the date itself when it is neither. */
@Composable
fun transactionDateLabel(dateChoice: DateChoice, dateMillis: Long): String = when (dateChoice) {
    DateChoice.TODAY -> stringResource(R.string.transaction_date_today)
    DateChoice.YESTERDAY -> stringResource(R.string.transaction_date_yesterday)
    DateChoice.OTHER -> DateFormatter.formatDate(dateMillis)
}

/** Today and Yesterday shortcuts, plus a chip that opens a date picker ending today. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionSection(
    dateChoice: DateChoice,
    dateMillis: Long,
    pickerUtcMillis: Long,
    maxPickerUtcMillis: Long,
    onDateChoiceSelected: (DateChoice) -> Unit,
    onDatePicked: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Purely visual: whether the date picker is showing
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionLabel(text = stringResource(R.string.transaction_date))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dateChoice == DateChoice.TODAY,
                onClick = { onDateChoiceSelected(DateChoice.TODAY) },
                label = { Text(stringResource(R.string.transaction_date_today)) }
            )
            FilterChip(
                selected = dateChoice == DateChoice.YESTERDAY,
                onClick = { onDateChoiceSelected(DateChoice.YESTERDAY) },
                label = { Text(stringResource(R.string.transaction_date_yesterday)) }
            )
            FilterChip(
                selected = dateChoice == DateChoice.OTHER,
                onClick = { showDatePicker = true },
                label = {
                    Text(
                        if (dateChoice == DateChoice.OTHER) DateFormatter.formatDate(dateMillis)
                        else stringResource(R.string.transaction_date_pick)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = pickerUtcMillis,
            selectableDates = remember(maxPickerUtcMillis) {
                // No future dates
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= maxPickerUtcMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDatePicked)
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.transaction_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.transaction_date_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
