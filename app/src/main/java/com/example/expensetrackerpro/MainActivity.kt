package com.example.expensetrackerpro

import AppNavHost
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val database = AppDatabase.getDatabase(this,scope)
        val repository = TransactionRepository(database.transactionDao())

        setContent {
            ExpenseTrackerProTheme {
                val app = LocalContext.current.applicationContext as ExpenseTrackerApplication
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavHost(
                            navController = navController,
                            repository = repository
                        )
                    }
                }
            }
        }
    }
}
