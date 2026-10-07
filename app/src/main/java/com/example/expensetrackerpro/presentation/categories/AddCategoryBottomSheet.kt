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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.utils.CategoryIcons



@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCategoryBottomSheet(
    initialCategory: Category? = null,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onSaveCategory: (String, String, TransactionType) -> Unit // New: Returns the type
) {
    // 1. Determine State and Mode
    val isEditMode = initialCategory != null
    var categoryName by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedIconName by remember { mutableStateOf(initialCategory?.iconName ?: "Shopping") }

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
                        imageVector = CategoryIcons.icons[selectedIconName] ?: Icons.Default.Category,
                        contentDescription = null,
                        tint = CategoryIcons.iconColors[categoryName] ?: MaterialTheme.colorScheme.primary
                    )
                }
            )

            // Icon Grid
            Text(
                text = "Select Icon",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryIcons.icons.forEach { (name, icon) ->
                    val isSelected = selectedIconName == name

                    // FETCH THE COLOR: Pull the specific color for this category name
                    // If name is not found in the map, it defaults to VeridianGreenPrimary
                    val categoryColor = CategoryIcons.iconColors[name] ?: MaterialTheme.colorScheme.primary

                    Surface(
                        onClick = { selectedIconName = name },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(44.dp),
                        // POLISH: Use the category color for background with low alpha when selected
                        color = if (isSelected) categoryColor.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        // POLISH: Border matches the actual category color
                        border = if (isSelected) BorderStroke(2.dp, categoryColor) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                modifier = Modifier.size(20.dp),
                                // FIX: Tint the icon with its specific category color
                                tint = categoryColor
                            )
                        }
                    }
                }
            }

            // Save Button: Contextual Color
            Button(
                onClick = { onSaveCategory(categoryName, selectedIconName, categoryType) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = categoryName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(if (isEditMode) "Update Category" else "Add Category")
            }
        }
    }
}
