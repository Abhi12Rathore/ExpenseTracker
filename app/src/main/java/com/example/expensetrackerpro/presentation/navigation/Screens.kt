package com.example.expensetrackerpro.presentation.navigation


sealed class Screens(val route: String) {

    data object TransactionForm : Screens("transaction_form?transactionId={transactionId}") {
        fun getRoute(transactionId: Int? = null): String {
            return if (transactionId != null && transactionId != -1) {
                "transaction_form?transactionId=$transactionId"
            } else {
                "transaction_form"
            }
        }
    }

    data object ManageCategories : Screens("manage_categories")
}