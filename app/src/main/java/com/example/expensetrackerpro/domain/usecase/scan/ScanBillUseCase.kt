package com.example.expensetrackerpro.domain.usecase.scan

import com.example.expensetrackerpro.domain.error.DomainError
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.models.DocumentUri
import com.example.expensetrackerpro.domain.models.TransactionDraft
import com.example.expensetrackerpro.domain.scan.BillTextReader
import com.example.expensetrackerpro.domain.time.TimeProvider
import javax.inject.Inject

/**
 * Reads the photo of a bill and works out a transaction from it. A photo with no amount on it
 * at all is not a bill that can be used, and fails like one that could not be read.
 */
class ScanBillUseCase @Inject constructor(
    private val billTextReader: BillTextReader,
    private val parseBill: ParseBillUseCase,
    private val timeProvider: TimeProvider
) {

    suspend operator fun invoke(image: DocumentUri): Outcome<TransactionDraft> =
        when (val text = billTextReader.read(image)) {
            is Outcome.Failure -> text
            is Outcome.Success -> {
                val draft = parseBill(text.value, today = timeProvider.now())

                if (draft.amount == null && draft.otherAmounts.isEmpty()) {
                    Outcome.Failure(DomainError.BillUnreadable)
                } else {
                    Outcome.Success(draft)
                }
            }
        }
}
