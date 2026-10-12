package com.example.expensetrackerpro.domain.usecase.export

import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.error.map
import com.example.expensetrackerpro.domain.models.DateRange
import com.example.expensetrackerpro.domain.models.ExportDestination
import com.example.expensetrackerpro.domain.repository.FileExportRepository
import com.example.expensetrackerpro.domain.usecase.transaction.GetTransactionsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Saves the transactions of a period as a CSV file where the user chose. */
class ExportTransactionsUseCase @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val buildTransactionsCsv: BuildTransactionsCsvUseCase,
    private val fileExportRepository: FileExportRepository
) {

    /** How many transactions were written. */
    suspend operator fun invoke(range: DateRange, destination: ExportDestination): Outcome<Int> {
        val transactions = getTransactions(range).first()

        return fileExportRepository
            .writeText(destination, buildTransactionsCsv(transactions))
            .map { transactions.size }
    }
}
