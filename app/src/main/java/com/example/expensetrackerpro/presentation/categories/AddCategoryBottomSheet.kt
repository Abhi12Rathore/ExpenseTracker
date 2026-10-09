package com.example.expensetrackerpro.presentation.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.CategoryIconGroup
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.category.color
import com.example.expensetrackerpro.presentation.common.category.labelRes
import com.example.expensetrackerpro.presentation.common.category.ui



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCategoryBottomSheet(
    initialCategory: Category? = null,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onSaveCategory: (String, CategoryIcon, TransactionType) -> Unit
) {
    // 1. Determine State and Mode
    val isEditMode = initialCategory != null
    var categoryName by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(initialCategory?.icon ?: CategoryIcon.DEFAULT) }

    // The type is fixed based on the context it was opened in (Income or Expense)
    val categoryType = initialCategory?.type ?: initialType

    val sheetState = rememberModalBottomSheetState()


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Now reflects the Type
            Column(modifier = Modifier.fillMaxWidth()) {
                val typeLabel = categoryType.name.lowercase().replaceFirstChar { it.uppercase() }
                Text(
                    text = if (isEditMode) "Edit $typeLabel Category" else "Add $typeLabel Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEditMode) "Modify details for your $typeLabel category"
                    else "Create a custom category for your $typeLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Input Field
            // Input Field
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = {
                    Icon(
                        imageVector = selectedIcon.ui.imageVector,
                        contentDescription = null,
                        tint = selectedIcon.color
                    )
                }
            )

            // Icon Grid
            Text(
                text = "Select Icon",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // The only part that scrolls, so the name and the save button stay in reach
            IconPicker(
                selected = selectedIcon,
                onSelect = { selectedIcon = it },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            )

            // Save Button: Contextual Color
            Button(
                onClick = { onSaveCategory(categoryName, selectedIcon, categoryType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (isEditMode) "Update Category" else "Add Category")
            }
        }
    }
}

/** Every icon, a section per [CategoryIconGroup]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(
    selected: CategoryIcon,
    onSelect: (CategoryIcon) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryIconGroup.entries.forEach { group ->
            Text(
                text = stringResource(group.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .semantics { heading() }
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryIcon.inGroup(group).forEach { icon ->
                    val isSelected = icon == selected

                    Surface(
                        selected = isSelected,
                        onClick = { onSelect(icon) },
                        shape = PickerTileShape,
                        color = Color.Transparent,
                        // The tile underneath is the same for every icon; only the ring marks the choice
                        border = if (isSelected) BorderStroke(2.dp, icon.color) else null
                    ) {
                        CategoryIconTile(
                            icon = icon,
                            modifier = Modifier.padding(3.dp),
                            contentDescription = stringResource(icon.ui.labelRes)
                        )
                    }
                }
            }
        }
    }
}

private val PickerTileShape = RoundedCornerShape(15.dp)

@PreviewLightDark
@Composable
private fun IconPickerPreview() {
    ExpenseTrackerProTheme {
        Surface {
            IconPicker(
                selected = CategoryIcon.SHOPPING_BASKET,
                onSelect = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
