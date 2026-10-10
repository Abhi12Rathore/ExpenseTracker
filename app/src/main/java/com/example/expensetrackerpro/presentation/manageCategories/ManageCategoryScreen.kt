package com.example.expensetrackerpro.presentation.manageCategories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategorySections
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorSheet
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.manageCategories.components.CategoryManageTile
import com.example.expensetrackerpro.presentation.manageCategories.components.DeleteCategoryDialog
import com.example.expensetrackerpro.presentation.manageCategories.components.ManageCategoriesTopBar
import com.example.expensetrackerpro.presentation.manageCategories.uistate.CategoryDeleteRequest
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryMessage
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryUiState
import com.example.expensetrackerpro.ExpenseTrackerProTheme

@Composable
fun ManageCategoryScreen(
    viewModel: ManageCategoryViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ManageCategoryContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun ManageCategoryContent(
    uiState: ManageCategoryUiState,
    onEvent: (ManageCategoryEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val sections = uiState.sections

    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = when (uiState.message) {
        ManageCategoryMessage.DELETE_FAILED -> stringResource(R.string.category_delete_failed)
        null -> null
    }
    LaunchedEffect(uiState.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        onEvent(ManageCategoryEvent.MessageShown)
    }

    uiState.categoryEditor?.let { editor ->
        CategoryEditorSheet(
            state = editor,
            onEvent = { onEvent(ManageCategoryEvent.Editor(it)) }
        )
    }

    uiState.pendingDelete?.let { request ->
        DeleteCategoryDialog(
            request = request,
            onTargetSelected = { onEvent(ManageCategoryEvent.DeleteTargetSelected(it)) },
            onConfirm = { onEvent(ManageCategoryEvent.DeleteConfirmed) },
            onDismiss = { onEvent(ManageCategoryEvent.DeleteDismissed) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ManageCategoriesTopBar(
                isSearchOpen = uiState.isSearchOpen,
                searchQuery = uiState.searchQuery,
                onNavigateBack = onNavigateBack,
                onOpenSearch = { onEvent(ManageCategoryEvent.SearchOpened) },
                onSearchChange = { onEvent(ManageCategoryEvent.SearchQueryChanged(it)) },
                onCloseSearch = { onEvent(ManageCategoryEvent.SearchClosed) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.manage_categories_new)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { onEvent(ManageCategoryEvent.AddClicked) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val expenseLabel = stringResource(
                R.string.manage_categories_tab,
                stringResource(R.string.transaction_type_expense),
                sections.expenseCount
            )
            val incomeLabel = stringResource(
                R.string.manage_categories_tab,
                stringResource(R.string.transaction_type_income),
                sections.incomeCount
            )
            PillSegmentedControl(
                options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                selected = uiState.selectedType,
                label = { if (it == TransactionType.INCOME) incomeLabel else expenseLabel },
                onSelect = { onEvent(ManageCategoryEvent.TypeChanged(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (sections.isEmpty) {
                EmptyMessage(searchQuery = uiState.searchQuery.trim())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Room for the last row to scroll clear of the button
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    categorySection(
                        titleRes = R.string.manage_categories_section_most_used,
                        usages = sections.mostUsed,
                        onClick = { onEvent(ManageCategoryEvent.CategoryClicked(it)) }
                    )
                    categorySection(
                        titleRes = R.string.manage_categories_section_unused,
                        usages = sections.unused,
                        onClick = { onEvent(ManageCategoryEvent.CategoryClicked(it)) }
                    )
                }
            }
        }
    }
}

/** A heading and its rows; nothing at all when [usages] is empty. */
private fun LazyListScope.categorySection(
    titleRes: Int,
    usages: List<CategoryUsage>,
    onClick: (Category) -> Unit
) {
    if (usages.isEmpty()) return

    item(key = "header-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 8.dp, start = 4.dp)
                .semantics { heading() }
                .animateItem()
        )
    }
    items(usages, key = { "category-${it.category.id}" }) { usage ->
        CategoryManageTile(
            usage = usage,
            onClick = { onClick(usage.category) },
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun EmptyMessage(searchQuery: String) {
    Text(
        text = if (searchQuery.isEmpty()) {
            stringResource(R.string.manage_categories_empty)
        } else {
            stringResource(R.string.manage_categories_no_results, searchQuery)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
    )
}

private val previewSections = CategorySections(
    expenseCount = PreviewData.categoryUsages.size,
    incomeCount = 9,
    mostUsed = PreviewData.categoryUsages.filter { it.periodCount > 0 },
    unused = PreviewData.categoryUsages.filter { it.periodCount == 0 }
)

@PreviewLightDark
@Composable
private fun ManageCategoryContentPreview() {
    ExpenseTrackerProTheme {
        ManageCategoryContent(
            uiState = ManageCategoryUiState(sections = previewSections),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ManageCategoryContentSearchPreview() {
    ExpenseTrackerProTheme {
        ManageCategoryContent(
            uiState = ManageCategoryUiState(
                sections = previewSections.copy(mostUsed = emptyList(), unused = emptyList()),
                isSearchOpen = true,
                searchQuery = "gym"
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ManageCategoryContentReassignPreview() {
    val categories = PreviewData.categories

    ExpenseTrackerProTheme {
        ManageCategoryContent(
            uiState = ManageCategoryUiState(
                sections = previewSections,
                pendingDelete = CategoryDeleteRequest(
                    category = categories.first(),
                    transactionCount = 12,
                    targets = categories.drop(1)
                )
            ),
            onEvent = {},
            onNavigateBack = {}
        )
    }
}
