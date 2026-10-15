package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R

/** Sentences quick add understands, shown until it is first used. Tapping one puts it in the bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddExamples(onExampleSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val examples = listOf(
        stringResource(R.string.quick_add_example_1),
        stringResource(R.string.quick_add_example_2),
        stringResource(R.string.quick_add_example_3)
    )

    FlowRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { example ->
            SuggestionChip(onClick = { onExampleSelected(example) }, label = { Text(example) })
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickAddExamplesPreview() {
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuickAddExamples(onExampleSelected = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
