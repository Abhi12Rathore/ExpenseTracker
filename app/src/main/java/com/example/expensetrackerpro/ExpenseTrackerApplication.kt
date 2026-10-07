package com.example.expensetrackerpro

import android.app.Application
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


@HiltAndroidApp
class ExpenseTrackerApplication : Application() {
    // Define a scope for background tasks like DB initialization
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val database by lazy {

        AppDatabase.getDatabase(this,applicationScope)
    }

    val repository by lazy {
        TransactionRepository(database.transactionDao())
    }
}