package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import com.example.expensetrackerpro.utils.DateFormatter
import javax.inject.Inject

/**
 * Turns transactions into a CSV file (RFC 4180) that Excel and Google Sheets open as they are:
 * ISO dates, amounts as plain positive decimals with the direction in the Type column.
 */
class BuildTransactionsCsvUseCase @Inject constructor() {

    /** A header line, then a line per transaction, newest first. Every line ends in CRLF. */
    operator fun invoke(transactions: List<Transaction>): String = buildString {
        appendLine(HEADER)
        transactions.sortedByDescending { it.dateMillis }.forEach { transaction ->
            appendLine(
                listOf(
                    DateFormatter.formatIsoDate(transaction.dateMillis),
                    transaction.transactionType.csvName,
                    transaction.categoryName.toTextCell(),
                    transaction.title.toTextCell(),
                    transaction.note.toTextCell(),
                    transaction.amount.toPlainDecimal()
                )
            )
        }
    }

    private fun StringBuilder.appendLine(cells: List<String>) {
        append(cells.joinToString(SEPARATOR)).append(LINE_END)
    }

    // The file's own words, the same in every language
    private val TransactionType.csvName: String
        get() = when (this) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
        }

    /** `1130.00`: no symbol, no grouping and no sign, so a spreadsheet reads it as a number. */
    private fun Money.toPlainDecimal(): String {
        val paise = abs().paise
        val decimals = (paise % Money.PAISE_PER_RUPEE).toString().padStart(2, '0')

        return "${paise / Money.PAISE_PER_RUPEE}.$decimals"
    }

    private fun String.toTextCell(): String = guardedAgainstFormulas().quotedIfNeeded()

    // A spreadsheet would run a cell that starts like a formula. The apostrophe makes it text.
    private fun String.guardedAgainstFormulas(): String =
        if (firstOrNull() in FORMULA_STARTS) "'$this" else this

    private fun String.quotedIfNeeded(): String =
        if (any { it in NEEDS_QUOTING }) "\"${replace("\"", "\"\"")}\"" else this

    private companion object {
        val HEADER = listOf("Date", "Type", "Category", "Title", "Note", "Amount")
        const val SEPARATOR = ","
        const val LINE_END = "\r\n"
        val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')
        val NEEDS_QUOTING = setOf(',', '"', '\n', '\r')
    }
}
