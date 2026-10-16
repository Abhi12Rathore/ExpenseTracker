package com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.quickadd

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.FieldConfidence
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.QuickAddField
import com.example.expensetrackerpro.domain.models.TransactionDate
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.category.ui
import com.example.expensetrackerpro.presentation.common.currency.LocalCurrencyFormatter
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.TransactionDatePickerDialog
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.transactionDateLabel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.QuickAddPreview
import com.example.expensetrackerpro.ui.theme.extendedColors

private val CardShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(10.dp)
private val ChipCorner = 10.dp
private val ChipIconSize = 16.dp
private val ChipMaxWidth = 220.dp
private val MenuMaxHeight = 280.dp

/**
 * What quick add understood from the bar's text, before anything in the form changes: a chip per
 * value, green when found, amber when guessed and dashed red when missing. The category and date
 * chips open a short list to change them; the amount chip fills the form and brings up the keypad.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddPreviewCard(
    preview: QuickAddPreview,
    editing: QuickAddField?,
    onChipClick: (QuickAddField) -> Unit,
    onEditDismissed: () -> Unit,
    onCategoryChosen: (CategoryId) -> Unit,
    onDateChoiceChosen: (DateChoice) -> Unit,
    onDatePicked: (Long) -> Unit,
    onFill: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.quick_add_understood),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.quick_add_tap_to_change),
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Each chip already sits in a 48dp slot, which spaces the rows
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AmountChip(preview.amount, preview.amountConfidence, onClick = { onChipClick(QuickAddField.AMOUNT) })
                CategoryChip(preview, isEditing = editing == QuickAddField.CATEGORY, onChipClick, onEditDismissed, onCategoryChosen)
                ConfidenceChip(
                    label = preview.title ?: stringResource(R.string.quick_add_title_missing),
                    confidence = preview.titleConfidence,
                    icon = Icons.Rounded.EditNote
                )
                DateChip(
                    date = preview.date,
                    pickerUtcMillis = preview.pickerUtcMillis,
                    maxPickerUtcMillis = preview.maxPickerUtcMillis,
                    isEditing = editing == QuickAddField.DATE,
                    onClick = { onChipClick(QuickAddField.DATE) },
                    onEditDismissed = onEditDismissed,
                    onDateChoiceChosen = onDateChoiceChosen,
                    onDatePicked = onDatePicked
                )
                preview.note?.let { note ->
                    ConfidenceChip(label = note, confidence = FieldConfidence.SURE, icon = Icons.AutoMirrored.Rounded.Notes)
                }
                if (preview.isTypeSaid) {
                    ConfidenceChip(
                        label = stringResource(
                            if (preview.type == TransactionType.INCOME) R.string.transaction_type_income
                            else R.string.transaction_type_expense
                        ),
                        confidence = FieldConfidence.SURE,
                        icon = Icons.Rounded.SwapVert
                    )
                }
            }

            Button(onClick = onFill, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.quick_add_fill), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AmountChip(amount: Money?, confidence: FieldConfidence, onClick: () -> Unit) {
    ConfidenceChip(
        label = amount?.let { LocalCurrencyFormatter.current.format(it) }
            ?: stringResource(R.string.quick_add_amount_missing),
        confidence = confidence,
        icon = Icons.Rounded.Payments,
        onClick = onClick
    )
}

@Composable
private fun CategoryChip(
    preview: QuickAddPreview,
    isEditing: Boolean,
    onChipClick: (QuickAddField) -> Unit,
    onEditDismissed: () -> Unit,
    onCategoryChosen: (CategoryId) -> Unit
) {
    Box {
        ConfidenceChip(
            label = preview.category?.name ?: stringResource(R.string.quick_add_category_missing),
            confidence = preview.categoryConfidence,
            icon = preview.category?.icon?.ui?.imageVector ?: Icons.Rounded.Category,
            onClick = { onChipClick(QuickAddField.CATEGORY) }
        )
        // A short list: the suggestion first, the rest a scroll away (the form shows them all too)
        val options = listOfNotNull(preview.category) + preview.categoryOptions.filter { it.id != preview.category?.id }
        DropdownMenu(
            expanded = isEditing,
            onDismissRequest = onEditDismissed,
            modifier = Modifier.heightIn(max = MenuMaxHeight)
        ) {
            options.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = category.name,
                            fontWeight = if (category.id == preview.category?.id) FontWeight.SemiBold else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { CategoryIconTile(icon = category.icon, size = 28.dp) },
                    onClick = { onCategoryChosen(category.id) }
                )
            }
        }
    }
}

@Composable
private fun DateChip(
    date: TransactionDate?,
    pickerUtcMillis: Long,
    maxPickerUtcMillis: Long,
    isEditing: Boolean,
    onClick: () -> Unit,
    onEditDismissed: () -> Unit,
    onDateChoiceChosen: (DateChoice) -> Unit,
    onDatePicked: (Long) -> Unit
) {
    // Purely visual: whether the date picker is showing
    var showDatePicker by remember { mutableStateOf(false) }

    Box {
        ConfidenceChip(
            // No date said: the form keeps today, which is neither found nor missing
            label = date?.let { transactionDateLabel(it.choice, it.millis) }
                ?: stringResource(R.string.transaction_date_today),
            confidence = if (date != null) FieldConfidence.SURE else null,
            icon = Icons.Rounded.CalendarToday,
            onClick = onClick
        )
        DropdownMenu(expanded = isEditing, onDismissRequest = onEditDismissed) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transaction_date_today)) },
                onClick = { onDateChoiceChosen(DateChoice.TODAY) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transaction_date_yesterday)) },
                onClick = { onDateChoiceChosen(DateChoice.YESTERDAY) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.transaction_date_pick)) },
                onClick = {
                    onEditDismissed()
                    showDatePicker = true
                }
            )
        }
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            pickerUtcMillis = pickerUtcMillis,
            maxPickerUtcMillis = maxPickerUtcMillis,
            onDatePicked = onDatePicked,
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * A value and how sure quick add is of it: green when found, amber when a guess, a dashed red
 * outline when missing, and plain when [confidence] is null (nothing said, nothing needed).
 */
