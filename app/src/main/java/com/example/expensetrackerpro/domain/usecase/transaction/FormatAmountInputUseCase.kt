package com.example.expensetrackerpro.domain.usecase.transaction

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/** A stored amount as the amount field shows it: `4201` or `4201.5`, never `4201.0`. */
class FormatAmountInputUseCase @Inject constructor() {

    operator fun invoke(amount: Double): String =
        BigDecimal.valueOf(amount)
            .setScale(ValidateAmountUseCase.MAX_DECIMALS, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}
