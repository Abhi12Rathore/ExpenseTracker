package com.example.expensetrackerpro.presentation.manageCategories

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.presentation.categories.AddCategoryBottomSheet
import com.example.expensetrackerpro.presentation.manageCategories.components.CategoryManageTile
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryUiState

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoryContent(
    uiState: ManageCategoryUiState,
    onEvent: (ManageCategoryEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val categories = uiState.categories
    val categoryToEdit = uiState.categoryToEdit
    val isSheetOpen = uiState.isSheetOpen
    val searchQuery = uiState.searchQuery
    val selectedType = uiState.selectedType

    // Purely visual: whether the type dropdown is showing
    var isFilterExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val deleteBlockedMessage = uiState.deleteBlockedCategoryName?.let {
        stringResource(R.string.category_delete_blocked, it)
    }
    LaunchedEffect(deleteBlockedMessage) {
        if (deleteBlockedMessage != null) {
            snackbarHostState.showSnackbar(deleteBlockedMessage)
            onEvent(ManageCategoryEvent.DeleteBlockedMessageShown)
        }
    }

    if (isSheetOpen) {
        AddCategoryBottomSheet(
            initialCategory = categoryToEdit,
            // Pass the context: If you have tabs, pass the active tab type.
            // If you are on the Add Transaction screen, pass the current state.transactionType
            initialType = selectedType,
            onDismiss = { onEvent(ManageCategoryEvent.SheetDismissed) },
            onSaveCategory = { name, icon, type ->
                // The ViewModel updates when a category is being edited, otherwise adds
                onEvent(ManageCategoryEvent.CategorySaved(name, icon, type))
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(ManageCategoryEvent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Header Section
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Manage Categories",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage all your categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Search and Filter Section
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { onEvent(ManageCategoryEvent.SearchQueryChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search categories...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onEvent(ManageCategoryEvent.SearchQueryChanged("")) }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Type Filter Dropdown
                Box {
                    FilterChip(
                        selected = true, // It's "selected" because it's showing the active filter
                        onClick = { isFilterExpanded = true },
                        modifier = Modifier.height(45.dp),
                        label = {
                            Text(
                                text = selectedType.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter Type", // Added required contentDescription
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        enabled = true, // Explicitly passed enabled
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedTrailingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = MaterialTheme.colorScheme.primary,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp
                        )
                    )

                    DropdownMenu(
                        expanded = isFilterExpanded,
                        onDismissRequest = { isFilterExpanded = false }
                    ) {
                        TransactionType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onEvent(ManageCategoryEvent.TypeChanged(type)) // This triggers the reactive filter
                                    isFilterExpanded = false
                                },
                                leadingIcon = {
                                    val icon = if (type == TransactionType.INCOME) Icons.Default.TrendingUp else Icons.Default.TrendingDown
                                    Icon(icon, contentDescription = null, tint = if (type == TransactionType.INCOME) Color(0xFF4CAF50) else Color.Red)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Result Info
            if (searchQuery.length in 1..2) {
                Text(
                    text = "Keep typing to search...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(categories) { category ->
                    CategoryManageTile(
                        category = category,
                        onEdit = { onEvent(ManageCategoryEvent.EditClicked(category)) },
                        onDelete = { onEvent(ManageCategoryEvent.DeleteClicked(category)) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 400),
                            fadeOutSpec = tween(durationMillis = 400),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )

                    )
                }
            }
        }
    }
}