package com.example.expensetrackerpro.domain.usecase.transaction

import com.example.expensetrackerpro.domain.models.Money
import java.math.BigDecimal
import javax.inject.Inject

/** A stored amount as the amount field shows it: `4201` or `4201.5`, never `4201.0`. */
class FormatAmountInputUseCase @Inject constructor() {

    operator fun invoke(amount: Money): String =
        BigDecimal.valueOf(amount.paise, ValidateAmountUseCase.MAX_DECIMALS)
            .stripTrailingZeros()
            .toPlainString()
}
