package com.example.expensetrackerpro.presentation.manageCategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.onFailure
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.usecase.category.DeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoryUsageUseCase
import com.example.expensetrackerpro.domain.usecase.category.ReassignAndDeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.SaveCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.SectionCategoryUsageUseCase
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorEvent
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorReducer
import com.example.expensetrackerpro.presentation.common.category.editor.CategoryEditorState
import com.example.expensetrackerpro.presentation.common.error.toMessageRes
import com.example.expensetrackerpro.presentation.manageCategories.uistate.CategoryDeleteRequest
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageCategoryViewModel @Inject constructor(
    getCategoryUsage: GetCategoryUsageUseCase,
    private val sectionCategoryUsage: SectionCategoryUsageUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val reassignAndDeleteCategory: ReassignAndDeleteCategoryUseCase,
    private val categoryEditorReducer: CategoryEditorReducer
) : ViewModel() {

    // Tab, search, sheet and dialog state; the category list is merged in below
    private val screenState = MutableStateFlow(ManageCategoryUiState())

    // The latest usage of every category, of both types, for the sheet and the delete dialog
    private var usages: List<CategoryUsage> = emptyList()

    private val categories: List<Category> get() = usages.map { it.category }

    val uiState: StateFlow<ManageCategoryUiState> =
        combine(
            getCategoryUsage().onEach { latest ->
                usages = latest
                // A name that was free may have been taken while the sheet was open
                updateEditor { categoryEditorReducer.validated(it, categories) }
            },
            screenState
        ) { allUsages, state ->
            state.copy(sections = sectionCategoryUsage(allUsages, state.selectedType, state.searchQuery))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageCategoryUiState())

    fun onEvent(event: ManageCategoryEvent) {
        when (event) {
            ManageCategoryEvent.SearchOpened ->
                screenState.update { it.copy(isSearchOpen = true) }
            ManageCategoryEvent.SearchClosed ->
                screenState.update { it.copy(isSearchOpen = false, searchQuery = "") }
            is ManageCategoryEvent.SearchQueryChanged ->
                screenState.update { it.copy(searchQuery = event.query) }
            is ManageCategoryEvent.TypeChanged ->
                screenState.update { it.copy(selectedType = event.type) }
            ManageCategoryEvent.AddClicked -> screenState.update {
                it.copy(categoryEditor = categoryEditorReducer.open(it.selectedType, existing = categories))
            }
            is ManageCategoryEvent.CategoryClicked -> screenState.update {
                it.copy(
                    categoryEditor = categoryEditorReducer.open(
                        type = event.category.type,
                        category = event.category,
                        existing = categories
                    )
                )
            }
            is ManageCategoryEvent.Editor -> onEditorEvent(event.event)
            is ManageCategoryEvent.DeleteTargetSelected ->
                updatePendingDelete { it.copy(selectedTargetId = event.categoryId) }
            ManageCategoryEvent.DeleteConfirmed -> confirmDelete()
            ManageCategoryEvent.DeleteDismissed ->
                screenState.update { it.copy(pendingDelete = null) }
            ManageCategoryEvent.MessageShown ->
                screenState.update { it.copy(message = null) }
        }
    }

    private fun onEditorEvent(event: CategoryEditorEvent) {
        when (event) {
            CategoryEditorEvent.Dismissed -> closeEditor()
            CategoryEditorEvent.SaveClicked -> save()
            CategoryEditorEvent.DeleteClicked -> requestDelete()
            else -> updateEditor { categoryEditorReducer.reduce(it, event, categories) }
        }
    }

    private fun save() {
        val editor = screenState.value.categoryEditor ?: return
        if (!editor.isSaveEnabled) return

        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveCategory(editor.toDraft())) {
                is Outcome.Success -> closeEditor()
                is Outcome.Failure -> when (result.error) {
                    // Deleted while the sheet was open, so there is nothing left to edit
                    DomainError.NotFound -> closeEditor()
                    else -> updateEditor { categoryEditorReducer.saveFailed(it, result.error) }
                }
            }
        }
    }

    /** The sheet's delete button: the sheet makes way for a dialog that confirms it. */
    private fun requestDelete() {
        val category = screenState.value.categoryEditor?.editing ?: return

        screenState.update {
            it.copy(categoryEditor = null, pendingDelete = deleteRequestFor(category))
        }
    }

    private fun deleteRequestFor(category: Category) = CategoryDeleteRequest(
        category = category,
        transactionCount = usages.find { it.category.id == category.id }?.totalCount ?: 0,
        targets = categories
            .filter { it.type == category.type && it.id != category.id }
            .sortedBy { it.name.lowercase() }
    )

    private fun confirmDelete() {
        val request = screenState.value.pendingDelete ?: return
        if (!request.isConfirmEnabled) return

        screenState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val target = request.selectedTarget
            val result = if (target != null) {
                reassignAndDeleteCategory(request.category, target)
            } else {
                deleteCategory(request.category)
            }

            result.onFailure { error ->
                if (error == DomainError.CategoryInUse && target == null) {
                    // It gained a transaction since the dialog opened, so they have to be moved after all
                    screenState.update {
                        val latest = deleteRequestFor(request.category)
                        it.copy(pendingDelete = latest.copy(transactionCount = latest.transactionCount.coerceAtLeast(1)))
                    }
                } else {
                    screenState.update { it.copy(message = error.toMessageRes()) }
                }
            }
        }
    }

    private fun closeEditor() {
        screenState.update { it.copy(categoryEditor = null) }
    }

    /** Changes the sheet's form, if the sheet is open. */
    private fun updateEditor(change: (CategoryEditorState) -> CategoryEditorState) {
        screenState.update { it.copy(categoryEditor = it.categoryEditor?.let(change)) }
    }

    private fun updatePendingDelete(change: (CategoryDeleteRequest) -> CategoryDeleteRequest) {
        screenState.update { it.copy(pendingDelete = it.pendingDelete?.let(change)) }
    }
}
