package com.example.expensetrackerpro.presentation.common.category.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.domain.models.CategoryNameValidation
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.ValidateCategoryNameUseCase
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.category.labelRes
import com.example.expensetrackerpro.presentation.common.category.ui
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import kotlinx.coroutines.launch

private const val ICON_COLUMNS = 5
private val PreviewTileSize = 64.dp
private val PickerTileShape = RoundedCornerShape(16.dp)
private val FieldShape = RoundedCornerShape(16.dp)

/** The sheet that creates a category or edits one, wherever categories can be added. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    state: CategoryEditorState,
    onEvent: (CategoryEditorEvent) -> Unit
) {
    // Opens at full height, so the save button is not left below the fold
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { onEvent(CategoryEditorEvent.Dismissed) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // The sheet's own back callback outranks the keyboard's once predictive back is on
        // (targetSdk 36), so one Back would close both and lose what was typed. Back is handled
        // below instead, where the keyboard gets it first
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BackHandler {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onEvent(CategoryEditorEvent.Dismissed)
            }
        }
        CategoryEditorContent(
            state = state,
            onEvent = onEvent,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

@Composable
fun CategoryEditorContent(
    state: CategoryEditorState,
    onEvent: (CategoryEditorEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(
                if (state.isEditMode) R.string.category_editor_title_edit else R.string.category_editor_title_new
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics { heading() }
        )

        CategoryPreview(name = state.name, type = state.type, icon = state.icon)

        val expenseLabel = stringResource(R.string.transaction_type_expense)
        val incomeLabel = stringResource(R.string.transaction_type_income)
        PillSegmentedControl(
            options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
            selected = state.type,
            label = { if (it == TransactionType.INCOME) incomeLabel else expenseLabel },
            onSelect = { onEvent(CategoryEditorEvent.TypeChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isEditMode
        )

        NameField(
            name = state.name,
            error = state.nameError,
            onNameChange = { onEvent(CategoryEditorEvent.NameChanged(it)) }
        )

        IconPicker(
            group = state.iconGroup,
            selected = state.icon,
            onGroupSelect = { onEvent(CategoryEditorEvent.IconGroupSelected(it)) },
            onIconSelect = { onEvent(CategoryEditorEvent.IconSelected(it)) }
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { onEvent(CategoryEditorEvent.SaveClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = FieldShape,
                enabled = state.isSaveEnabled
            ) {
                Text(
                    stringResource(
                        if (state.isEditMode) R.string.category_editor_save else R.string.category_editor_create
                    )
                )
            }
            if (state.isEditMode) {
                TextButton(
                    onClick = { onEvent(CategoryEditorEvent.DeleteClicked) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.category_editor_delete))
                }
            }
        }
    }
}

/** The category as it will look in a list, updating as the form changes. */
@Composable
private fun CategoryPreview(name: String, type: TransactionType, icon: CategoryIcon) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategoryIconTile(icon = icon, size = PreviewTileSize)
            Column {
                Text(
                    text = name.trim().ifEmpty { stringResource(R.string.category_editor_name_placeholder) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (name.isBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        if (type == TransactionType.INCOME) R.string.category_editor_type_income
                        else R.string.category_editor_type_expense
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NameField(
    name: String,
    error: CategoryNameValidation?,
    onNameChange: (String) -> Unit
) {
    val maxLength = ValidateCategoryNameUseCase.NAME_MAX_LENGTH
    val errorText = when (error) {
        null, CategoryNameValidation.VALID -> null
        CategoryNameValidation.BLANK -> stringResource(R.string.category_editor_name_error_blank)
        CategoryNameValidation.TOO_LONG ->
            pluralStringResource(R.plurals.category_editor_name_error_too_long, maxLength, maxLength)
        CategoryNameValidation.DUPLICATE -> stringResource(R.string.category_editor_name_error_duplicate)
    }

    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.category_editor_name_label)) },
        modifier = Modifier.fillMaxWidth(),
        shape = FieldShape,
        singleLine = true,
        isError = errorText != null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        supportingText = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = errorText.orEmpty(), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.category_editor_name_counter, name.length, maxLength))
            }
        }
    )
}

/** A tab per [CategoryIconGroup], and the icons of the one that is open. */
@Composable
private fun IconPicker(
    group: CategoryIconGroup,
    selected: CategoryIcon,
    onGroupSelect: (CategoryIconGroup) -> Unit,
    onIconSelect: (CategoryIcon) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.category_editor_icon_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryIconGroup.entries.forEach { entry ->
                FilterChip(
                    selected = entry == group,
                    onClick = { onGroupSelect(entry) },
                    label = { Text(stringResource(entry.labelRes())) }
                )
            }
        }

        CategoryIcon.inGroup(group).chunked(ICON_COLUMNS).forEach { rowIcons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIcons.forEach { icon ->
                    IconChoice(
                        icon = icon,
                        isSelected = icon == selected,
                        onClick = { onIconSelect(icon) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Keeps the tiles of a short last row the same width as the rest
                repeat(ICON_COLUMNS - rowIcons.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun IconChoice(
    icon: CategoryIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.aspectRatio(1f)) {
        Surface(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = PickerTileShape,
            color = Color.Transparent,
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Box(contentAlignment = Alignment.Center) {
                CategoryIconTile(icon = icon, contentDescription = stringResource(icon.ui.labelRes))
            }
        }
        if (isSelected) {
            // Sits on the corner of the outline, outside what the tile clips
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CategoryEditorCreatePreview() {
    ExpenseTrackerProTheme {
        Surface {
            CategoryEditorContent(
                state = CategoryEditorState(
                    name = "Pottery class",
                    icon = CategoryIcon.SCHOOL,
                    nameValidation = CategoryNameValidation.VALID,
                    isNameEdited = true
                ),
                onEvent = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CategoryEditorEditPreview() {
    val category = PreviewData.categories.first()

    ExpenseTrackerProTheme {
        Surface {
            CategoryEditorContent(
                state = CategoryEditorState(
                    editing = category,
                    name = category.name,
                    type = category.type,
                    icon = category.icon,
                    nameValidation = CategoryNameValidation.VALID
                ),
                onEvent = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CategoryEditorDuplicatePreview() {
    ExpenseTrackerProTheme {
        Surface {
            CategoryEditorContent(
                state = CategoryEditorState(
                    name = "Food",
                    type = TransactionType.INCOME,
                    icon = CategoryIcon.SAVINGS,
                    nameValidation = CategoryNameValidation.DUPLICATE,
                    isNameEdited = true
                ),
                onEvent = {}
            )
        }
    }
}
