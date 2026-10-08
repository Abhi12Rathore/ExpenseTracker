package com.example.expensetrackerpro.presentation.transactions.transactionList

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import DeleteConfirmationDialog
import androidx.compose.runtime.Composable


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.EmptyExpenseUI
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.ExpenseTile
import com.example.expensetrackerpro.presentation.transactions.transactionList.component.TotalBalanceCard
import com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel.TransactionListViewModel

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddExpenseClick: () -> Unit,
    onEditExpenseClick: (Int) -> Unit,
    onNavigateToManageCategories: () -> Unit,
) {
    val expenses by viewModel.transactions.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val totalBalance by viewModel.totalBalance.collectAsState() // Income - Expense
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()

    val expenseToDelete by viewModel.transactionToDelete.collectAsState()

    // Show Dialog if state is not null

    expenseToDelete?.let { expense ->
        DeleteConfirmationDialog(
            expense = expense,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddExpenseClick()}, // Opens the menu
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
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
                    Row {
                        IconButton(onClick = { /* Search logic */ }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { /* Filter logic */ }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
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
                    // Show Empty UI if no data
                    EmptyExpenseUI()
                }
            } else {
                items(expenses) { expense ->
                    ExpenseTile(
                        transaction = expense,
                        onEditClick = { onEditExpenseClick.invoke(expense.id)},
                        onDeleteClick = {
                            viewModel.showDeleteConfirmation(expense)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // Space for FAB
        }
    }
}