package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetFinancialSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val calculateFinancialSummary: CalculateFinancialSummaryUseCase
) {

    operator fun invoke(): Flow<FinancialSummary> =
        transactionRepository.getAllTransactions().map { calculateFinancialSummary(it) }
}
