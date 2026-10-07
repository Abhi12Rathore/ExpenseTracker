package com.example.expensetrackerpro.presentation.transactions.addEditTransactions


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.presentation.categories.AddCategoryBottomSheet
import com.example.expensetrackerpro.presentation.transactions.CategorySection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    // Observe the reactively filtered categories from the ViewModel
    val categories by viewModel.availableCategories.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }

    if (showBottomSheet) {
        AddCategoryBottomSheet(
            // Pass the current transaction type (Income/Expense) from the screen state
            initialType = state.transactionType,
            onDismiss = { showBottomSheet = false },
            onSaveCategory = { name, iconName, type ->
                // Pass the type to the ViewModel so it's saved correctly in the DB
                viewModel.addCategory(name, iconName, type)
                showBottomSheet = false
            }
        )
    }

    val scrollState = rememberScrollState()

    // Dynamic Titles based on Transaction Type and Edit Mode
    val typeText = if (state.transactionType == TransactionType.INCOME) "Income" else "Expense"
    val titleText = if (viewModel.isEditMode) "Edit $typeText" else "Add $typeText"
    val buttonText = if (viewModel.isEditMode) "Update $typeText" else "Save $typeText"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ExpenseHeader(onBackClick = onNavigateBack, title = titleText,selectedType = state.transactionType,)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- NEW: Transaction Type Toggle ---
            // This is the core migration change: switching between Income and Expense
            TransactionTypeToggle(
                selectedType = state.transactionType,
                onTypeSelected = { viewModel.onTypeChange(it) }
            )

            // Input Fields Card (Updated to use 'note' instead of 'title')
            ExpenseDetailCard(
                title = state.note,
                amount = state.amount,
                onTitleChange = viewModel::onNoteChange,
                onAmountChange = viewModel::onAmountChange,
                selectedType = state.transactionType
            )

            // Category Selection (Updated to handle Category Object)
            CategorySection(
                categories = categories,
                // Note: state.selectedCategory is now a Category object, we pass the name for UI
                selectedCategory = state.selectedCategory?.name ?: "",
                onCategorySelected = { categoryName ->
                    // Find the object from the list to pass back to VM
                    val selectedObj = categories.find { it.name == categoryName }
                    selectedObj?.let { viewModel.onCategorySelect(it) }
                },
                onAddCategoryClicked = { showBottomSheet = true }
            )

            // Date Selection
            DateSelectionSection(
                selectedDateMillis = state.dateMillis,
                onDateSelected = viewModel::onDateSelected
            )

            // Preview Section (Updated for polymorphic types)
            AddExpensePreview(
                amount = state.amount,
                category = state.selectedCategory?.name ?: "None",
                dateMillis = state.dateMillis,
                type = state.transactionType, // Ensure your preview component handles color based on type
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            SaveExpenseButton(
                isEnabled = state.isSaveEnabled,
                onSaveClick = {
                    viewModel.saveTransaction()
                    onNavigateBack()
                },
                modifier = Modifier.navigationBarsPadding(),
                buttonLabel = buttonText
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
