package com.example.expensetrackerpro.presentation.manageCategories


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.DeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.FilterCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.GetCategoriesUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryEvent
import com.example.expensetrackerpro.presentation.manageCategories.uistate.ManageCategoryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ManageCategoryViewModel @Inject constructor(
    getCategories: GetCategoriesUseCase,
    private val filterCategories: FilterCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val updateCategory: UpdateCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase
) : ViewModel() {

    // Search, type filter and sheet state; the category list is merged in below
    private val screenState = MutableStateFlow(ManageCategoryUiState())

    val uiState: StateFlow<ManageCategoryUiState> =
        combine(getCategories(), screenState) { allCategories, state ->
            state.copy(
                categories = filterCategories(allCategories, state.selectedType, state.searchQuery)
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ManageCategoryUiState())

    fun onEvent(event: ManageCategoryEvent) {
        when (event) {
            is ManageCategoryEvent.SearchQueryChanged ->
                screenState.update { it.copy(searchQuery = event.query) }
            is ManageCategoryEvent.TypeChanged ->
                screenState.update { it.copy(selectedType = event.type) }
            ManageCategoryEvent.AddClicked ->
                // Ensure we are in "Add" mode
                screenState.update { it.copy(isSheetOpen = true, categoryToEdit = null) }
            is ManageCategoryEvent.EditClicked ->
                screenState.update { it.copy(isSheetOpen = true, categoryToEdit = event.category) }
            ManageCategoryEvent.SheetDismissed -> closeSheet()
            is ManageCategoryEvent.CategorySaved ->
                saveCategory(event.name, event.iconName, event.type)
            is ManageCategoryEvent.DeleteClicked ->
                viewModelScope.launch { deleteCategory(event.category) }
        }
    }

    private fun saveCategory(name: String, icon: String, type: TransactionType) {
        val categoryToEdit = screenState.value.categoryToEdit
        viewModelScope.launch {
            if (categoryToEdit != null) {
                // We use the existing ID to ensure Room updates the record instead of inserting a new one
                updateCategory(
                    Category(id = categoryToEdit.id, name = name, type = type, iconName = icon, isCustom = true)
                )
            } else {
                addCategory(Category(name = name, iconName = icon, type = type))
            }
        }
        closeSheet()
    }

    private fun closeSheet() {
        screenState.update { it.copy(isSheetOpen = false, categoryToEdit = null) }
    }
}
