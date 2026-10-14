package com.example.expensetrackerpro.presentation.transactions.addEditTransactions

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.expensetrackerpro.presentation.common.haptics.rememberHaptics
import com.example.expensetrackerpro.domain.models.DateChoice
import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.transaction.ValidateTransactionDraftUseCase
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorSheet
import com.example.expensetrackerpro.presentation.common.filter.components.PillSegmentedControl
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.AmountHero
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.AmountSummary
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.BillAmountChips
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.BillFilledBanner
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.BillTotalMissingNotice
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.CategorySection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.DateSelectionSection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.NoteSection
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.SaveExpenseButton
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.ScanBillCard
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.TitleField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.TransactionTopBar
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.components.transactionDateLabel
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEffect
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionEvent
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.scan.rememberBillScanner
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEditTransactionUiState
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftField
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.DraftFill
import com.example.expensetrackerpro.utils.CurrencyFormatter
import kotlinx.coroutines.launch

@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    val haptics = rememberHaptics()
    val launchBillScanner = rememberBillScanner(
        onScanned = { viewModel.onEvent(AddEditTransactionEvent.BillScanned(it)) },
        onUnavailable = { viewModel.onEvent(AddEditTransactionEvent.BillScannerFailed) }
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // Sent only once the save or delete has succeeded
                AddEditTransactionEffect.Saved -> {
                    haptics.confirm()
                    currentOnNavigateBack()
                }
                AddEditTransactionEffect.NavigateBack -> currentOnNavigateBack()
                is AddEditTransactionEffect.ShowError -> launch {
                    snackbarHostState.showSnackbar(context.getString(effect.messageRes))
                }
                is AddEditTransactionEffect.BillUnreadable -> launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(effect.messageRes),
                        actionLabel = context.getString(R.string.bill_scan_try_again)
                    )
                    if (result == SnackbarResult.ActionPerformed) launchBillScanner()
                }
            }
        }
    }

    AddEditTransactionContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onScanBillClick = launchBillScanner,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionContent(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onScanBillClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    // Purely visual: which of the text fields below the amount has focus
    var isTitleFocused by remember { mutableStateOf(false) }
    var isNoteFocused by remember { mutableStateOf(false) }

    AddEditTransactionLayout(
        state = state,
        onEvent = onEvent,
        onNavigateBack = onNavigateBack,
        onScanBillClick = onScanBillClick,
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
    onScanBillClick: () -> Unit,
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

    state.categoryEditor?.let { editor ->
        CategoryEditorSheet(
            state = editor,
            onEvent = { onEvent(AddEditTransactionEvent.CategoryEditor(it)) }
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
                BillScanSection(state = state, onEvent = onEvent, onScanBillClick = onScanBillClick)
                AmountHero(
                    amount = state.amount,
                    transactionType = state.transactionType,
                    onAmountChange = { onEvent(AddEditTransactionEvent.AmountChanged(it)) },
                    focusRequester = amountFocusRequester,
                    isFromBill = state.isFromDraft(DraftField.AMOUNT)
                )
                // A single amount is already in the field, so there is nothing to pick from
                state.draftFill?.amountOptions?.takeIf { it.size > 1 || state.draftFill.isAmountMissing }?.let {
                    BillAmountChips(
                        amounts = it,
                        selected = state.validAmount,
                        onAmountSelected = { amount -> onEvent(AddEditTransactionEvent.BillAmountSelected(amount)) }
                    )
                }
            }

            CategorySection(
                categories = state.availableCategories,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = { onEvent(AddEditTransactionEvent.CategorySelected(it)) },
                onAddCategoryClicked = { onEvent(AddEditTransactionEvent.CategorySheetOpened) },
                isSuggested = state.isFromDraft(DraftField.CATEGORY)
            )

            TitleField(
                title = state.title,
                transactionType = state.transactionType,
                isError = state.isTitleErrorVisible,
                onTitleChange = { onEvent(AddEditTransactionEvent.TitleChanged(it)) },
                onFocusChange = onTitleFocusChange,
                isFromBill = state.isFromDraft(DraftField.TITLE)
            )

            DateSelectionSection(
                dateChoice = state.dateChoice,
                dateMillis = state.dateMillis,
                pickerUtcMillis = state.pickerUtcMillis,
                maxPickerUtcMillis = state.maxPickerUtcMillis,
                onDateChoiceSelected = { onEvent(AddEditTransactionEvent.DateChoiceSelected(it)) },
                onDatePicked = { onEvent(AddEditTransactionEvent.DatePicked(it)) },
                isFromBill = state.isFromDraft(DraftField.DATE)
            )

            NoteSection(
                note = state.note,
                maxLength = ValidateTransactionDraftUseCase.NOTE_MAX_LENGTH,
                isExpanded = state.isNoteExpanded,
                onExpand = { onEvent(AddEditTransactionEvent.NoteExpanded) },
                onNoteChange = { onEvent(AddEditTransactionEvent.NoteChanged(it)) },
                onRemove = { onEvent(AddEditTransactionEvent.NoteRemoved) },
                onFocusChange = onNoteFocusChange,
                isFromBill = state.isFromDraft(DraftField.NOTE)
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Under the type switch: the way into the scanner, or once a bill has filled the form, the
 * banner saying so. Nothing at all where scanning is not offered.
 */
@Composable
private fun BillScanSection(
    state: AddEditTransactionUiState,
    onEvent: (AddEditTransactionEvent) -> Unit,
    onScanBillClick: () -> Unit
) {
    val fill = state.draftFill

    when {
        fill != null -> {
            BillFilledBanner(onClear = { onEvent(AddEditTransactionEvent.BillFillCleared) })
            if (fill.isAmountMissing) BillTotalMissingNotice()
        }
        state.isScanAvailable -> ScanBillCard(isReading = state.isReadingBill, onClick = onScanBillClick)
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

// As the form looks once a bill has been read: every field found, and two other amounts to pick
private val previewScannedState = previewAddState.copy(
    title = "Cafe Coffee Day",
    amount = "1428",
    validAmount = Money.ofRupees(1428),
    note = "Bill no. 4521",
    isNoteExpanded = true,
    selectedCategoryId = PreviewData.categories.first().id,
    dateChoice = DateChoice.OTHER,
    isSaveEnabled = true,
    isScanAvailable = true,
    draftFill = DraftFill(
        filledFields = DraftField.entries.toSet(),
        amountOptions = listOf(Money.ofRupees(1428), Money.ofRupees(1360), Money.ofRupees(690))
    )
)

private val previewEditState = AddEditTransactionUiState(
    title = "Groceries",
    amount = "1850.5",
    validAmount = Money(1_850_50),
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
            onScanBillClick = {},
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
private fun ScanAvailablePreview() {
    AddEditTransactionPreview(state = previewAddState.copy(isScanAvailable = true))
}

@PreviewLightDark
@Composable
private fun FilledFromBillPreview() {
    AddEditTransactionPreview(state = previewScannedState)
}

@PreviewLightDark
@Composable
private fun BillTotalMissingPreview() {
    AddEditTransactionPreview(
        state = previewScannedState.copy(
            amount = "",
            validAmount = null,
            isSaveEnabled = false,
            draftFill = DraftFill(
                filledFields = DraftField.entries.toSet() - DraftField.AMOUNT,
                amountOptions = listOf(Money.ofRupees(1360), Money.ofRupees(690), Money.ofRupees(380)),
                isAmountMissing = true
            )
        )
    )
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
            validAmount = Money.ofRupees(250),
            selectedCategoryId = PreviewData.categories.first().id,
            isTitleErrorVisible = true
        )
    )
}
