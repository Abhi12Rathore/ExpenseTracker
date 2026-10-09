package com.example.expensetrackerpro.presentation.transactions.transactionList

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import DeleteConfirmationDialog
import androidx.compose.runtime.Composable


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetrackerpro.R
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.presentation.common.filter.components.PeriodFilter
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.EmptyExpenseUI
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.ExpenseTile
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TotalBalanceCard
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListEvent
import com.example.expensetrackerpro.presentation.transactions.transactionList.uistate.TransactionListUiState
import com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel.TransactionListViewModel
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.presentation.common.preview.PreviewData

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Int) -> Unit,
    onNavigateToManageCategories: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            // Launched separately so a snackbar on screen doesn't hold up the next effect
            launch {
                when (effect) {
                    is TransactionListEffect.ShowUndoDelete -> {
                        // A newer delete replaces the snackbar of the one before
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.history_transaction_deleted),
                            actionLabel = context.getString(R.string.history_undo),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.onEvent(TransactionListEvent.UndoDelete(effect.transaction))
                        }
                    }
                    TransactionListEffect.ShowRestoreFailed -> snackbarHostState.showSnackbar(
                        context.getString(R.string.history_restore_failed)
                    )
                }
            }
        }
    }

    TransactionListContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
        onAddExpenseClick = onAddExpenseClick,
        onEditExpenseClick = onEditExpenseClick,
        onNavigateToManageCategories = onNavigateToManageCategories
    )
}

@Composable
fun TransactionListContent(
    uiState: TransactionListUiState,
    onEvent: (TransactionListEvent) -> Unit,
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Int) -> Unit,
    onNavigateToManageCategories: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val expenses = uiState.transactions
    val expenseToDelete = uiState.transactionToDelete

    // Show Dialog if state is not null

    expenseToDelete?.let { expense ->
        DeleteConfirmationDialog(
            expense = expense,
            onConfirm = { onEvent(TransactionListEvent.DeleteConfirmed) },
            onDismiss = { onEvent(TransactionListEvent.DeleteDismissed) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpenseClick()}, // Opens the menu
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Actions"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 1. Header Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Expense List",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track your spending",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { /* Filter logic */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { onEvent(TransactionListEvent.SearchChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onEvent(TransactionListEvent.SearchChanged("")) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.history_search_clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                PeriodFilter(
                    state = uiState.filter,
                    onEvent = { onEvent(TransactionListEvent.Filter(it)) }
                )
            }

            uiState.categoryFilter?.let { category ->
                item {
                    InputChip(
                        selected = true,
                        onClick = { onEvent(TransactionListEvent.CategoryFilterCleared) },
                        label = { Text(category.name) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.history_clear_category_filter),
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }

            // 2. Section Title: Expenses + Count
            if (!expenses.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${expenses.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }


            // 3. Reactive List Logic
            if (expenses.isEmpty()) {
                item {
                    if (uiState.searchQuery.isBlank()) {
                        // Show Empty UI if no data
                        EmptyExpenseUI()
                    } else {
                        NoSearchMatches(query = uiState.searchQuery.trim())
                    }
                }
            } else {
                items(expenses) { expense ->
                    ExpenseTile(
                        transaction = expense,
                        onEditClick = { onEditExpenseClick.invoke(expense.id)},
                        onDeleteClick = {
                            onEvent(TransactionListEvent.DeleteRequested(expense))
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
        }
    }
}

@Composable
private fun NoSearchMatches(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.history_no_matches_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.history_no_matches_description, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@PreviewLightDark
@Composable
private fun TransactionListContentPreview() {
    ExpenseTrackerProTheme {
        TransactionListContent(
            uiState = TransactionListUiState(transactions = PreviewData.transactions),
            onEvent = {},
            onAddExpenseClick = {},
            onEditExpenseClick = {},
            onNavigateToManageCategories = {}
        )
    }
}
