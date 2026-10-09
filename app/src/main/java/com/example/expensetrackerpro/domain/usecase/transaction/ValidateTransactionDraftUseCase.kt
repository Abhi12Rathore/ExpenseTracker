package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.AmountValidation
import com.example.expensetrackerpro.domain.models.TransactionDraftValidation
import javax.inject.Inject

/** A transaction can be saved once it has a valid amount, a title, a category and a note that fits. */
class ValidateTransactionDraftUseCase @Inject constructor(
    private val validateAmount: ValidateAmountUseCase
) {

    operator fun invoke(
        amount: String,
        title: String,
        note: String,
        hasCategory: Boolean
    ): TransactionDraftValidation {
        val amountValidation = validateAmount(amount)

        return TransactionDraftValidation(
            amount = (amountValidation as? AmountValidation.Valid)?.amount,
            amountError = (amountValidation as? AmountValidation.Invalid)?.reason,
            isTitleBlank = title.isBlank(),
            isNoteTooLong = note.length > NOTE_MAX_LENGTH,
            isCategoryMissing = !hasCategory
        )
    }

    companion object {
        const val NOTE_MAX_LENGTH = 200
    }
}
