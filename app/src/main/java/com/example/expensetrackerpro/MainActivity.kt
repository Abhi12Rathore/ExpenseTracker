package com.example.expensetrackerpro

import AppNavHost
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.expensetrackerpro.cleanArchPOC.presentation.ExpenseViewModel
import com.example.expensetrackerpro.cleanArchPOC.presentation.error.ExpenseUiError
import com.example.expensetrackerpro.data.local.expenseDatabase.AppDatabase
import com.example.expensetrackerpro.data.repository.expenseRepositoryImpl.TransactionRepository
import com.example.expensetrackerpro.hiltPOC.PaymentViewModel
import com.example.expensetrackerpro.hiltPOC.PaymentViewModelWithParam
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
//    private val expenseViewModel: ExpenseViewModel by viewModels()
//    private val viewModel: PaymentViewModel by viewModels()
//    private val paymentId = "payment_123"
//
//    private val viewModel1: PaymentViewModelWithParam by viewModels(
//        extrasProducer = {
//            defaultViewModelCreationExtras.withCreationCallback<
//                    PaymentViewModelWithParam.Factory
//                    > { factory ->
//                factory.create(paymentId)
//            }
//        }
//    )

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

/*        val runner = HiltPocRunner()
        val result = runner.run()

        println(result)

        setContent {
            Text(text = result)
        }*/


        /* Uncomment for checking HILT POC Code..

        val result = viewModel1.loadPayment()
        viewModel.testMultibinding()

        setContent {
            Text(text = result)
        }

        */

       /* setContent {

            val state by expenseViewModel.uiState.collectAsState()

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                when {

                    // 1. No cached data + API still running
                    state.expenses.isEmpty() && state.isRefreshing -> {
                        Text("Loading...")
                    }

                    // 2. No cached data + refresh failed
                    state.expenses.isEmpty() && state.refreshError != null -> {
                        Text(
                            text = when (state.refreshError) {
                                ExpenseUiError.NoInternet ->
                                    "No internet connection"

                                ExpenseUiError.Unauthorized ->
                                    "Session expired"

                                ExpenseUiError.ServerError ->
                                    "Server error"

                                ExpenseUiError.Unknown ->
                                    "Something went wrong"

                                null -> ""
                            }
                        )
                    }

                    // 3. Expenses available
                    state.expenses.isNotEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            state.expenses.forEach { expense ->
                                Text(
                                    text = "${expense.category} - ₹${expense.amount}"
                                )
                            }

                            // Cached data remains visible even if refresh failed
                            state.refreshError?.let { error ->

                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )

                                Text(
                                    text = when (error) {
                                        ExpenseUiError.NoInternet ->
                                            "Couldn't refresh - No internet"

                                        ExpenseUiError.Unauthorized ->
                                            "Couldn't refresh - Session expired"

                                        ExpenseUiError.ServerError ->
                                            "Couldn't refresh - Server error"

                                        ExpenseUiError.Unknown ->
                                            "Couldn't refresh expenses"
                                    }
                                )
                            }
                        }
                    }

                    // 4. Refresh succeeded but server returned no expenses
                    else -> {
                        Text("No expenses")
                    }
                }
            }
        }*/
    }
}
