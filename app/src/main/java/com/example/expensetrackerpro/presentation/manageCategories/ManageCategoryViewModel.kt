package com.example.expensetrackerpro.presentation.manageCategories


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.domain.repository.CategoryRepository
import com.example.expensetrackerpro.domain.usecase.category.AddCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.DeleteCategoryUseCase
import com.example.expensetrackerpro.domain.usecase.category.UpdateCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.text.contains

@HiltViewModel
class ManageCategoryViewModel @Inject constructor(
    private val repository: CategoryRepository,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    // Fetch all categories from the repository
    // Internal states for filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Requirement: Expense visible by default
    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    val selectedType = _selectedType.asStateFlow()

    // This is the flow the UI MUST observe
    val filteredCategories = combine(
        repository.getAllCategories(), // Returns Flow<List<Category>>
        _selectedType,
        _searchQuery
    ) { allCategories, type, query ->
        allCategories.filter { category ->
            // 1. Filter by Type
            val matchesType = category.type == type

            // 2. Filter by Search (Exact/Contains check)
            val matchesSearch = if (query.length >= 3) {
                category.name.contains(query, ignoreCase = true)
            } else {
                true
            }

            matchesType && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTypeChange(type: TransactionType) {
        _selectedType.value = type
    }


    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addCategory(name: String, icon: String, type: TransactionType) {
        viewModelScope.launch {
            addCategoryUseCase(Category(name = name, iconName = icon, type = type))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            deleteCategoryUseCase(category)
        }
    }

    fun updateCategory(id: Int, name: String, icon: String,type: TransactionType) {
        viewModelScope.launch {
            // We use the existing ID to ensure Room updates the record instead of inserting a new one
            updateCategoryUseCase(
                Category(id = id, name = name,type= type, iconName = icon, isCustom = true)
            )
        }
    }
}