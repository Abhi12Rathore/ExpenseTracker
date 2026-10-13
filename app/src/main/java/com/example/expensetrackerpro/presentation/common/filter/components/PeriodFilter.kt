package com.example.expensetrackerpro.presentation.common.filter.components

import com.example.expensetrackerpro.presentation.common.date.LocalDateFormatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.PeriodGranularity
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterEvent
import com.example.expensetrackerpro.presentation.common.filter.PeriodFilterState
import com.example.expensetrackerpro.presentation.common.filter.labelRes
import com.example.expensetrackerpro.presentation.common.filter.shortLabel
import com.example.expensetrackerpro.presentation.common.filter.subtitle
import com.example.expensetrackerpro.presentation.common.filter.title

/**
 * The period filter Home shows in full: [PeriodFilterBar] (Week | Month | Year plus
 * the custom range button) above a [PeriodStepper], driven by one [PeriodFilterState].
 */
@Composable
fun PeriodFilter(
    state: PeriodFilterState,
    onEvent: (PeriodFilterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRangePicker by remember { mutableStateOf(false) }

    val isCustom = state.filter is DateFilter.Custom
    val granularityLabels = PeriodGranularity.entries.associateWith { stringResource(it.labelRes()) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PeriodFilterBar(
            options = PeriodGranularity.entries,
            selected = (state.filter as? DateFilter.Period)?.granularity,
            label = { granularityLabels.getValue(it) },
            onSelect = { onEvent(PeriodFilterEvent.GranularitySelected(it)) },
            onCustomRangeClick = { showRangePicker = true },
            isCustomActive = isCustom
        )

        PeriodStepper(
            title = state.title(LocalDateFormatter.current),
            subtitle = state.subtitle(LocalDateFormatter.current),
            canGoPrevious = !isCustom,
            canGoNext = state.canStepForward,
            onPrevious = { onEvent(PeriodFilterEvent.PreviousPeriod) },
            onNext = { onEvent(PeriodFilterEvent.NextPeriod) },
            showArrows = !isCustom
        )
    }

    if (showRangePicker) {
        CustomRangePickerDialog(
            onConfirm = { start, end ->
                onEvent(PeriodFilterEvent.CustomRangeSelected(start, end))
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false }
        )
    }
}

/**
 * The same filter on one row, for screens where the list needs the room: a chip that opens
 * Week / Month / Year, a compact [PeriodStepper] and the custom range button.
 */
@Composable
fun CompactPeriodFilter(
    state: PeriodFilterState,
    onEvent: (PeriodFilterEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRangePicker by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    val granularity = (state.filter as? DateFilter.Period)?.granularity
    val isAllTime = state.dateRange == DateRange.ALL
    val allTimeLabel = stringResource(R.string.filter_all_time)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            DropdownChip(
                label = when {
                    granularity != null -> stringResource(granularity.labelRes())
                    isAllTime -> allTimeLabel
                    else -> stringResource(R.string.filter_custom)
                },
                contentDescription = stringResource(R.string.filter_choose_granularity),
                onClick = { isMenuExpanded = true }
            )
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                PeriodGranularity.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes())) },
                        onClick = {
                            isMenuExpanded = false
                            onEvent(PeriodFilterEvent.GranularitySelected(option))
                        }
                    )
                }
            }
        }

        PeriodStepper(
            // An unbounded range has no days to name
            title = if (isAllTime) allTimeLabel else state.shortLabel(LocalDateFormatter.current),
            subtitle = null,
            canGoPrevious = granularity != null,
            canGoNext = state.canStepForward,
            onPrevious = { onEvent(PeriodFilterEvent.PreviousPeriod) },
            onNext = { onEvent(PeriodFilterEvent.NextPeriod) },
            modifier = Modifier.weight(1f),
            showArrows = granularity != null,
            compact = true
        )

        TonalSquareIconButton(
            icon = Icons.Rounded.CalendarMonth,
            contentDescription = stringResource(R.string.filter_custom_range),
            onClick = { showRangePicker = true },
            highlighted = granularity == null && !isAllTime
        )
    }

    if (showRangePicker) {
        CustomRangePickerDialog(
            onConfirm = { start, end ->
                onEvent(PeriodFilterEvent.CustomRangeSelected(start, end))
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false }
        )
    }
}

/** The date range picker behind every "Custom" period choice. Both dates come back as midnight UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomRangePickerDialog(
    onConfirm: (startUtcMillis: Long, endUtcMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDateRangePickerState()
    val start = pickerState.selectedStartDateMillis
    val end = pickerState.selectedEndDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = start != null && end != null,
                onClick = { if (start != null && end != null) onConfirm(start, end) }
            ) {
                Text(stringResource(R.string.filter_custom_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.filter_custom_cancel))
            }
        }
    ) {
        // Weighted so the picker can't push the buttons off the dialog
        DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
    }
}
