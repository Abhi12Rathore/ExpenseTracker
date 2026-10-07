package com.example.expensetrackerpro.presentation.manageCategories


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.text.contains

class ManageCategoryViewModel(
    private val repository: TransactionRepository
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
            repository.insertCategory(Category(name = name, iconName = icon, type = type))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun updateCategory(id: Int, name: String, icon: String,type: TransactionType) {
        viewModelScope.launch {
            // We use the existing ID to ensure Room updates the record instead of inserting a new one
            repository.updateCategory(
                Category(id = id, name = name,type= type, iconName = icon, isCustom = true)
            )
        }
    }
}