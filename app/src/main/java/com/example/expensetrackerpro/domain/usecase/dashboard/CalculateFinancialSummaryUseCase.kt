package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionType
import javax.inject.Inject

class CalculateFinancialSummaryUseCase @Inject constructor(
    private val calculateSavingsRate: CalculateSavingsRateUseCase
) {

    operator fun invoke(transactions: List<Transaction>): FinancialSummary {
        val income = transactions
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.amount }
        val expense = transactions
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.amount }

        return FinancialSummary(
            totalBalance = income - expense,
            totalIncome = income,
            totalExpense = expense,
            savingsRate = calculateSavingsRate(income, expense)
        )
    }
}
