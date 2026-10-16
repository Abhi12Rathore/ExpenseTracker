package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.quickadd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddBatchPreview
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddBatchRow

private val CardShape = RoundedCornerShape(16.dp)
private val RowShape = RoundedCornerShape(12.dp)

/**
 * Several transactions found in one sentence: a row of chips for each, the date they share, then
 * "Save all" with their total and "Review one by one". Every value is on screen before Save all,
 * which is offered only once each row has a title and a category; a row's category chip changes it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddBatchCard(
    batch: QuickAddBatchPreview,
    editing: QuickAddField?,
    editingRow: Int?,
    isSaving: Boolean,
    onCategoryClick: (Int) -> Unit,
    onCategoryChosen: (Int, CategoryId) -> Unit,
    onDateClick: () -> Unit,
    onEditDismissed: () -> Unit,
    onDateChoiceChosen: (DateChoice) -> Unit,
    onDatePicked: (Long) -> Unit,
    onSaveAll: () -> Unit,
    onReviewOneByOne: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val count = batch.rows.size
            Text(
                text = if (batch.type == TransactionType.EXPENSE) {
                    pluralStringResource(R.plurals.quick_add_batch_found_expenses, count, count)
                } else {
                    pluralStringResource(R.plurals.quick_add_batch_found_transactions, count, count)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            batch.rows.forEachIndexed { index, row ->
                BatchRow(
                    row = row,
                    // Rows with dates of their own show them; a date they share is shown once below
                    showDate = batch.hasMixedDates,
                    isEditingCategory = editing == QuickAddField.CATEGORY && editingRow == index,
                    onCategoryClick = { onCategoryClick(index) },
                    onCategoryChosen = { onCategoryChosen(index, it) },
                    onEditDismissed = onEditDismissed
                )
            }

            if (!batch.hasMixedDates) {
                DateChip(
                    date = batch.sharedDate,
                    pickerUtcMillis = batch.pickerUtcMillis,
                    maxPickerUtcMillis = batch.maxPickerUtcMillis,
                    isEditing = editing == QuickAddField.DATE,
                    onClick = onDateClick,
                    onEditDismissed = onEditDismissed,
                    onDateChoiceChosen = onDateChoiceChosen,
                    onDatePicked = onDatePicked
                )
            }

            if (!batch.canSaveAll) {
                Text(
                    text = stringResource(R.string.quick_add_batch_incomplete),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onSaveAll,
                enabled = batch.canSaveAll && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(2.dp))
                } else {
                    val total = batch.total
                    Text(
                        text = if (total != null) {
                            stringResource(R.string.quick_add_batch_save_all_total, LocalCurrencyFormatter.current.format(total))
                        } else {
                            stringResource(R.string.quick_add_batch_save_all)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onReviewOneByOne, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_add_batch_review), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BatchRow(
    row: QuickAddBatchRow,
    showDate: Boolean,
    isEditingCategory: Boolean,
    onCategoryClick: () -> Unit,
    onCategoryChosen: (CategoryId) -> Unit,
    onEditDismissed: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RowShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmountChip(row.amount, row.amountConfidence, onClick = null)
            CategoryChip(
                category = row.category,
                confidence = row.categoryConfidence,
                options = row.categoryOptions,
                isEditing = isEditingCategory,
                onClick = onCategoryClick,
                onEditDismissed = onEditDismissed,
                onCategoryChosen = onCategoryChosen
            )
            ConfidenceChip(
                label = row.title ?: stringResource(R.string.quick_add_title_missing),
                confidence = row.titleConfidence,
                icon = Icons.Rounded.EditNote
            )
            row.note?.let { note ->
                ConfidenceChip(label = note, confidence = FieldConfidence.SURE, icon = Icons.AutoMirrored.Rounded.Notes)
            }
            if (row.type == TransactionType.INCOME) {
                ConfidenceChip(
                    label = stringResource(R.string.transaction_type_income),
                    confidence = FieldConfidence.SURE,
                    icon = Icons.Rounded.SwapVert
                )
            }
            if (showDate) {
                // Each row's own date; changed by editing the sentence
                DateChip(
                    date = row.date,
                    pickerUtcMillis = 0L,
                    maxPickerUtcMillis = Long.MAX_VALUE,
                    isEditing = false,
                    onClick = null,
                    onEditDismissed = {},
                    onDateChoiceChosen = {},
                    onDatePicked = {}
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickAddBatchCardPreview() {
    val food = PreviewData.categories.first()
    val transport = PreviewData.categories[1]
    fun row(rupees: Long, title: String, category: Category?) = QuickAddBatchRow(
        amount = Money.ofRupees(rupees),
        amountConfidence = FieldConfidence.SURE,
        category = category,
        categoryConfidence = if (category != null) FieldConfidence.GUESS else FieldConfidence.MISSING,
        categoryOptions = PreviewData.categories,
        title = title,
        titleConfidence = FieldConfidence.SURE,
        date = null,
        note = null,
        type = TransactionType.EXPENSE
    )
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            QuickAddBatchCard(
                batch = QuickAddBatchPreview(
                    rows = listOf(row(450, "Swiggy", food), row(120, "Uber", transport)),
                    sharedDate = null,
                    hasMixedDates = false,
                    total = Money.ofRupees(570),
                    type = TransactionType.EXPENSE,
                    canSaveAll = true,
                    pickerUtcMillis = 0L,
                    maxPickerUtcMillis = Long.MAX_VALUE
                ),
                editing = null,
                editingRow = null,
                isSaving = false,
                onCategoryClick = {},
                onCategoryChosen = { _, _ -> },
                onDateClick = {},
                onEditDismissed = {},
                onDateChoiceChosen = {},
                onDatePicked = {},
                onSaveAll = {},
                onReviewOneByOne = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
