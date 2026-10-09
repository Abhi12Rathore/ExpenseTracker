package com.example.expensetrackerpro.presentation.transactions.addEditTransactions

import DeleteConfirmationDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.presentation.categories.AddCategoryBottomSheet
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.AmountHero
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.AmountSummary
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.CategorySection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.DateSelectionSection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.NoteSection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.SaveExpenseButton
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.TitleField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.TransactionTopBar
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.transactionDateLabel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionError
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.utils.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // Sent only once the save or delete has succeeded
                AddEditTransactionEffect.NavigateBack -> currentOnNavigateBack()
                is AddEditTransactionEffect.ShowError -> launch {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            when (effect.error) {
                                AddEditTransactionError.SAVE_FAILED -> R.string.transaction_save_failed
                                AddEditTransactionError.DELETE_FAILED -> R.string.transaction_delete_failed
                            }
                        )
                    )
                }
            }
        }
    }

    AddEditTransactionContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionContent(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // Purely visual: which of the text fields below the amount has focus
    var isTitleFocused by remember { mutableStateOf(false) }
    var isNoteFocused by remember { mutableStateOf(false) }

    AddEditTransactionLayout(
        state = state,
        onEvent = onEvent,
        onNavigateBack = onNavigateBack,
        snackbarHostState = snackbarHostState,
        // Typing a title or note leaves little room, so the amount shrinks to a summary line
        isAmountCollapsed = WindowInsets.isImeVisible && (isTitleFocused || isNoteFocused),
        onTitleFocusChange = { isTitleFocused = it },
        onNoteFocusChange = { isNoteFocused = it }
    )
}

