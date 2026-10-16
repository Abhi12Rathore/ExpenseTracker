package com.example.expensetrackerpro.presentation.navigation

import android.net.Uri
import com.example.expensetrackerpro.domain.models.DateFilter
import com.example.expensetrackerpro.presentation.transactions.addEditTransactions.uistate.AddEntryMode

sealed class Screens(val route: String) {

    /**
     * The transaction form: a stored transaction by its id, or a new one opened in a mode (the
     * bar focused, the scanner or the voice open). App shortcuts reach a new one through [DEEP_LINK].
     */
    data object TransactionForm : Screens("transaction_form?transactionId={transactionId}&mode={mode}") {
        const val ARG_TRANSACTION_ID = "transactionId"
        const val ARG_MODE = "mode"

        /** What the launcher's app shortcuts open (`res/xml-v25/shortcuts.xml`). */
        const val DEEP_LINK = "expensetracker://add?mode={mode}"

        fun getRoute(transactionId: Int? = null): String {
            return if (transactionId != null && transactionId != -1) {
                "transaction_form?transactionId=$transactionId"
            } else {
                "transaction_form"
            }
        }

        /** A new transaction, opened ready for [mode]. */
        fun newRoute(mode: AddEntryMode): String =
            if (mode == AddEntryMode.MANUAL) getRoute() else "transaction_form?$ARG_MODE=${mode.name}"
    }

    data object ManageCategories : Screens("manage_categories")

    data object ExportBackup : Screens("export_backup")

    data object Language : Screens("language")

    data object Currency : Screens("currency")

    /** The Transactions tab. Every argument is optional: without them it opens on the current month. */
    data object Transactions : Screens(
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

        fun getRoute(filter: TransactionsFilter): String {
            val dates = when (val dateFilter = filter.dateFilter) {
                is DateFilter.Period ->
                    "$ARG_START=${dateFilter.anchorMillis}&$ARG_GRANULARITY=${dateFilter.granularity.name}"
                is DateFilter.Custom ->
                    "$ARG_START=${dateFilter.range.startMillis}&$ARG_END=${dateFilter.range.endMillis}"
            }
            val category = filter.category
                ?.let { "&$ARG_CATEGORY_ID=${it.id.value}&$ARG_CATEGORY_NAME=${Uri.encode(it.name)}" }
                .orEmpty()

            return "$BASE_ROUTE?$dates$category"
        }
    }
}
