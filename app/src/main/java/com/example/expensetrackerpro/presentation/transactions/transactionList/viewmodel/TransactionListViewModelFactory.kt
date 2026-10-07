package com.example.expensetrackerpro.presentation.transactions.transactionList.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository

class TransactionListViewModelFactory(
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}