package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Transaction
import com.example.expensetrackerpro.domain.models.TransactionId
import com.example.expensetrackerpro.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    suspend operator fun invoke(id: TransactionId): Transaction? =
        transactionRepository.getTransactionById(id)
}
