package com.example.expensetrackerpro.presentation.manageCategories.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.manageCategories.uistate.CategoryDeleteRequest
import com.example.expensetrackerpro.ExpenseTrackerProTheme

/**
 * Confirms deleting a category. One that has transactions first asks which category of the same
 * type they should move to.
 */
@Composable
fun DeleteCategoryDialog(
    request: CategoryDeleteRequest,
    onTargetSelected: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.category_delete_dialog_title, request.category.name)) },
        text = {
            when {
                !request.needsReassign -> Text(stringResource(R.string.category_delete_dialog_message))
                request.targets.isEmpty() -> Text(
                    pluralStringResource(
                        R.plurals.category_delete_no_target,
                        request.transactionCount,
                        request.transactionCount
                    )
                )
                else -> ReassignTargets(
                    request = request,
                    onTargetSelected = onTargetSelected
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = request.isConfirmEnabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    stringResource(
                        if (request.needsReassign) R.string.category_delete_move_confirm
                        else R.string.category_delete_confirm
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.category_delete_cancel))
            }
        }
    )
}

@Composable
private fun ReassignTargets(
    request: CategoryDeleteRequest,
    onTargetSelected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            pluralStringResource(
                R.plurals.category_delete_move_message,
                request.transactionCount,
                request.transactionCount
            )
        )
        // Scrolls on its own, so a long list never pushes the buttons off the dialog
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 280.dp)
                .selectableGroup()
        ) {
            items(request.targets, key = { it.id }) { target ->
                TargetRow(
                    category = target,
                    isSelected = target.id == request.selectedTargetId,
                    onClick = { onTargetSelected(target.id) }
                )
            }
        }
    }
}

@Composable
private fun TargetRow(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // The whole row is the click target, so the button itself takes no clicks
        RadioButton(selected = isSelected, onClick = null)
        CategoryIconTile(icon = category.icon, size = 32.dp)
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@PreviewLightDark
@Composable
private fun DeleteCategoryDialogReassignPreview() {
    val categories = PreviewData.categories

    ExpenseTrackerProTheme {
        DeleteCategoryDialog(
            request = CategoryDeleteRequest(
                category = categories.first(),
                transactionCount = 12,
                targets = categories.drop(1),
                selectedTargetId = categories[1].id
            ),
            onTargetSelected = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun DeleteCategoryDialogEmptyPreview() {
    ExpenseTrackerProTheme {
        DeleteCategoryDialog(
            request = CategoryDeleteRequest(category = PreviewData.categories.last()),
            onTargetSelected = {},
            onConfirm = {},
            onDismiss = {}
        )
    }
}