@Composable
private fun AddEditTransactionLayout(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onNavigateBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    isAmountCollapsed: Boolean,
    onTitleFocusChange: (Boolean) -> Unit,
    onNoteFocusChange: (Boolean) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val amountFocusRequester = remember { FocusRequester() }

    // A new transaction starts with the keypad up, ready for the amount
    LaunchedEffect(Unit) {
        if (!state.isEditMode && !isAmountCollapsed) amountFocusRequester.requestFocus()
    }

    if (state.isCategorySheetVisible) {
        AddCategoryBottomSheet(
            initialType = state.transactionType,
            onDismiss = { onEvent(AddEditTransactionEvent.CategorySheetDismissed) },
            onSaveCategory = { name, icon, type ->
                onEvent(AddEditTransactionEvent.CategoryAdded(name, icon, type))
            }
        )
    }

    state.transactionToDelete?.let { transaction ->
        DeleteConfirmationDialog(
            expense = transaction,
            onConfirm = { onEvent(AddEditTransactionEvent.DeleteConfirmed) },
            onDismiss = { onEvent(AddEditTransactionEvent.DeleteDismissed) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar(
                isEditMode = state.isEditMode,
                transactionType = state.transactionType,
                onNavigateBack = onNavigateBack,
                onDeleteClick = { onEvent(AddEditTransactionEvent.DeleteClicked) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Sticks to the bottom, and rides above the keyboard while it is open
            Surface(color = MaterialTheme.colorScheme.background) {
                SaveExpenseButton(
                    isEnabled = state.isSaveEnabled,
                    onSaveClick = { onEvent(AddEditTransactionEvent.SaveClicked) },
                    buttonLabel = saveLabel(state),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val expenseLabel = stringResource(R.string.transaction_type_expense)
            val incomeLabel = stringResource(R.string.transaction_type_income)

            if (isAmountCollapsed) {
                AmountSummary(
                    amount = state.validAmount,
                    transactionType = state.transactionType,
                    categoryName = state.selectedCategory?.name,
                    dateLabel = transactionDateLabel(state.dateChoice, state.dateMillis),
                    // Closing the keyboard brings the full amount field back
                    onClick = { focusManager.clearFocus() }
                )
            } else {
                PillSegmentedControl(
                    options = listOf(TransactionType.EXPENSE, TransactionType.INCOME),
                    selected = state.transactionType,
                    label = { if (it == TransactionType.INCOME) incomeLabel else expenseLabel },
                    onSelect = { onEvent(AddEditTransactionEvent.TypeChanged(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
                AmountHero(
                    amount = state.amount,
                    transactionType = state.transactionType,
                    onAmountChange = { onEvent(AddEditTransactionEvent.AmountChanged(it)) },
                    focusRequester = amountFocusRequester
                )
            }

            CategorySection(
                categories = state.availableCategories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = { onEvent(AddEditTransactionEvent.CategorySelected(it)) },
                onAddCategoryClicked = { onEvent(AddEditTransactionEvent.CategorySheetOpened) }
            )

            TitleField(
                title = state.title,
                transactionType = state.transactionType,
                isError = state.isTitleErrorVisible,
                onTitleChange = { onEvent(AddEditTransactionEvent.TitleChanged(it)) },
                onFocusChange = onTitleFocusChange
            )

            DateSelectionSection(
                dateChoice = state.dateChoice,
                dateMillis = state.dateMillis,
                pickerUtcMillis = state.pickerUtcMillis,
                maxPickerUtcMillis = state.maxPickerUtcMillis,
                onDateChoiceSelected = { onEvent(AddEditTransactionEvent.DateChoiceSelected(it)) },
                onDatePicked = { onEvent(AddEditTransactionEvent.DatePicked(it)) }
            )

            NoteSection(
                note = state.note,
                maxLength = ValidateTransactionDraftUseCase.NOTE_MAX_LENGTH,
                isExpanded = state.isNoteExpanded,
                onExpand = { onEvent(AddEditTransactionEvent.NoteExpanded) },
                onNoteChange = { onEvent(AddEditTransactionEvent.NoteChanged(it)) },
                onRemove = { onEvent(AddEditTransactionEvent.NoteRemoved) },
                onFocusChange = onNoteFocusChange
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/** "Save ₹1,234.50" once there is a valid amount, plain "Save" until then. */
@Composable
private fun saveLabel(state: AddEditTransactionUiState): String {
    val amount = state.validAmount?.let { CurrencyFormatter.format(it) }

    return when {
        state.isEditMode && amount != null -> stringResource(R.string.transaction_update_amount, amount)
        state.isEditMode -> stringResource(R.string.transaction_update)
        amount != null -> stringResource(R.string.transaction_save_amount, amount)
        else -> stringResource(R.string.transaction_save)
    }
}

private val previewAddState = AddEditTransactionUiState(
    dateMillis = PreviewData.DATE_MILLIS,
    availableCategories = PreviewData.categories
)

private val previewEditState = AddEditTransactionUiState(
    title = "Groceries",
    amount = "1850.5",
    validAmount = 1850.5,
    note = "Weekly shop, plus the cake for Sunday",
    isNoteExpanded = true,
    selectedCategoryId = PreviewData.categories.first().id,
    dateMillis = PreviewData.DATE_MILLIS,
    dateChoice = DateChoice.OTHER,
    isSaveEnabled = true,
    isEditMode = true,
    availableCategories = PreviewData.categories
)

@Composable
private fun AddEditTransactionPreview(
    state: AddEditTransactionUiState,
    isAmountCollapsed: Boolean = false
) {
    ExpenseTrackerProTheme {
        AddEditTransactionLayout(
            state = state,
            onEvent = {},
            onNavigateBack = {},
            snackbarHostState = remember { SnackbarHostState() },
            isAmountCollapsed = isAmountCollapsed,
            onTitleFocusChange = {},
            onNoteFocusChange = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun AddTransactionPreview() {
    AddEditTransactionPreview(state = previewAddState)
}

@PreviewLightDark
@Composable
private fun EditTransactionWithNotePreview() {
    AddEditTransactionPreview(state = previewEditState)
}

@PreviewLightDark
@Composable
private fun KeyboardOpenPreview() {
    AddEditTransactionPreview(state = previewEditState, isAmountCollapsed = true)
}

@PreviewLightDark
@Composable
private fun TitleErrorPreview() {
    AddEditTransactionPreview(
        state = previewAddState.copy(
            amount = "250",
            validAmount = 250.0,
            selectedCategoryId = PreviewData.categories.first().id,
            isTitleErrorVisible = true
        )
    )
}
