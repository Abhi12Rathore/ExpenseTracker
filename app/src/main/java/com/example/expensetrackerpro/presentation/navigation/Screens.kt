package com.example.expensetrackerpro.presentation.navigation

import android.net.Uri
import com.example.expensetrackerpro.domain.models.DateFilter

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

    /** The History tab. Every argument is optional: without them it opens on the current month. */
    data object History : Screens(
        "transactions?start={start}&end={end}&granularity={granularity}" +
            "&categoryId={categoryId}&categoryName={categoryName}"
    ) {
        const val BASE_ROUTE = "transactions"
        const val ARG_START = "start"
        const val ARG_END = "end"
        const val ARG_GRANULARITY = "granularity"
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_CATEGORY_NAME = "categoryName"
        const val NO_VALUE = -1L

        fun getRoute(filter: HistoryFilter): String {
            val dates = when (val dateFilter = filter.dateFilter) {
                is DateFilter.Period ->
                    "$ARG_START=${dateFilter.anchorMillis}&$ARG_GRANULARITY=${dateFilter.granularity.name}"
                is DateFilter.Custom ->
                    "$ARG_START=${dateFilter.range.startMillis}&$ARG_END=${dateFilter.range.endMillis}"
            }
            val category = filter.category
                ?.let { "&$ARG_CATEGORY_ID=${it.id}&$ARG_CATEGORY_NAME=${Uri.encode(it.name)}" }
                .orEmpty()

            return "$BASE_ROUTE?$dates$category"
        }
    }
}
