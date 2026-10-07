package com.example.expensetrackerpro.presentation.transactions.addEditTransactions

import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddEditTransactionViewModelFactory(
    private val repository: TransactionRepository,
    private val transactionId: Int?
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Updated check for the new Transaction ViewModel
        if (modelClass.isAssignableFrom(AddEditTransactionViewModel::class.java)) {
            return AddEditTransactionViewModel(
                repository = repository,
                transactionId = transactionId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}