@Composable
private fun ConfidenceChip(
    label: String,
    confidence: FieldConfidence?,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    val (container, content) = when (confidence) {
        FieldConfidence.SURE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        FieldConfidence.GUESS -> MaterialTheme.extendedColors.guessContainer to MaterialTheme.extendedColors.onGuessContainer
        FieldConfidence.MISSING -> Color.Transparent to MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    }
    val description = when (confidence) {
        FieldConfidence.SURE -> stringResource(R.string.quick_add_sure, label)
        FieldConfidence.GUESS -> stringResource(R.string.quick_add_guess, label)
        FieldConfidence.MISSING -> stringResource(R.string.quick_add_missing, label)
        null -> label
    }
    val dashColor = MaterialTheme.colorScheme.error
    // The same touch-sized slot for every chip, tappable or not, so a row of them lines up
    val modifier = Modifier
        .minimumInteractiveComponentSize()
        .widthIn(max = ChipMaxWidth)
        .heightIn(min = 32.dp)
        .semantics(mergeDescendants = true) { contentDescription = description }
        .then(
            if (confidence == FieldConfidence.MISSING) {
                Modifier.drawBehind {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(ChipCorner.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                    )
                }
            } else {
                Modifier
            }
        )

    val chipContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = ChipShape, color = container, contentColor = content) {
            chipContent()
        }
    } else {
        Surface(modifier = modifier, shape = ChipShape, color = container, contentColor = content) {
            chipContent()
        }
    }
}

@PreviewLightDark
@Composable
private fun QuickAddPreviewCardPreview() {
    val food = PreviewData.categories.first()
    ExpenseTrackerProTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickAddPreviewCard(
                    preview = QuickAddPreview(
                        amount = Money.ofRupees(450),
                        amountConfidence = FieldConfidence.SURE,
                        category = food,
                        categoryConfidence = FieldConfidence.GUESS,
                        categoryOptions = PreviewData.categories,
                        title = "Swiggy",
                        titleConfidence = FieldConfidence.SURE,
                        date = TransactionDate(PreviewData.DATE_MILLIS, DateChoice.YESTERDAY, 0L, 0L),
                        note = "dinner with Rahul",
                        type = TransactionType.EXPENSE,
                        isTypeSaid = false,
                        pickerUtcMillis = 0L,
                        maxPickerUtcMillis = Long.MAX_VALUE
                    ),
                    editing = null,
                    onChipClick = {},
                    onEditDismissed = {},
                    onCategoryChosen = {},
                    onDateChoiceChosen = {},
                    onDatePicked = {},
                    onFill = {}
                )
                QuickAddPreviewCard(
                    preview = QuickAddPreview(
                        amount = null,
                        amountConfidence = FieldConfidence.MISSING,
                        category = null,
                        categoryConfidence = FieldConfidence.MISSING,
                        categoryOptions = PreviewData.categories,
                        title = "DMart",
                        titleConfidence = FieldConfidence.SURE,
                        date = null,
                        note = null,
                        type = TransactionType.EXPENSE,
                        isTypeSaid = false,
                        pickerUtcMillis = 0L,
                        maxPickerUtcMillis = Long.MAX_VALUE
                    ),
                    editing = null,
                    onChipClick = {},
                    onEditDismissed = {},
                    onCategoryChosen = {},
                    onDateChoiceChosen = {},
                    onDatePicked = {},
                    onFill = {}
                )
            }
        }
    }
}
